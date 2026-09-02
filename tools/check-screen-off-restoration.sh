#!/usr/bin/env bash
set -euo pipefail

PACKAGE="${PACKAGE:-com.steamforge.game}"
ARTIFACT_DIR="${ARTIFACT_DIR:-ci-process-recreation}"
mkdir -p "$ARTIFACT_DIR"

current_pid() {
  adb shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r' | awk '{print $1}'
}

dump_ui() {
  local label="$1"
  local attempt
  for attempt in $(seq 1 15); do
    adb shell rm -f /sdcard/window.xml >/dev/null 2>&1 || true
    rm -f /tmp/window.xml /tmp/ui-dump.log
    if adb shell uiautomator dump /sdcard/window.xml > /tmp/ui-dump.log 2>&1 \
      && adb pull /sdcard/window.xml /tmp/window.xml >/dev/null 2>&1 \
      && [[ -s /tmp/window.xml ]]; then
      cp /tmp/window.xml "$ARTIFACT_DIR/${label}.xml"
      return 0
    fi
    sleep 2
  done
  echo "UI dump failed for ${label}" >&2
  cat /tmp/ui-dump.log >&2 || true
  return 1
}

has_tile() {
  grep -Eqi 'content-desc="[^\"]+, [0-9]+"' /tmp/window.xml
}

wait_for_tile() {
  local label="$1"
  local attempt
  for attempt in $(seq 1 15); do
    if dump_ui "$label" && has_tile; then
      return 0
    fi
    sleep 2
  done
  echo 'Game tile not found after screen wake' >&2
  cat /tmp/window.xml >&2 || true
  return 1
}

board_signature() {
  local label="$1"
  local output="$2"
  dump_ui "$label"
  python3 - "$output" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

output = Path(sys.argv[1])
root = ET.parse('/tmp/window.xml').getroot()
bounds_re = re.compile(r'^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$')
tile_desc_re = re.compile(r'^.+, [0-9]+$')
score_re = re.compile(r'^СЧЁТ: ([0-9]+)$')
move_re = re.compile(r'^ХОД ([0-9]+)$')
tiles = []
scores = set()
moves = set()

for node in root.iter('node'):
    text = node.attrib.get('text') or ''
    desc = node.attrib.get('content-desc') or ''
    for value in (text, desc):
        score = score_re.fullmatch(value)
        if score:
            scores.add(int(score.group(1)))
        move = move_re.fullmatch(value)
        if move:
            moves.add(int(move.group(1)))

    if not tile_desc_re.fullmatch(desc):
        continue
    bounds = bounds_re.match(node.attrib.get('bounds') or '')
    if not bounds:
        continue
    left, top, right, bottom = map(int, bounds.groups())
    width = right - left
    height = bottom - top
    if width < 100 or height < 100 or abs(width - height) > 4:
        continue
    tiles.append((top, left, bottom, right, desc))

assert len(scores) == 1, f'unexpected score semantics: {sorted(scores)}'
assert len(moves) == 1, f'unexpected move semantics: {sorted(moves)}'
assert tiles, 'no semantic board tiles found'
assert len(tiles) <= 16, f'unexpected tile count: {len(tiles)}'
tiles.sort()
lines = [f'SCORE|{next(iter(scores))}', f'MOVE|{next(iter(moves))}']
lines.extend(f'TILE|{desc}|[{left},{top}][{right},{bottom}]' for top, left, bottom, right, desc in tiles)
output.write_text('\n'.join(lines) + '\n', encoding='utf-8')
PY
  cp "$output" "$ARTIFACT_DIR/$(basename "$output")"
}

power_dump() {
  adb shell dumpsys power | tr -d '\r'
}

is_awake() {
  power_dump | grep -Eq 'mWakefulness=Awake|Display Power: state=ON'
}

is_asleep() {
  power_dump | grep -Eq 'mWakefulness=Asleep|Display Power: state=OFF'
}

wait_for_power_state() {
  local expected="$1"
  local attempt
  for attempt in $(seq 1 20); do
    if [[ "$expected" == 'awake' ]] && is_awake; then
      return 0
    fi
    if [[ "$expected" == 'asleep' ]] && is_asleep; then
      return 0
    fi
    sleep 1
  done
  echo "Device did not become ${expected}" >&2
  power_dump >&2 || true
  return 1
}

baseline=/tmp/screen-off-before.signature.txt
after=/tmp/screen-off-after.signature.txt
board_signature '50-before-screen-off-window' "$baseline"
before_pid="$(current_pid)"
test -n "$before_pid"

if ! is_awake; then
  echo 'Expected an awake device before screen-off validation' >&2
  power_dump >&2 || true
  exit 1
fi

adb shell input keyevent KEYCODE_POWER
wait_for_power_state asleep
power_dump > "$ARTIFACT_DIR/51-screen-off-power.txt"
off_pid="$(current_pid)"
if [[ "$off_pid" != "$before_pid" ]]; then
  echo "App process changed while the screen was off: ${before_pid} -> ${off_pid}" >&2
  exit 1
fi

echo 'Screen-off state reached with the Steamforge process still alive.'

adb shell input keyevent KEYCODE_WAKEUP
wait_for_power_state awake
adb shell cmd window dismiss-keyguard
sleep 2
wait_for_tile '52-after-screen-wake-window'
after_pid="$(current_pid)"
if [[ "$after_pid" != "$before_pid" ]]; then
  echo "App process changed across screen off/wake: ${before_pid} -> ${after_pid}" >&2
  exit 1
fi

board_signature '53-after-screen-wake-state' "$after"
if ! diff -u "$baseline" "$after" > "$ARTIFACT_DIR/screen-off-wake.diff"; then
  echo 'Active game changed across screen off/wake:' >&2
  cat "$ARTIFACT_DIR/screen-off-wake.diff" >&2
  exit 1
fi
adb exec-out screencap -p > "$ARTIFACT_DIR/53-after-screen-wake.png"
test -s "$ARTIFACT_DIR/53-after-screen-wake.png"

echo 'Screen off/wake OK: process stayed alive and score, move count, tiles and bounds were unchanged.'
