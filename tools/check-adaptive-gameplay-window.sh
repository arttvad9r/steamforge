#!/usr/bin/env bash
set -euo pipefail

PACKAGE="${PACKAGE:-com.steamforge.game}"
ARTIFACT_DIR="${ARTIFACT_DIR:-ci-adaptive-window}"
mkdir -p "$ARTIFACT_DIR"

launch_app() {
  adb shell am force-stop "$PACKAGE"
  adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null
  sleep 2
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
  cat /tmp/ui-dump.log >&2 || true
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
  python3 - "$needle" > /tmp/tap.txt <<'PY'
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
current = original
for _ in range(3):
    current = parents.get(current)
    if current is None:
        break
    chain.append(current)
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

wait_for_game() {
  local label="$1"
  local attempt
  for attempt in $(seq 1 15); do
    if dump_ui "$label" \
      && grep -Eqi 'content-desc="[^\"]+, [0-9]+"' /tmp/window.xml \
      && grep -Fqi 'ОТМЕНА' /tmp/window.xml \
      && grep -Fqi 'КЛЮЧ' /tmp/window.xml; then
      return 0
    fi
    sleep 2
  done
  echo 'Production game surface not found' >&2
  cat /tmp/window.xml >&2 || true
  return 1
}

capture() {
  local label="$1"
  adb exec-out screencap -p > "$ARTIFACT_DIR/${label}.png"
  test -s "$ARTIFACT_DIR/${label}.png"
}

assert_game_inside_display() {
  local label="$1"
  dump_ui "$label"
  python3 <<'PY'
import re
import subprocess
import xml.etree.ElementTree as ET

root = ET.parse('/tmp/window.xml').getroot()
size_text = subprocess.check_output(['adb', 'shell', 'wm', 'size'], text=True)
sizes = re.findall(r'(\d+)x(\d+)', size_text)
assert sizes, size_text
width, height = map(int, sizes[-1])
bounds_re = re.compile(r'^\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]$')

def label(node):
    return ((node.attrib.get('text') or '') + ' ' + (node.attrib.get('content-desc') or '')).casefold()

def inside(node, name):
    match = bounds_re.match(node.attrib.get('bounds') or '')
    assert match, (name, node.attrib)
    left, top, right, bottom = map(int, match.groups())
    assert right > left and bottom > top, (name, node.attrib)
    assert left >= 0 and top >= 0, (name, width, height, node.attrib)
    assert right <= width and bottom <= height, (name, width, height, node.attrib)
    print(f'{name}: [{left},{top}][{right},{bottom}] inside {width}x{height}')

for needle in ('счёт', 'отмена', 'ключ'):
    node = next((node for node in root.iter('node') if needle in label(node)), None)
    assert node is not None, f'missing node: {needle}'
    inside(node, needle)

tile = next(
    (
        node for node in root.iter('node')
        if re.fullmatch(r'.+,\s*[0-9]+', node.attrib.get('content-desc') or '')
    ),
    None,
)
assert tile is not None, 'missing semantic gameplay tile'
inside(tile, 'tile')
PY
}

set_window() {
  local size="$1"
  local density="$2"
  adb shell wm size "$size"
  adb shell wm density "$density"
  sleep 1
}

prepare_consent() {
  set_window '1080x2400' '420'
  launch_app
  wait_for_text 'ПРИВАТНОСТЬ' '00-privacy'
  if grep -Fqi 'ОТКЛЮЧИТЬ' /tmp/window.xml; then
    tap_node 'ОТКЛЮЧИТЬ' '00-disable-analytics'
  fi
  wait_for_text 'ИГРАТЬ' '01-workshop'
}

verify_shape() {
  local size="$1"
  local density="$2"
  local label="$3"

  set_window "$size" "$density"
  launch_app
  wait_for_text 'ИГРАТЬ' "${label}-workshop"
  tap_node 'ИГРАТЬ' "${label}-play"
  wait_for_game "${label}-game"
  assert_game_inside_display "${label}-bounds" | tee "$ARTIFACT_DIR/${label}-bounds.txt"
  capture "${label}-game"
  adb shell input keyevent KEYCODE_BACK
  sleep 1
}

prepare_consent

# 360 x 640 dp, classic 16:9 portrait.
verify_shape '720x1280' '320' '10-portrait-16x9'

# 390 x 845 dp, approximately 19.5:9 portrait.
verify_shape '780x1690' '320' '20-portrait-19_5x9'

# 640 x 360 dp, classic 16:9 landscape.
verify_shape '1280x720' '320' '30-landscape-16x9'

adb shell wm size reset || true
adb shell wm density reset || true

echo 'Adaptive gameplay window OK: production score, tile, Undo and Wrench stay inside all tested window shapes.'
