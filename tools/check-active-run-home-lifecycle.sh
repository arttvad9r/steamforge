#!/usr/bin/env bash
set -euo pipefail

PACKAGE="${PACKAGE:-com.steamforge.game}"
ARTIFACT_DIR="${ARTIFACT_DIR:-ci-process-recreation}"
mkdir -p "$ARTIFACT_DIR"

shot() {
  local label="$1"
  adb exec-out screencap -p > "$ARTIFACT_DIR/${label}.png"
  test -s "$ARTIFACT_DIR/${label}.png"
}

dump_ui() {
  local label="$1"
  for attempt in $(seq 1 15); do
    adb shell rm -f /sdcard/window.xml >/dev/null 2>&1 || true
    rm -f /tmp/window.xml
    if adb shell uiautomator dump /sdcard/window.xml >/dev/null 2>&1 \
      && adb pull /sdcard/window.xml /tmp/window.xml >/dev/null 2>&1 \
      && [[ -s /tmp/window.xml ]]; then
      cp /tmp/window.xml "$ARTIFACT_DIR/${label}.xml"
      return 0
    fi
    sleep 2
  done
  return 1
}

has_tile() {
  grep -Eqi 'content-desc="[^\"]+, [0-9]+"' /tmp/window.xml
}

wait_for_tile() {
  local label="$1"
  for attempt in $(seq 1 15); do
    if dump_ui "$label" && has_tile; then return 0; fi
    sleep 2
  done
  echo 'Game tile not found' >&2
  cat /tmp/window.xml >&2 || true
  return 1
}

wait_for_home() {
  local label="$1"
  for attempt in $(seq 1 15); do
    if dump_ui "$label" \
      && grep -Fqi 'MECHANICAL 2048' /tmp/window.xml \
      && { grep -Fqi 'ПРОДОЛЖИТЬ' /tmp/window.xml || grep -Fqi 'ИГРАТЬ' /tmp/window.xml; }; then
      return 0
    fi
    sleep 2
  done
  echo 'Home shell not found' >&2
  cat /tmp/window.xml >&2 || true
  return 1
}

