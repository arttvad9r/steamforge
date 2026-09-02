#!/usr/bin/env bash
set -euo pipefail

if [[ $# -gt 0 ]]; then
  AAB="$1"
else
  mapfile -t release_aabs < <(find app/build/outputs/bundle/release -maxdepth 1 -type f -name '*.aab' | sort)
  if [[ ${#release_aabs[@]} -ne 1 ]]; then
    echo "ERROR: expected exactly one release AAB, found ${#release_aabs[@]}" >&2
    printf '  %s\n' "${release_aabs[@]:-}" >&2
    exit 1
  fi
  AAB="${release_aabs[0]}"
fi

[[ -s "$AAB" ]] || { echo "ERROR: AAB not found or empty: $AAB" >&2; exit 1; }

unzip -tqq "$AAB"
entries_text="$(unzip -Z1 "$AAB")"

require_entry() {
  local expected="$1"
  if ! grep -Fxq "$expected" <<<"$entries_text"; then
    echo "ERROR: release AAB missing required entry: $expected" >&2
    exit 1
  fi
}

require_entry 'base/manifest/AndroidManifest.xml'
require_entry 'base/resources.pb'

if ! grep -Eq '^base/dex/classes([0-9]+)?\.dex$' <<<"$entries_text"; then
  echo 'ERROR: release AAB contains no base DEX payload' >&2
  exit 1
fi

size_bytes="$(stat -c '%s' "$AAB")"
echo "Release AAB OK: $AAB (${size_bytes} bytes)"
