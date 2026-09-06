#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

read_prop() {
  local key="$1"
  local file value
  for file in "$ROOT_DIR/gradle.properties" "$HOME/.gradle/gradle.properties"; do
    if [[ -f "$file" ]]; then
      value="$(awk -F= -v k="$key" '$1 == k {sub(/^[^=]*=/, ""); print; exit}' "$file")"
      if [[ -n "$value" ]]; then
        printf '%s' "$value"
        return 0
      fi
    fi
  done
  return 1
}

read_declared_metadata() {
  python3 - <<'PY'
import re
from pathlib import Path

text = Path('app/build.gradle.kts').read_text(encoding='utf-8')
patterns = {
    'applicationId': r'applicationId\s*=\s*"([^"]+)"',
    'versionCode': r'versionCode\s*=\s*(\d+)',
    'versionName': r'versionName\s*=\s*"([^"]+)"',
}
values = {}
for key, pattern in patterns.items():
    match = re.search(pattern, text)
    if not match:
        raise SystemExit(f'ERROR: could not read {key} from app/build.gradle.kts')
    values[key] = match.group(1)
print('|'.join((values['applicationId'], values['versionCode'], values['versionName'])))
PY
}

printf 'Steamforge RuStore release preflight\n'
printf '%s\n' '---------------------------------'

DIRTY_STATE="$(git status --porcelain --untracked-files=normal)"
if [[ -n "$DIRTY_STATE" ]]; then
  printf '%s\n' "$DIRTY_STATE" >&2
  fail 'working tree is not clean; commit/stash/remove local source changes before a production build'
fi
SOURCE_SHA="$(git rev-parse HEAD)"

# Tracking/advertising credentials are intentionally not part of Steamforge.
for key in \
  steamforge.appmetricaApiKey \
  steamforge.privacyPolicyUrl \
  steamforge.rewardedAdUnitId \
  steamforge.interstitialAdUnitId; do
  if grep -Eq "^[[:space:]]*${key}[[:space:]]*=" "$ROOT_DIR/gradle.properties"; then
    fail "Obsolete tracking/advertising property $key must not be stored in tracked gradle.properties"
  fi
done

IFS='|' read -r APP_ID VERSION_CODE VERSION_NAME <<< "$(read_declared_metadata)"
printf 'Declared release: %s  version %s (code %s)\n' "$APP_ID" "$VERSION_NAME" "$VERSION_CODE"
printf 'Source commit: %s\n' "$SOURCE_SHA"

DIST_DIR="$ROOT_DIR/dist"
DIST_APK="$DIST_DIR/Steamforge-${VERSION_NAME}-vc${VERSION_CODE}-rustore.apk"
DIST_INVENTORY="$DIST_DIR/Steamforge-${VERSION_NAME}-vc${VERSION_CODE}-inventory.txt"
mkdir -p "$DIST_DIR"
rm -f "$DIST_APK" "$DIST_APK.sha256" "$DIST_APK.metadata.txt" "$DIST_INVENTORY"

CONFIRMED_APP_ID="$(read_prop steamforge.confirmApplicationId || true)"
[[ -n "$CONFIRMED_APP_ID" ]] || fail "steamforge.confirmApplicationId is missing. Set it to '$APP_ID' only after confirming the final package name before first publication."
[[ "$CONFIRMED_APP_ID" == "$APP_ID" ]] || fail "steamforge.confirmApplicationId='$CONFIRMED_APP_ID' does not match applicationId '$APP_ID'"

[[ -f keystore.properties ]] || fail 'keystore.properties is missing. See docs/RELEASE_SIGNING.md.'

for key in storeFile storePassword keyAlias keyPassword; do
  value="$(awk -F= -v k="$key" '$1 == k {sub(/^[^=]*=/, ""); print; exit}' keystore.properties)"
  [[ -n "$value" ]] || fail "keystore.properties: $key is missing"
done