tap_node() {
  local needle="$1"
  local label="$2"
  dump_ui "$label"
  python3 - "$needle" <<'PY' > /tmp/tap.txt
import re
import sys
import xml.etree.ElementTree as ET

needle = sys.argv[1].casefold()
root = ET.parse('/tmp/window.xml').getroot()
parents = {child: parent for parent in root.iter() for child in parent}

def label(node):
    return ((node.attrib.get('text') or '') + ' ' + (node.attrib.get('content-desc') or '')).casefold()

matches = [node for node in root.iter('node') if needle in label(node)]
assert matches, f'node not found: {needle}'
original = matches[0]
chain = [original]
parent = parents.get(original)
if parent is not None:
    chain.append(parent)
    grandparent = parents.get(parent)
    if grandparent is not None:
        chain.append(grandparent)
node = next((candidate for candidate in chain if candidate.attrib.get('clickable') == 'true'), original)
match = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', node.attrib.get('bounds', ''))
assert match, node.attrib
left, top, right, bottom = map(int, match.groups())
print((left + right) // 2, (top + bottom) // 2)
PY
  read -r x y < /tmp/tap.txt
  adb shell input tap "$x" "$y"
  sleep 2
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
tiles = []
for node in root.iter('node'):
    desc = node.attrib.get('content-desc') or ''
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

assert tiles, 'no semantic board tiles found'
assert len(tiles) <= 16, f'unexpected tile count: {len(tiles)}'
tiles.sort()
text = '\n'.join(f'TILE|{desc}|[{left},{top}][{right},{bottom}]' for top, left, bottom, right, desc in tiles) + '\n'
output.write_text(text, encoding='utf-8')
print(text, end='')
PY
  cp "$output" "$ARTIFACT_DIR/$(basename "$output")"
}

assert_signature_matches() {
  local expected="$1"
  local actual="$2"
  local label="$3"
  if ! diff -u "$expected" "$actual" > "$ARTIFACT_DIR/${label}.diff"; then
    cat "$ARTIFACT_DIR/${label}.diff" >&2
    return 1
  fi
}

current_pid() {
  adb shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r' | awk '{print $1}'
}

perform_successful_move() {
  local baseline=/tmp/game-before-move.signature.txt
  local candidate=/tmp/game-after-move.signature.txt
  board_signature '10-before-move-window' "$baseline" >/dev/null
  for gesture in \
    '800 1050 280 1050' \
    '280 1050 800 1050' \
    '540 1250 540 720' \
    '540 720 540 1250'; do
    read -r x1 y1 x2 y2 <<< "$gesture"
    adb shell input swipe "$x1" "$y1" "$x2" "$y2" 260
    sleep 1
    board_signature '11-after-move-attempt-window' "$candidate" >/dev/null
    if ! cmp -s "$baseline" "$candidate"; then
      cp "$candidate" /tmp/expected-active-run.signature.txt
      cp "$candidate" "$ARTIFACT_DIR/expected-active-run.signature.txt"
      return 0
    fi
  done
  echo 'No swipe changed the board' >&2
  return 1
}

resume_from_home() {
  wait_for_home '40-after-relaunch-home'
  if grep -Fqi 'ПРОДОЛЖИТЬ' /tmp/window.xml; then
    tap_node 'ПРОДОЛЖИТЬ' '40-tap-continue'
  else
    echo 'Saved run did not expose Continue after process recreation' >&2
    return 1
  fi
  wait_for_tile '41-resumed-game-window'
}

# Fresh production route: privacy -> Home -> Play.
for attempt in $(seq 1 15); do
  dump_ui '00-launch-window'
  if grep -Fqi 'ПРИВАТНОСТЬ' /tmp/window.xml; then
    if grep -Fqi 'ОТКЛЮЧИТЬ' /tmp/window.xml; then tap_node 'ОТКЛЮЧИТЬ' '00-privacy-disable'; fi
    break
  fi
  if grep -Fqi 'MECHANICAL 2048' /tmp/window.xml; then break; fi
  sleep 2
done
wait_for_home '01-home-window'
if grep -Fqi 'ПРОДОЛЖИТЬ' /tmp/window.xml; then
  tap_node 'ПРОДОЛЖИТЬ' '02-continue-existing'
else
  tap_node 'ИГРАТЬ' '02-play'
fi
wait_for_tile '03-active-game-window'
shot '03-active-game'

perform_successful_move
board_signature '12-stable-active-run-window' /tmp/expected-active-run.signature.txt >/dev/null
shot '12-stable-active-run'

# Background/resume must keep the same Linux process and exact board geometry.
before_pid="$(current_pid)"
test -n "$before_pid"
adb shell input keyevent KEYCODE_HOME
sleep 2
after_home_pid="$(current_pid)"
test "$after_home_pid" = "$before_pid"
adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null
wait_for_tile '20-after-background-resume-window'
test "$(current_pid)" = "$before_pid"
board_signature '21-after-background-resume-state' /tmp/actual-background-resume.signature.txt >/dev/null
assert_signature_matches /tmp/expected-active-run.signature.txt /tmp/actual-background-resume.signature.txt 'background-resume'
shot '21-after-background-resume'

# Full process recreation must return through Home Continue and restore the exact board.
adb shell am force-stop "$PACKAGE"
sleep 1
test -z "$(current_pid)"
adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null
resume_from_home
board_signature '42-after-process-recreation-state' /tmp/actual-process-recreation.signature.txt >/dev/null
assert_signature_matches /tmp/expected-active-run.signature.txt /tmp/actual-process-recreation.signature.txt 'process-recreation'
shot '42-after-process-recreation'

echo 'Active-run lifecycle OK: background/resume and process recreation preserved exact tile semantics and bounds.'
