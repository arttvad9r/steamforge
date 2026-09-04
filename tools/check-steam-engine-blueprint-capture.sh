#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.steamforge.game"
ARTIFACT_DIR="ci-steam-engine-capture"
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
  for i in $(seq 1 15); do
    adb shell rm -f /sdcard/window.xml >/dev/null 2>&1 || true
    rm -f /tmp/window.xml
    if adb shell uiautomator dump /sdcard/window.xml >/dev/null 2>&1 \
      && adb pull /sdcard/window.xml /tmp/window.xml >/dev/null 2>&1 \
      && [ -s /tmp/window.xml ]; then
      cp /tmp/window.xml "$ARTIFACT_DIR/${label}.xml"
      return 0
    fi
    sleep 2
  done
  return 1
}

wait_for_text() {
  local needle="$1" label="$2"
  for i in $(seq 1 15); do
    if dump_ui "$label" && grep -Fqi "$needle" /tmp/window.xml; then return 0; fi
    sleep 2
  done
  echo "Text not found: $needle" >&2
  cat /tmp/window.xml >&2 || true
  return 1
}

tap_node() {
  local needle="$1" label="$2"
  dump_ui "$label"
  python3 - "$needle" > /tmp/tap.txt <<'PY'
import re, sys, xml.etree.ElementTree as ET
needle=sys.argv[1].casefold()
root=ET.parse('/tmp/window.xml').getroot()
parents={c:p for p in root.iter() for c in p}
pattern=re.compile(r'^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$')
def text(n): return ((n.attrib.get('text') or '')+' '+(n.attrib.get('content-desc') or '')).casefold()
def bounds(n):
    m=pattern.match(n.attrib.get('bounds') or '')
    if not m: return None
    a=tuple(map(int,m.groups()))
    return a if a[2]>a[0] and a[3]>a[1] else None
for original in root.iter('node'):
    if needle not in text(original): continue
    current=original
    for _ in range(8):
        if current is None: break
        b=bounds(current)
        if current.attrib.get('clickable')=='true' and b:
            l,t,r,bottom=b
            print((l+r)//2,(t+bottom)//2)
            raise SystemExit(0)
        current=parents.get(current)
raise SystemExit(2)
PY
  read -r x y < /tmp/tap.txt
  adb shell input tap "$x" "$y"
  sleep 2
}

swipe_up() {
  local width height
  read -r width height < <(adb shell wm size | tail -1 | sed -E 's/.*: ([0-9]+)x([0-9]+)/\1 \2/' | tr -d '\r')
  adb shell input swipe $((width/2)) $((height*4/5)) $((width/2)) $((height*2/5)) 280
  sleep 1
}

open_workshop() {
  local label="$1"
  wait_for_text 'MECHANICAL 2048' "${label}-home"
  for i in $(seq 1 8); do
    dump_ui "${label}-home-${i}"
    if grep -Fqi 'Мастерская' /tmp/window.xml; then
      if tap_node 'Мастерская' "${label}-tap-${i}" && wait_for_text 'УРОВЕНЬ МАСТЕРСКОЙ' "${label}-workshop"; then
        return 0
      fi
    fi
    swipe_up
  done
  echo 'Workshop entry not reachable' >&2
  cat /tmp/window.xml >&2 || true
  return 1
}

assert_blueprint_state() {
  local label="$1" expected="$2"
  dump_ui "$label"
  python3 - "$label" "$expected" <<'PY'
import re, subprocess, sys, xml.etree.ElementTree as ET
label,expected=sys.argv[1:3]
root=ET.parse('/tmp/window.xml').getroot()
size=subprocess.check_output(['adb','shell','wm','size'],text=True)
width,height=map(int,re.findall(r'(\d+)x(\d+)',size)[-1])
pattern=re.compile(r'^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$')
def text(n): return ((n.attrib.get('text') or '')+' '+(n.attrib.get('content-desc') or '')).casefold()
def bounds(n):
    m=pattern.match(n.attrib.get('bounds') or '')
    if not m: return None
    a=tuple(map(int,m.groups()))
    return a if a[2]>a[0] and a[3]>a[1] else None
for needle in ['уровень мастерской','steam engine']:
    matches=[n for n in root.iter('node') if needle in text(n) and bounds(n)]
    assert matches, f'{label}: missing {needle}'
state_needle = 'собрано 0 из 6 частей' if expected == 'incomplete' else 'собран и установлен в мастерской'
matches=[n for n in root.iter('node') if state_needle in text(n) and bounds(n)]
assert matches, f'{label}: missing state {state_needle}'
for node in matches:
    l,t,r,b=bounds(node)
    assert l>=0 and t>=0 and r<=width and b<=height, (label,node.attrib,width,height)
print(f'{label}: {expected} Steam Engine state visible inside {width}x{height}')
PY
}

assert_play_reachable() {
  local label="$1"
  for i in $(seq 0 3); do
    dump_ui "${label}-play-${i}"
    if grep -Fqi 'ИГРАТЬ' /tmp/window.xml; then
      python3 - "$label" <<'PY'
import re, subprocess, sys, xml.etree.ElementTree as ET
label=sys.argv[1]
root=ET.parse('/tmp/window.xml').getroot()
parents={c:p for p in root.iter() for c in p}
pattern=re.compile(r'^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$')
def text(n): return ((n.attrib.get('text') or '')+' '+(n.attrib.get('content-desc') or '')).casefold()
def bounds(n):
    m=pattern.match(n.attrib.get('bounds') or '')
    return tuple(map(int,m.groups())) if m else None
candidates=[]
for original in root.iter('node'):
    if 'играть' not in text(original): continue
    current=original
    for _ in range(8):
        if current is None: break
        b=bounds(current)
        if current.attrib.get('clickable')=='true' and b:
            candidates.append((current,b)); break
        current=parents.get(current)
assert candidates, f'{label}: Play text exists but clickable ancestor missing'
node,(l,t,r,b)=min(candidates,key=lambda x:(x[1][2]-x[1][0])*(x[1][3]-x[1][1]))
density=int(re.findall(r'\d+',subprocess.check_output(['adb','shell','wm','density'],text=True))[-1])
min_px=48*density/160
assert r-l>=min_px-1 and b-t>=min_px-1, (label,node.attrib,min_px)
print(f'{label}: Play reachable [{l},{t}][{r},{b}]')
PY
      return 0
    fi
    swipe_up
  done
  echo "$label: Play CTA not reachable" >&2
  return 1
}

capture_shape() {
  local size="$1" density="$2" scale="$3" label="$4" expected="$5"
  adb shell wm size "$size"
  adb shell wm density "$density"
  adb shell settings put system font_scale "$scale"
  launch_app
  dump_ui "${label}-entry"
  if grep -Fqi 'ПРИВАТНОСТЬ' /tmp/window.xml && grep -Fqi 'ОТКЛЮЧИТЬ' /tmp/window.xml; then
    tap_node 'ОТКЛЮЧИТЬ' "${label}-privacy"
  fi
  open_workshop "$label"
  adb exec-out screencap -p > "$ARTIFACT_DIR/${label}-top.png"
  test -s "$ARTIFACT_DIR/${label}-top.png"
  assert_blueprint_state "${label}-assert" "$expected" | tee "$ARTIFACT_DIR/${label}-state.txt"
  assert_play_reachable "$label" | tee "$ARTIFACT_DIR/${label}-play.txt"
  adb exec-out screencap -p > "$ARTIFACT_DIR/${label}-play.png"
  test -s "$ARTIFACT_DIR/${label}-play.png"
}

adb shell pm clear "$PACKAGE" >/dev/null
capture_shape '1080x2400' '420' '1.0' '10-incomplete-standard' 'incomplete'
capture_shape '720x1600' '320' '1.0' '20-incomplete-compact' 'incomplete'
capture_shape '1080x2400' '420' '1.3' '30-incomplete-large-font' 'incomplete'

adb shell am instrument -w \
  -e class com.steamforge.game.SteamEngineBlueprintStateTest \
  com.steamforge.game.test/androidx.test.runner.AndroidJUnitRunner \
  | tee "$ARTIFACT_DIR/state-injector.txt"
grep -Fq 'OK (1 test)' "$ARTIFACT_DIR/state-injector.txt"

capture_shape '1080x2400' '420' '1.0' '40-complete-standard' 'complete'
capture_shape '720x1600' '320' '1.0' '50-complete-compact' 'complete'
capture_shape '1080x2400' '420' '1.3' '60-complete-large-font' 'complete'

echo 'Steam Engine blueprint targeted capture OK.'
