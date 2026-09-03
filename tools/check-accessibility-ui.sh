#!/usr/bin/env bash
set -euo pipefail

PACKAGE="${PACKAGE:-com.steamforge.game}"
ARTIFACT_DIR="${ARTIFACT_DIR:-ci-accessibility-ui}"
FONT_SCALE="${FONT_SCALE:-1.3}"
mkdir -p "$ARTIFACT_DIR"

cleanup() {
  adb shell settings put system font_scale 1.0 >/dev/null 2>&1 || true
  adb shell wm size reset >/dev/null 2>&1 || true
  adb shell wm density reset >/dev/null 2>&1 || true
}
trap cleanup EXIT

launch_app() {
  adb shell am force-stop "$PACKAGE"
  adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null
  sleep 2
}

dump_ui() {
  local label="$1"
  local attempt
  for attempt in $(seq 1 15); do
    adb shell rm -f /sdcard/accessibility-window.xml >/dev/null 2>&1 || true
    rm -f /tmp/accessibility-window.xml /tmp/accessibility-dump.log
    if adb shell uiautomator dump /sdcard/accessibility-window.xml > /tmp/accessibility-dump.log 2>&1 \
      && adb pull /sdcard/accessibility-window.xml /tmp/accessibility-window.xml >/dev/null 2>&1 \
      && [[ -s /tmp/accessibility-window.xml ]]; then
      cp /tmp/accessibility-window.xml "$ARTIFACT_DIR/${label}.xml"
      return 0
    fi
    sleep 2
  done
  cat /tmp/accessibility-dump.log >&2 || true
  return 1
}

wait_for_text() {
  local needle="$1"
  local label="$2"
  local attempt
  for attempt in $(seq 1 15); do
    if dump_ui "$label" && grep -Fqi "$needle" /tmp/accessibility-window.xml; then
      return 0
    fi
    sleep 2
  done
  echo "Text not found: $needle" >&2
  cat /tmp/accessibility-window.xml >&2 || true
  return 1
}

capture() {
  local label="$1"
  adb exec-out screencap -p > "$ARTIFACT_DIR/${label}.png"
  test -s "$ARTIFACT_DIR/${label}.png"
}

