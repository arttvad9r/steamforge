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

tap_control() {
  local needle="$1"
  local label="$2"
  dump_ui "$label"
  python3 - "$needle" > /tmp/tap.txt <<'PY'
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

needle = sys.argv[1].casefold()
root = ET.parse('/tmp/window.xml').getroot()
parents = {child: parent for parent in root.iter() for child in parent}
bounds_re = re.compile(r'^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$')
size_text = subprocess.check_output(['adb', 'shell', 'wm', 'size'], text=True)
sizes = re.findall(r'(\d+)x(\d+)', size_text)
assert sizes, size_text
width, height = map(int, sizes[-1])

def label(node):
    return ((node.attrib.get('text') or '') + ' ' + (node.attrib.get('content-desc') or '')).casefold()

def bounds(node):
    match = bounds_re.match(node.attrib.get('bounds') or '')
    if not match:
        return None
    left, top, right, bottom = map(int, match.groups())
    if right <= left or bottom <= top:
        return None
    if left < 0 or top < 0 or right > width or bottom > height:
        return None
    return left, top, right, bottom

for original in root.iter('node'):
    if needle not in label(original):
        continue
    chain = [original]
    current = original
    for _ in range(4):
        current = parents.get(current)
        if current is None:
            break
        chain.append(current)
    candidate = next((node for node in chain if node.attrib.get('clickable') == 'true' and bounds(node)), None)
    if candidate is None:
        candidate = next((node for node in chain if bounds(node)), None)
    if candidate is None:
        continue
    left, top, right, bottom = bounds(candidate)
    print((left + right) // 2, (top + bottom) // 2)
    raise SystemExit(0)
raise SystemExit(2)
PY
  read -r x y < /tmp/tap.txt
  adb shell input tap "$x" "$y"
  sleep 2
}

scroll_home_up() {
  local size_text width height x start_y end_y
  size_text="$(adb shell wm size | tail -1 | tr -d '\r')"
  read -r width height < <(printf '%s\n' "$size_text" | sed -E 's/.*: ([0-9]+)x([0-9]+)/\1 \2/')
  x=$((width / 2))
  start_y=$((height * 4 / 5))
  end_y=$((height * 2 / 5))
  adb shell input swipe "$x" "$start_y" "$x" "$end_y" 280
  sleep 1
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

enter_game_from_home() {
  local label="$1"
  local attempt entry
  for attempt in $(seq 1 15); do
    dump_ui "${label}-home-${attempt}"
    if grep -Fqi 'ПРОДОЛЖИТЬ' /tmp/window.xml; then
      entry='ПРОДОЛЖИТЬ'
    elif grep -Fqi 'ИГРАТЬ' /tmp/window.xml; then
      entry='ИГРАТЬ'
    else
      entry=''
    fi

    if [[ -n "$entry" ]] && tap_control "$entry" "${label}-tap-${attempt}"; then
      if wait_for_game "${label}-game"; then
        return 0
      fi
    fi
    scroll_home_up
  done
  echo 'Play/Continue entry not reachable from Home' >&2
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
parents = {child: parent for parent in root.iter() for child in parent}
size_text = subprocess.check_output(['adb', 'shell', 'wm', 'size'], text=True)
sizes = re.findall(r'(\d+)x(\d+)', size_text)
assert sizes, size_text
width, height = map(int, sizes[-1])
bounds_re = re.compile(r'^\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]$')

def parsed_bounds(node):
    match = bounds_re.match(node.attrib.get('bounds') or '')
    if not match:
        return None
    left, top, right, bottom = map(int, match.groups())
    if right <= left or bottom <= top:
        return None
    return left, top, right, bottom

def inside(node, name):
    bounds = parsed_bounds(node)
    assert bounds is not None, (name, node.attrib)
    left, top, right, bottom = bounds
    assert left >= 0 and top >= 0, (name, width, height, node.attrib)
    assert right <= width and bottom <= height, (name, width, height, node.attrib)
    print(f'{name}: [{left},{top}][{right},{bottom}] inside {width}x{height}')

def score_node():
    candidates = [
        node for node in root.iter('node')
        if (node.attrib.get('text') or '').strip().casefold() == 'счёт'
        and parsed_bounds(node) is not None
    ]
    assert candidates, 'missing visible score label'
    return candidates[0]

def semantic_control(prefix):
    matches = [
        node for node in root.iter('node')
        if (node.attrib.get('content-desc') or '').casefold().startswith(prefix + ' ')
    ]
    assert matches, f'missing semantic control: {prefix}'
    for original in matches:
        chain = [original]
        current = original
        for _ in range(4):
            current = parents.get(current)
            if current is None:
                break
            chain.append(current)
        clickable = next((node for node in chain if node.attrib.get('clickable') == 'true' and parsed_bounds(node)), None)
        if clickable is not None:
            return clickable
        visible = next((node for node in chain if parsed_bounds(node)), None)
        if visible is not None:
            return visible
    raise AssertionError(f'missing bounded control ancestor: {prefix}')

inside(score_node(), 'счёт')
inside(semantic_control('отмена'), 'отмена')
inside(semantic_control('ключ'), 'ключ')

tiles = [
    node for node in root.iter('node')
    if re.fullmatch(r'.+,\s*[0-9]+', node.attrib.get('content-desc') or '')
    and parsed_bounds(node) is not None
]
assert tiles, 'missing visible semantic gameplay tile'
inside(tiles[0], 'tile')
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
  dump_ui '00-entry'
  if grep -Fqi 'ПРИВАТНОСТЬ' /tmp/window.xml; then
    if grep -Fqi 'ОТКЛЮЧИТЬ' /tmp/window.xml; then
      tap_control 'ОТКЛЮЧИТЬ' '00-disable-analytics'
    fi
  fi
  wait_for_text 'MECHANICAL 2048' '01-home'
}

verify_shape() {
  local size="$1"
  local density="$2"
  local label="$3"

  set_window "$size" "$density"
  launch_app
  wait_for_text 'MECHANICAL 2048' "${label}-home"
  enter_game_from_home "$label"
  assert_game_inside_display "${label}-bounds" | tee "$ARTIFACT_DIR/${label}-bounds.txt"
  capture "${label}-game"
  adb shell input keyevent KEYCODE_BACK
  sleep 1
}

prepare_consent

# 360 x 640 dp, classic 16:9 portrait. Starts a production run.
verify_shape '720x1280' '320' '10-portrait-16x9'

# 390 x 845 dp, approximately 19.5:9 portrait. Resumes the same run.
verify_shape '780x1690' '320' '20-portrait-19_5x9'

# 640 x 360 dp, classic 16:9 landscape. Scrolls Home to the CTA when needed.
verify_shape '1280x720' '320' '30-landscape-16x9'

adb shell wm size reset || true
adb shell wm density reset || true

echo 'Adaptive gameplay window OK: score, tile, Undo and Wrench stay inside all tested window shapes.'