STORE_FILE="$(awk -F= '$1 == "storeFile" {sub(/^[^=]*=/, ""); print; exit}' keystore.properties)"
if [[ "$STORE_FILE" == /* ]]; then
  KEYSTORE_PATH="$STORE_FILE"
else
  KEYSTORE_PATH="$ROOT_DIR/$STORE_FILE"
fi
[[ -f "$KEYSTORE_PATH" ]] || fail "keystore file does not exist: $STORE_FILE"

case "$KEYSTORE_PATH" in
  "$ROOT_DIR"/*)
    KEYSTORE_RELATIVE="${KEYSTORE_PATH#"$ROOT_DIR"/}"
    if git ls-files --error-unmatch -- "$KEYSTORE_RELATIVE" >/dev/null 2>&1; then
      fail "keystore must not be tracked by git: $KEYSTORE_RELATIVE"
    fi
    ;;
esac

printf 'Production inputs: OK\n'
printf 'Running tests, lint and signed release builds...\n'
./gradlew --no-daemon testDebugUnitTest lintDebug assembleRelease bundleRelease

APK="app/build/outputs/apk/release/app-release.apk"
[[ -s "$APK" ]] || fail "signed release APK not found: $APK"
AAB="app/build/outputs/bundle/release/app-release.aab"
[[ -s "$AAB" ]] || fail "signed release AAB not found: $AAB"

OUTPUT_METADATA="app/build/outputs/apk/release/output-metadata.json"
[[ -s "$OUTPUT_METADATA" ]] || fail "release metadata not found: $OUTPUT_METADATA"
IFS='|' read -r BUILT_APP_ID BUILT_VERSION_CODE BUILT_VERSION_NAME <<< "$(python3 - <<'PY'
import json
from pathlib import Path

metadata = json.loads(Path('app/build/outputs/apk/release/output-metadata.json').read_text(encoding='utf-8'))
elements = metadata.get('elements') or []
if len(elements) != 1:
    raise SystemExit(f'ERROR: expected exactly one release APK element, got {len(elements)}')
element = elements[0]
print('|'.join((
    str(metadata.get('applicationId', '')),
    str(element.get('versionCode', '')),
    str(element.get('versionName', '')),
)))
PY
)"
[[ "$BUILT_APP_ID" == "$APP_ID" ]] || fail "built applicationId '$BUILT_APP_ID' does not match declared '$APP_ID'"
[[ "$BUILT_VERSION_CODE" == "$VERSION_CODE" ]] || fail "built versionCode '$BUILT_VERSION_CODE' does not match declared '$VERSION_CODE'"
[[ "$BUILT_VERSION_NAME" == "$VERSION_NAME" ]] || fail "built versionName '$BUILT_VERSION_NAME' does not match declared '$VERSION_NAME'"

APKSIGNER="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/build-tools/36.0.0/apksigner"
if [[ ! -x "$APKSIGNER" ]]; then
  APKSIGNER="$(command -v apksigner || true)"
fi
[[ -n "$APKSIGNER" && -x "$APKSIGNER" ]] || fail 'apksigner not found; install Android Build Tools 36.0.0'

printf 'Checking APK 16 KB compatibility...\n'
bash tools/check-android-16kb.sh "$APK"

printf 'Checking final APK/AAB dependency and manifest inventory...\n'
STEAMFORGE_INVENTORY_REPORT="$DIST_INVENTORY" \
  bash tools/check-release-inventory.sh "$APK" "$AAB"

printf 'Checking APK signature...\n'
APKSIGNER_OUTPUT="$("$APKSIGNER" verify --verbose --print-certs "$APK")"
printf '%s\n' "$APKSIGNER_OUTPUT"
CERT_SHA256="$(printf '%s\n' "$APKSIGNER_OUTPUT" | awk -F': ' '/Signer #1 certificate SHA-256 digest:/ {print $2; exit}')"
[[ -n "$CERT_SHA256" ]] || fail 'could not read signer certificate SHA-256 digest from apksigner output'

cp -f "$APK" "$DIST_APK"
DIST_BASENAME="$(basename "$DIST_APK")"
(
  cd "$DIST_DIR"
  sha256sum "$DIST_BASENAME" > "$DIST_BASENAME.sha256"
)
APK_SHA256="$(awk '{print $1}' "$DIST_APK.sha256")"
cat > "$DIST_APK.metadata.txt" <<EOF
sourceCommit=$SOURCE_SHA
applicationId=$APP_ID
versionCode=$VERSION_CODE
versionName=$VERSION_NAME
apkSha256=$APK_SHA256
certificateSha256=$CERT_SHA256
inventoryReport=$(basename "$DIST_INVENTORY")
EOF

printf '\nRelease artifact:\n'
ls -lh "$DIST_APK" "$DIST_INVENTORY"
printf 'SHA-256: %s\n' "$APK_SHA256"
printf 'Certificate SHA-256: %s\n' "$CERT_SHA256"
printf 'Package: %s\nVersion: %s (%s)\n' "$APP_ID" "$VERSION_NAME" "$VERSION_CODE"
printf 'Source: %s\n' "$SOURCE_SHA"
printf 'Inventory: %s\n' "$DIST_INVENTORY"
printf '\nPreflight complete. Device-smoke and upload the exact APK file from dist/.\n'