# Resolve a semantic label to its nearest real clickable ancestor. Compose may expose
# a useful text/content-desc node with [0,0][0,0] while its clickable parent owns the
# actual touch target bounds.
control_geometry() {
  local needle="$1"
  python3 - "$needle" <<'PY'
import math
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

needle = sys.argv[1].casefold()
root = ET.parse('/tmp/accessibility-window.xml').getroot()
parents = {child: parent for parent in root.iter() for child in parent}
bounds_re = re.compile(r'^\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]$')

def node_label(node):
    return ((node.attrib.get('text') or '') + ' ' + (node.attrib.get('content-desc') or '')).casefold()

def parsed_bounds(node):
    match = bounds_re.match(node.attrib.get('bounds') or '')
    if not match:
        return None
    left, top, right, bottom = map(int, match.groups())
    if right <= left or bottom <= top:
        return None
    return left, top, right, bottom

def nearest_clickable(node):
    current = node
    for _ in range(9):
        if current is None:
            break
        bounds = parsed_bounds(current)
        if current.attrib.get('clickable') == 'true' and bounds is not None:
            return current, bounds
        current = parents.get(current)
    return None

resolved = []
seen = set()
for node in root.iter('node'):
    if needle not in node_label(node):
        continue
    hit = nearest_clickable(node)
    if hit is None:
        continue
    resolved_node, bounds = hit
    if bounds in seen:
        continue
    seen.add(bounds)
    resolved.append((resolved_node, bounds))

assert resolved, f'clickable control not found: {needle}'
# Prefer the smallest matching touch target so a broad clickable container cannot hide
# a too-small child control.
node, bounds = min(
    resolved,
    key=lambda item: (item[1][2] - item[1][0]) * (item[1][3] - item[1][1]),
)
left, top, right, bottom = bounds

size_text = subprocess.check_output(['adb', 'shell', 'wm', 'size'], text=True)
sizes = re.findall(r'(\d+)x(\d+)', size_text)
assert sizes, size_text
width, height = map(int, sizes[-1])

density_text = subprocess.check_output(['adb', 'shell', 'wm', 'density'], text=True)
densities = re.findall(r'(\d+)', density_text)
assert densities, density_text
density = int(densities[-1])
min_px = math.ceil(48.0 * density / 160.0 - 0.01)
control_width = right - left
control_height = bottom - top

assert left >= 0 and top >= 0, (needle, width, height, node.attrib)
assert right <= width and bottom <= height, (needle, width, height, node.attrib)
assert control_width >= min_px, (needle, 'width', control_width, 'min', min_px, node.attrib)
assert control_height >= min_px, (needle, 'height', control_height, 'min', min_px, node.attrib)

print(
    f'{needle}: [{left},{top}][{right},{bottom}] '
    f'= {control_width}x{control_height}px; minimum 48dp={min_px}px; '
    f'display={width}x{height}; density={density}'
)
print((left + right) // 2, (top + bottom) // 2, file=sys.stderr)
PY
}

control_center() {
  local needle="$1"
  python3 - "$needle" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

needle = sys.argv[1].casefold()
root = ET.parse('/tmp/accessibility-window.xml').getroot()
parents = {child: parent for parent in root.iter() for child in parent}
bounds_re = re.compile(r'^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$')

def node_label(node):
    return ((node.attrib.get('text') or '') + ' ' + (node.attrib.get('content-desc') or '')).casefold()

def parsed_bounds(node):
    match = bounds_re.match(node.attrib.get('bounds') or '')
    if not match:
        return None
    left, top, right, bottom = map(int, match.groups())
    if right <= left or bottom <= top:
        return None
    return left, top, right, bottom

hits = []
seen = set()
for original in root.iter('node'):
    if needle not in node_label(original):
        continue
    current = original
    for _ in range(9):
        if current is None:
            break
        bounds = parsed_bounds(current)
        if current.attrib.get('clickable') == 'true' and bounds is not None:
            if bounds not in seen:
                seen.add(bounds)
                hits.append(bounds)
            break
        current = parents.get(current)

assert hits, f'clickable control not found: {needle}'
left, top, right, bottom = min(hits, key=lambda b: (b[2] - b[0]) * (b[3] - b[1]))
print((left + right) // 2, (top + bottom) // 2)
PY
}

assert_control() {
  local needle="$1"
  local label="$2"
  dump_ui "$label"
  control_geometry "$needle" | tee "$ARTIFACT_DIR/${label}-${needle// /_}-bounds.txt"
}

tap_control() {
  local needle="$1"
  local label="$2"
  dump_ui "$label"
  local x y
  read -r x y < <(control_center "$needle")
  adb shell input tap "$x" "$y"
  sleep 2
}

scroll_until_control() {
  local needle="$1"
  local label="$2"
  local attempt
  local size_text width height x start_y end_y
  size_text="$(adb shell wm size | tail -1 | tr -d '\r')"
  read -r width height < <(printf '%s\n' "$size_text" | sed -E 's/.*: ([0-9]+)x([0-9]+)/\1 \2/')
  x=$((width / 2))
  start_y=$((height * 4 / 5))
  end_y=$((height * 2 / 5))

  for attempt in $(seq 1 8); do
    dump_ui "${label}-${attempt}"
    if control_geometry "$needle" > "$ARTIFACT_DIR/${label}-${needle// /_}-bounds.txt" 2>/dev/null; then
      cat "$ARTIFACT_DIR/${label}-${needle// /_}-bounds.txt"
      return 0
    fi
    adb shell input swipe "$x" "$start_y" "$x" "$end_y" 280
    sleep 1
  done
  echo "Reachable 48dp control not found after scrolling: $needle" >&2
  cat /tmp/accessibility-window.xml >&2 || true
  return 1
}

wait_for_game() {
  local label="$1"
  local attempt
  for attempt in $(seq 1 15); do
    if dump_ui "$label" \
      && grep -Eqi 'content-desc="[^\"]+, [0-9]+"' /tmp/accessibility-window.xml \
      && grep -Fqi 'ОТМЕНА' /tmp/accessibility-window.xml \
      && grep -Fqi 'КЛЮЧ' /tmp/accessibility-window.xml; then
      return 0
    fi
    sleep 2
  done
  echo 'Production gameplay surface not found' >&2
  cat /tmp/accessibility-window.xml >&2 || true
  return 1
}

assert_game_tile_inside_display() {
  local label="$1"
  dump_ui "$label"
  python3 <<'PY' | tee "$ARTIFACT_DIR/${label}-tile-bounds.txt"
import re
import subprocess
import xml.etree.ElementTree as ET

root = ET.parse('/tmp/accessibility-window.xml').getroot()
size_text = subprocess.check_output(['adb', 'shell', 'wm', 'size'], text=True)
sizes = re.findall(r'(\d+)x(\d+)', size_text)
assert sizes, size_text
width, height = map(int, sizes[-1])
bounds_re = re.compile(r'^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$')

def bounds(node):
    match = bounds_re.match(node.attrib.get('bounds') or '')
    if not match:
        return None
    left, top, right, bottom = map(int, match.groups())
    if right <= left or bottom <= top:
        return None
    return left, top, right, bottom

candidates = []
for node in root.iter('node'):
    if not re.fullmatch(r'.+,\s*[0-9]+', node.attrib.get('content-desc') or ''):
        continue
    parsed = bounds(node)
    if parsed is not None:
        candidates.append((node, parsed))

assert candidates, 'missing visible semantic gameplay tile'
node, (left, top, right, bottom) = max(
    candidates,
    key=lambda item: (item[1][2] - item[1][0]) * (item[1][3] - item[1][1]),
)
assert left >= 0 and top >= 0, node.attrib
assert right <= width and bottom <= height, (width, height, node.attrib)
print(f'tile: [{left},{top}][{right},{bottom}] inside {width}x{height}')
PY
}

# Fixed representative phone window; font scaling is the variable under test.
adb shell wm size 1080x2400
adb shell wm density 420
adb shell settings put system font_scale "$FONT_SCALE"
actual_scale="$(adb shell settings get system font_scale | tr -d '\r')"
printf 'Requested font scale: %s; device reports: %s\n' "$FONT_SCALE" "$actual_scale" | tee "$ARTIFACT_DIR/font-scale.txt"
python3 - "$FONT_SCALE" "$actual_scale" <<'PY'
import math
import sys
expected = float(sys.argv[1])
actual = float(sys.argv[2])
assert math.isclose(expected, actual, rel_tol=0, abs_tol=0.01), (expected, actual)
PY

launch_app
wait_for_text 'ПРИВАТНОСТЬ' '00-privacy'
if grep -Fqi 'ОТКЛЮЧИТЬ' /tmp/accessibility-window.xml; then
  assert_control 'ОТКЛЮЧИТЬ' '00-disable-target'
  tap_control 'ОТКЛЮЧИТЬ' '00-disable-analytics'
fi
wait_for_text 'MECHANICAL 2048' '01-home'

# Home: critical controls must remain reachable, unclipped and at least 48dp at 1.3x font scale.
assert_control 'Настройки' '02-home-settings'
if grep -Fqi 'ПРОДОЛЖИТЬ' /tmp/accessibility-window.xml; then
  play_label='ПРОДОЛЖИТЬ'
else
  play_label='ИГРАТЬ'
fi
assert_control "$play_label" '03-home-play'
scroll_until_control 'Мастерская' '04-home-workshop'
scroll_until_control 'Контракты' '05-home-contracts'
capture '06-home-large-font'

# Return to the top before using the primary CTA.
adb shell input swipe 540 420 540 2100 360
sleep 1
scroll_until_control "$play_label" '07-home-play-return'
tap_control "$play_label" '08-enter-game'
wait_for_game '09-game'

# Game: semantic tool buttons keep real >=48dp targets and the board remains inside the display.
assert_control 'ОТМЕНА' '10-game-undo'
assert_control 'КЛЮЧ' '11-game-wrench'
assert_game_tile_inside_display '12-game'
capture '13-game-large-font'

echo 'Accessibility UI OK: Home/Game remain reachable at font scale 1.3; critical controls are >=48dp and inside the display.'
