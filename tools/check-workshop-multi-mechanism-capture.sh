#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.steamforge.game"
ARTIFACT_DIR="ci-workshop-multi-capture"
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

scroll_home() {
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
      if tap_node 'Мастерская' "${label}-tap-${i}" && wait_for_text 'ВОССТАНОВЛЕНИЕ ЦЕХА' "${label}-workshop"; then
        return 0
      fi
    fi
    scroll_home
  done
  echo 'Workshop entry not reachable' >&2
  cat /tmp/window.xml >&2 || true
  return 1
}

assert_workshop() {
  local label="$1"
  dump_ui "$label"
  python3 - "$label" <<'PY'
import re, subprocess, sys, xml.etree.ElementTree as ET
label=sys.argv[1]
root=ET.parse('/tmp/window.xml').getroot()
parents={c:p for p in root.iter() for c in p}
size=subprocess.check_output(['adb','shell','wm','size'],text=True)
width,height=map(int,re.findall(r'(\d+)x(\d+)',size)[-1])
density=int(re.findall(r'\d+',subprocess.check_output(['adb','shell','wm','density'],text=True))[-1])
pattern=re.compile(r'^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$')
def text(n): return ((n.attrib.get('text') or '')+' '+(n.attrib.get('content-desc') or '')).casefold()
def bounds(n):
    m=pattern.match(n.attrib.get('bounds') or '')
    if not m: return None
    a=tuple(map(int,m.groups()))
    return a if a[2]>a[0] and a[3]>a[1] else None
def assert_visible(needle):
    matches=[n for n in root.iter('node') if needle.casefold() in text(n) and bounds(n)]
    assert matches, f'{label}: missing visible {needle}'
    l,t,r,b=bounds(matches[0])
    assert l>=0 and t>=0 and r<=width and b<=height, (label,needle,matches[0].attrib,width,height)
    return matches[0],(l,t,r,b)
for needle in ['уровень мастерской','восстановление цеха','ядро','генератор','пресс','партий','рекорд','xp']:
    assert_visible(needle)

def clickable_for(needle):
    matches=[]
    for original in root.iter('node'):
        if needle.casefold() not in text(original): continue
        current=original
        for _ in range(8):
            if current is None: break
            b=bounds(current)
            if current.attrib.get('clickable')=='true' and b:
                matches.append((current,b)); break
            current=parents.get(current)
    return matches
play=clickable_for('играть')
assert play, f'{label}: missing clickable Play'
node,(l,t,r,b)=min(play,key=lambda x:(x[1][2]-x[1][0])*(x[1][3]-x[1][1]))
assert l>=0 and t>=0 and r<=width and b<=height, (label,node.attrib,width,height)
min_px=48*density/160
assert r-l>=min_px-1 and b-t>=min_px-1, (label,'play',node.attrib,min_px)
print(f'{label}: Play [{l},{t}][{r},{b}], display={width}x{height}, density={density}')

# New profiles start with zero WorkshopParts, so upgrade rows are intentionally disabled.
# Verify their semantic row bounds and 48dp height even when not clickable.
for needle in ['механическое ядро:', 'генератор давления:', 'шестерёночный пресс:']:
    matches=[n for n in root.iter('node') if needle in (n.attrib.get('content-desc') or '').casefold() and bounds(n)]
    assert matches, f'{label}: missing mechanism semantics {needle}'
    n=matches[0]; l,t,r,b=bounds(n)
    assert l>=0 and t>=0 and r<=width and b<=height, (label,needle,n.attrib,width,height)
    assert b-t>=min_px-1, (label,needle,n.attrib,min_px)
    print(f'{label}: {needle} [{l},{t}][{r},{b}]')
PY
}

capture_shape() {
  local size="$1" density="$2" scale="$3" label="$4"
  adb shell wm size "$size"
  adb shell wm density "$density"
  adb shell settings put system font_scale "$scale"
  launch_app
  dump_ui "${label}-entry"
  if grep -Fqi 'ПРИВАТНОСТЬ' /tmp/window.xml && grep -Fqi 'ОТКЛЮЧИТЬ' /tmp/window.xml; then
    tap_node 'ОТКЛЮЧИТЬ' "${label}-privacy"
  fi
  open_workshop "$label"
  adb exec-out screencap -p > "$ARTIFACT_DIR/${label}.png"
  test -s "$ARTIFACT_DIR/${label}.png"
  assert_workshop "${label}-assert" | tee "$ARTIFACT_DIR/${label}-bounds.txt"
}

capture_shape '1080x2400' '420' '1.0' '10-standard'
capture_shape '720x1600' '320' '1.0' '20-compact'
capture_shape '1080x2400' '420' '1.3' '30-large-font'

echo 'Workshop multi-mechanism targeted capture OK.'
