#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.steamforge.game"
ARTIFACT_DIR="ci-player-profile-capture"
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

open_profile() {
  local label="$1"
  wait_for_text 'MECHANICAL 2048' "${label}-home"
  if grep -Fqi 'ПРОФИЛЬ' /tmp/window.xml; then return 0; fi
  tap_node 'Коллекция' "${label}-profile-tap"
  wait_for_text 'ПРОФИЛЬ' "${label}-profile"
}

assert_visible_nodes() {
  local label="$1"; shift
  dump_ui "$label"
  python3 - "$label" "$@" <<'PY'
import re, subprocess, sys, xml.etree.ElementTree as ET
label=sys.argv[1]; needles=[x.casefold() for x in sys.argv[2:]]
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
for needle in needles:
    matches=[(n,bounds(n)) for n in root.iter('node') if needle in text(n) and bounds(n)]
    assert matches, f'{label}: missing {needle}'
    visible=False
    for node,(l,t,r,b) in matches:
        if l>=0 and t>=0 and r<=width and b<=height and r>l and b>t:
            visible=True; break
    assert visible, f'{label}: {needle} not fully inside {width}x{height}'
print(f'{label}: visible nodes OK inside {width}x{height}')
PY
}

assert_reachable_text() {
  local label="$1" needle="$2"
  for i in $(seq 0 5); do
    dump_ui "${label}-reach-${i}"
    if grep -Fqi "$needle" /tmp/window.xml; then
      if python3 - "$label" "$needle" <<'PY'
import re, subprocess, sys, xml.etree.ElementTree as ET
label,needle=sys.argv[1],sys.argv[2].casefold()
root=ET.parse('/tmp/window.xml').getroot()
size=subprocess.check_output(['adb','shell','wm','size'],text=True)
width,height=map(int,re.findall(r'(\d+)x(\d+)',size)[-1])
pattern=re.compile(r'^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$')
def text(n): return ((n.attrib.get('text') or '')+' '+(n.attrib.get('content-desc') or '')).casefold()
def bounds(n):
    m=pattern.match(n.attrib.get('bounds') or '')
    return tuple(map(int,m.groups())) if m else None
for n in root.iter('node'):
    if needle not in text(n): continue
    b=bounds(n)
    if not b: continue
    l,t,r,bottom=b
    if l>=0 and t>=0 and r<=width and bottom<=height and r>l and bottom>t:
        print(f'{label}: reachable {needle} [{l},{t}][{r},{bottom}]')
        raise SystemExit(0)
raise SystemExit(3)
PY
      then return 0; fi
    fi
    swipe_up
  done
  echo "$label: $needle not reachable" >&2
  return 1
}

assert_achievements_button() {
  local label="$1"
  dump_ui "${label}-achievements"
  python3 - "$label" <<'PY'
import re, subprocess, sys, xml.etree.ElementTree as ET
label=sys.argv[1]
root=ET.parse('/tmp/window.xml').getroot(); parents={c:p for p in root.iter() for c in p}
pattern=re.compile(r'^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$')
def text(n): return ((n.attrib.get('text') or '')+' '+(n.attrib.get('content-desc') or '')).casefold()
def bounds(n):
    m=pattern.match(n.attrib.get('bounds') or '')
    return tuple(map(int,m.groups())) if m else None
candidate=None
for original in root.iter('node'):
    if 'достижения' not in text(original): continue
    current=original
    for _ in range(8):
        if current is None: break
        b=bounds(current)
        if current.attrib.get('clickable')=='true' and b:
            candidate=(current,b); break
        current=parents.get(current)
    if candidate: break
assert candidate, f'{label}: Achievements clickable ancestor missing'
node,(l,t,r,b)=candidate
density=int(re.findall(r'\d+',subprocess.check_output(['adb','shell','wm','density'],text=True))[-1])
min_px=48*density/160
assert r-l>=min_px-1 and b-t>=min_px-1, (label,node.attrib,min_px)
print(f'{label}: Achievements button [{l},{t}][{r},{b}]')
PY
}

capture_shape() {
  local size="$1" density="$2" scale="$3" label="$4"
  adb shell wm size "$size"
  adb shell wm density "$density"
  adb shell settings put system font_scale "$scale"
  launch_app
  open_profile "$label"
  adb exec-out screencap -p > "$ARTIFACT_DIR/${label}-top.png"
  test -s "$ARTIFACT_DIR/${label}-top.png"
  assert_visible_nodes "${label}-top-assert" 'ПРОФИЛЬ' '12 345' '987 654 321' '87 654 321'
  assert_reachable_text "$label" 'ЛУЧШАЯ ЕЖЕДНЕВНАЯ СЕРИЯ'
  assert_reachable_text "$label" '123 дн.'
  assert_reachable_text "$label" 'ПОСТОЯННЫЙ ПРОГРЕСС'
  assert_reachable_text "$label" '9/12 этапов'
  assert_reachable_text "$label" 'ДОСТИЖЕНИЯ'
  assert_achievements_button "$label"
  adb exec-out screencap -p > "$ARTIFACT_DIR/${label}-bottom.png"
  test -s "$ARTIFACT_DIR/${label}-bottom.png"
}

adb shell pm clear "$PACKAGE" >/dev/null
adb shell am instrument -w \
  -e class com.steamforge.game.PlayerProfileStateTest \
  com.steamforge.game.test/androidx.test.runner.AndroidJUnitRunner \
  | tee "$ARTIFACT_DIR/state-injector.txt"
grep -Fq 'OK (1 test)' "$ARTIFACT_DIR/state-injector.txt"

capture_shape '1080x2400' '420' '1.0' '10-standard'
capture_shape '720x1600' '320' '1.0' '20-compact'
capture_shape '1080x2400' '420' '1.3' '30-large-font'

echo 'Player profile targeted capture OK.'
