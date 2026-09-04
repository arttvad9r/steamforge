#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.steamforge.game"
ARTIFACT_DIR="ci-contracts-capture"
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
  local needle="$1"
  local label="$2"
  for i in $(seq 1 15); do
    if dump_ui "$label" && grep -Fqi "$needle" /tmp/window.xml; then return 0; fi
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

open_contracts() {
  local label="$1"
  wait_for_text 'MECHANICAL 2048' "${label}-home"
  for i in $(seq 1 8); do
    dump_ui "${label}-home-${i}"
    if grep -Fqi 'Контракты' /tmp/window.xml; then
      if tap_node 'Контракты' "${label}-tap-${i}" && wait_for_text 'Ежедневные задачи мастерской' "${label}-contracts"; then
        return 0
      fi
    fi
    scroll_home
  done
  echo 'Contracts entry not reachable' >&2
  cat /tmp/window.xml >&2 || true
  return 1
}

assert_contracts_top() {
  local label="$1"
  dump_ui "$label"
  python3 - "$label" <<'PY'
import re, subprocess, sys, xml.etree.ElementTree as ET
label=sys.argv[1]
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
for needle in ['контракты','ежедневные задачи мастерской','сегодня']:
    matches=[n for n in root.iter('node') if needle in text(n) and bounds(n)]
    assert matches, f'{label}: missing visible {needle}'
    l,t,r,b=bounds(matches[0]); assert l>=0 and t>=0 and r<=width and b<=height, (label,needle,matches[0].attrib)
rows=[]
for n in root.iter('node'):
    desc=(n.attrib.get('content-desc') or '').casefold()
    b=bounds(n)
    if b and 'награда ' in desc and ' из ' in desc:
        rows.append((n,b,desc))
assert len(rows) >= 2, f'{label}: expected at least two visible contract cards, got {len(rows)}'
for node,(l,t,r,b),desc in rows:
    assert l>=0 and t>=0 and r<=width and b<=height, (label,node.attrib,width,height)
print(f'{label}: visible contract cards={len(rows)}, display={width}x{height}')
PY
}

assert_contracts_bottom() {
  local label="$1"
  dump_ui "$label"
  python3 - "$label" <<'PY'
import re, subprocess, sys, xml.etree.ElementTree as ET
label=sys.argv[1]
root=ET.parse('/tmp/window.xml').getroot()
size=subprocess.check_output(['adb','shell','wm','size'],text=True)
width,height=map(int,re.findall(r'(\d+)x(\d+)',size)[-1])
pattern=re.compile(r'^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$')
def bounds(n):
    m=pattern.match(n.attrib.get('bounds') or '')
    if not m: return None
    a=tuple(map(int,m.groups()))
    return a if a[2]>a[0] and a[3]>a[1] else None
rows=[]
for n in root.iter('node'):
    desc=(n.attrib.get('content-desc') or '').casefold()
    b=bounds(n)
    if b and 'награда ' in desc and ' из ' in desc:
        rows.append((n,b,desc))
assert rows, f'{label}: no visible contract cards after scroll'
for node,(l,t,r,b),desc in rows:
    assert l>=0 and t>=0 and r<=width and b<=height, (label,node.attrib,width,height)
footer=[n for n in root.iter('node') if 'прогресс контрактов сохраняется автоматически' in ((n.attrib.get('text') or '')+' '+(n.attrib.get('content-desc') or '')).casefold() and bounds(n)]
assert footer, f'{label}: footer not reachable after scroll'
l,t,r,b=bounds(footer[0]); assert l>=0 and t>=0 and r<=width and b<=height, (label,footer[0].attrib,width,height)
print(f'{label}: bottom cards={len(rows)}, footer=[{l},{t}][{r},{b}]')
PY
}

scroll_contracts_to_bottom() {
  local width height
  read -r width height < <(adb shell wm size | tail -1 | sed -E 's/.*: ([0-9]+)x([0-9]+)/\1 \2/' | tr -d '\r')
  for _ in 1 2 3; do
    adb shell input swipe $((width/2)) $((height*4/5)) $((width/2)) $((height/3)) 320
    sleep 1
  done
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
  open_contracts "$label"
  adb exec-out screencap -p > "$ARTIFACT_DIR/${label}-top.png"
  test -s "$ARTIFACT_DIR/${label}-top.png"
  assert_contracts_top "${label}-top-assert" | tee "$ARTIFACT_DIR/${label}-top-bounds.txt"
  scroll_contracts_to_bottom
  adb exec-out screencap -p > "$ARTIFACT_DIR/${label}-bottom.png"
  test -s "$ARTIFACT_DIR/${label}-bottom.png"
  assert_contracts_bottom "${label}-bottom-assert" | tee "$ARTIFACT_DIR/${label}-bottom-bounds.txt"
}

capture_shape '1080x2400' '420' '1.0' '10-standard'
capture_shape '720x1600' '320' '1.0' '20-compact'
capture_shape '1080x2400' '420' '1.3' '30-large-font'

echo 'Contracts targeted capture OK.'
