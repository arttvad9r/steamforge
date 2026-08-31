#!/usr/bin/env bash
set -euo pipefail

APK="${1:-app/build/outputs/apk/release/app-release.apk}"
[[ -s "$APK" ]] || { echo "ERROR: APK not found: $APK" >&2; exit 1; }

ZIPALIGN="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/build-tools/36.0.0/zipalign"
if [[ ! -x "$ZIPALIGN" ]]; then
  ZIPALIGN="$(command -v zipalign || true)"
fi
[[ -n "$ZIPALIGN" && -x "$ZIPALIGN" ]] || { echo 'ERROR: zipalign not found' >&2; exit 1; }

READELF="$(command -v readelf || true)"
if [[ -z "$READELF" && -n "${ANDROID_NDK_HOME:-}" ]]; then
  candidate="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf"
  [[ -x "$candidate" ]] && READELF="$candidate"
fi
[[ -n "$READELF" && -x "$READELF" ]] || { echo 'ERROR: readelf/llvm-readelf not found' >&2; exit 1; }

echo "Checking 16 KB ZIP alignment: $APK"
"$ZIPALIGN" -c -P 16 -v 4 "$APK" >/dev/null

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

# unzip exits non-zero when the pattern has no matches; an APK with no native code is valid.
unzip -qq "$APK" 'lib/*/*.so' -d "$tmp_dir" 2>/dev/null || true
mapfile -t libraries < <(find "$tmp_dir" -type f -name '*.so' | sort)

if [[ ${#libraries[@]} -eq 0 ]]; then
  echo 'No native shared libraries in APK; ELF page alignment check not needed.'
  exit 0
fi

echo "Checking ELF LOAD alignment for ${#libraries[@]} native libraries..."
for library in "${libraries[@]}"; do
  "$READELF" -lW "$library" | python3 -c '
import sys

path = sys.argv[1]
alignments = []
for line in sys.stdin:
    fields = line.split()
    if fields and fields[0] == "LOAD":
        try:
            alignments.append(int(fields[-1], 0))
        except ValueError:
            pass

if not alignments:
    raise SystemExit(f"ERROR: {path}: no ELF LOAD segments found")

minimum = min(alignments)
print(f"{path}: min LOAD align = 0x{minimum:x}")
if minimum < 16384:
    raise SystemExit(f"ERROR: {path}: requires at least 0x4000 (16 KB) LOAD alignment")
' "$library"
done

echo '16 KB compatibility checks passed.'
