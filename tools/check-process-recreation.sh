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
    echo "UI dump attempt ${attempt} failed for ${label}" >&2
    cat /tmp/ui-dump.log >&2 || true
    sleep 2
  done
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
  echo 'Game tile not found' >&2
  cat /tmp/window.xml >&2 || true
  return 1
}

wait_for_text() {
  local needle="$1"
  local label="$2"
  local attempt
  for attempt in $(seq 1 15); do
    if dump_ui "$label" && grep -Fqi "$needle" /tmp/window.xml; then
      return 0
    fi
    sleep 2
  done
  echo "Text not found: $needle" >&2
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
assert match, f'bounds missing: {node.attrib}'
left, top, right, bottom = map(int, match.groups())
print((left + right) // 2, (top + bottom) // 2)
PY
  read -r x y < /tmp/tap.txt
  echo "Tap '$needle' at ${x},${y}"
  adb shell input tap "$x" "$y"
  sleep 2
}

open_workshop() {
  local label="$1"
  local attempt
  for attempt in $(seq 1 12); do
    dump_ui "$label"
    if grep -Fqi 'ИГРАТЬ' /tmp/window.xml; then
      return 0
    fi
    if grep -Fqi 'content-desc="Мастерская.' /tmp/window.xml; then
      tap_node 'Мастерская.' "${label}-tap-workshop"
      if wait_for_text 'ИГРАТЬ' "${label}-workshop"; then
        return 0
      fi
    fi
    sleep 2
  done
  echo 'Workshop not reachable from app shell' >&2
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
    # Tile semantics are square, large board nodes. This excludes unrelated
    # comma+number accessibility labels if any are added elsewhere later.
    if width < 100 or height < 100 or abs(width - height) > 4:
        continue
    tiles.append((top, left, bottom, right, desc))

assert tiles, 'no semantic board tiles found'
assert len(tiles) <= 16, f'unexpected tile count: {len(tiles)}'
tiles.sort()
text = '\n'.join(f'{desc}|[{left},{top}][{right},{bottom}]' for top, left, bottom, right, desc in tiles) + '\n'
output.write_text(text, encoding='utf-8')
print(text, end='')
PY
  cp "$output" "$ARTIFACT_DIR/$(basename "$output")"
}

perform_successful_move() {
  local baseline=/tmp/board-before-move.signature.txt
  local candidate=/tmp/board-after-move.signature.txt
  board_signature '10-before-move-window' "$baseline" >/dev/null

  # The board occupies the central square on the fixed 1080x2400 / 420 dpi
  # phone surface. Try all four real swipes; at least one must change a live
  # 2048 board. Invalid moves do not spawn and therefore keep the signature.
  local gesture
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
      cp "$candidate" /tmp/expected-process-recreation.signature.txt
      cp "$candidate" "$ARTIFACT_DIR/expected-process-recreation.signature.txt"
      echo "Accepted gameplay move: ${x1},${y1} -> ${x2},${y2}"
      return 0
    fi
  done

  echo 'No swipe changed the board; cannot validate process recreation' >&2
  cat "$baseline" >&2
  return 1
}

resume_active_game() {
  local attempt
  for attempt in $(seq 1 15); do
    dump_ui '20-after-relaunch-window'
    if has_tile; then
      return 0
    fi
    if grep -Fqi 'ПРОДОЛЖИТЬ' /tmp/window.xml; then
      tap_node 'ПРОДОЛЖИТЬ' '20-tap-continue'
      wait_for_tile '21-resumed-game-window'
      return 0
    fi
    if grep -Fqi 'ИГРАТЬ' /tmp/window.xml; then
      tap_node 'ИГРАТЬ' '20-tap-play'
      wait_for_tile '21-resumed-game-window'
      return 0
    fi
    if grep -Fqi 'content-desc="Мастерская.' /tmp/window.xml; then
      tap_node 'Мастерская.' '20-tap-workshop'
      wait_for_text 'ИГРАТЬ' '20-workshop-window'
      tap_node 'ИГРАТЬ' '20-workshop-play'
      wait_for_tile '21-resumed-game-window'
      return 0
    fi
    sleep 2
  done

  echo 'Active game could not be resumed after process recreation' >&2
  cat /tmp/window.xml >&2 || true
  return 1
}

# Fresh CI install: complete the real privacy/onboarding path, then open a
# normal run. No debug/test-only app hooks are used.
wait_for_text 'ПРИВАТНОСТЬ' '00-privacy-window'
if grep -Fqi 'ОТКЛЮЧИТЬ' /tmp/window.xml; then
  tap_node 'ОТКЛЮЧИТЬ' '00-privacy-disable'
fi

for attempt in $(seq 1 15); do
  dump_ui '01-post-privacy-window'
  if grep -Fqi 'ПРОПУСТИТЬ ОБУЧЕНИЕ' /tmp/window.xml; then
    tap_node 'ПРОПУСТИТЬ ОБУЧЕНИЕ' '01-onboarding-skip'
    break
  fi
  if grep -Fqi 'ИГРАТЬ' /tmp/window.xml || grep -Fqi 'content-desc="Мастерская.' /tmp/window.xml; then
    break
  fi
  sleep 2
done

open_workshop '02-shell-window'
tap_node 'ИГРАТЬ' '03-play'
wait_for_tile '04-active-game-window'
shot '04-active-game'

perform_successful_move
# Capturing the settled accessibility tree also gives the normal autosave path
# time to finish before the OS-level force-stop.
board_signature '12-before-force-stop-window' /tmp/expected-process-recreation.signature.txt >/dev/null
shot '12-before-force-stop'

adb shell am force-stop "$PACKAGE"
adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1
resume_active_game
board_signature '22-after-process-recreation-window' /tmp/actual-process-recreation.signature.txt >/dev/null
shot '22-after-process-recreation'

if ! diff -u /tmp/expected-process-recreation.signature.txt /tmp/actual-process-recreation.signature.txt > "$ARTIFACT_DIR/process-recreation.diff"; then
  echo 'Board changed across process recreation:' >&2
  cat "$ARTIFACT_DIR/process-recreation.diff" >&2
  exit 1
fi

echo 'Process recreation OK: active board tiles and exact accessibility bounds were restored.'
