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

printf 'Steamforge RuStore release preflight\n'
printf '%s\n' '---------------------------------'

[[ -f keystore.properties ]] || fail 'keystore.properties is missing. See docs/RELEASE_SIGNING.md.'

for key in storeFile storePassword keyAlias keyPassword; do
  value="$(awk -F= -v k="$key" '$1 == k {sub(/^[^=]*=/, ""); print; exit}' keystore.properties)"
  [[ -n "$value" ]] || fail "keystore.properties: $key is missing"
done

STORE_FILE="$(awk -F= '$1 == "storeFile" {sub(/^[^=]*=/, ""); print; exit}' keystore.properties)"
[[ -f "$STORE_FILE" ]] || [[ -f "$ROOT_DIR/$STORE_FILE" ]] || fail "keystore file does not exist: $STORE_FILE"

required_gradle_props=(
  steamforge.appmetricaApiKey
  steamforge.privacyPolicyUrl
  steamforge.rewardedAdUnitId
  steamforge.interstitialAdUnitId
)

for key in "${required_gradle_props[@]}"; do
  value="$(read_prop "$key" || true)"
  [[ -n "$value" ]] || fail "Gradle property $key is missing (recommended location: ~/.gradle/gradle.properties)"
done

PRIVACY_URL="$(read_prop steamforge.privacyPolicyUrl)"
[[ "$PRIVACY_URL" == https://* ]] || fail 'steamforge.privacyPolicyUrl must be an HTTPS URL'

printf 'Production inputs: OK\n'
printf 'Running tests, lint and signed release build...\n'
./gradlew --no-daemon testDebugUnitTest lintDebug assembleRelease

APK="app/build/outputs/apk/release/app-release.apk"
[[ -s "$APK" ]] || fail "release APK not found: $APK"

APKSIGNER="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/build-tools/36.0.0/apksigner"
if [[ ! -x "$APKSIGNER" ]]; then
  APKSIGNER="$(command -v apksigner || true)"
fi
[[ -n "$APKSIGNER" && -x "$APKSIGNER" ]] || fail 'apksigner not found; install Android Build Tools 36.0.0'

ZIPALIGN="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/build-tools/36.0.0/zipalign"
if [[ ! -x "$ZIPALIGN" ]]; then
  ZIPALIGN="$(command -v zipalign || true)"
fi
[[ -n "$ZIPALIGN" && -x "$ZIPALIGN" ]] || fail 'zipalign not found; install Android Build Tools 36.0.0'

printf 'Checking APK alignment...\n'
"$ZIPALIGN" -c -P 16 -v 4 "$APK" >/dev/null

printf 'Checking APK signature...\n'
"$APKSIGNER" verify --verbose --print-certs "$APK"

printf '\nRelease artifact:\n'
ls -lh "$APK"
printf 'SHA-256: '
sha256sum "$APK" | awk '{print $1}'
printf '\nPreflight complete. Upload this exact APK to RuStore after final device smoke-test.\n'
