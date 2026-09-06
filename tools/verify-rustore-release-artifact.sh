#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

if [[ $# -ge 1 && -n "$1" ]]; then
  APK="$1"
else
  mapfile -t dist_apks < <(find dist -maxdepth 1 -type f -name 'Steamforge-*-rustore.apk' | sort)
  if [[ ${#dist_apks[@]} -ne 1 ]]; then
    printf 'ERROR: expected exactly one RuStore APK in dist/, found %s\n' "${#dist_apks[@]}" >&2
    printf '  %s\n' "${dist_apks[@]:-}" >&2
    exit 1
  fi
  APK="${dist_apks[0]}"
fi

[[ -s "$APK" ]] || fail "release APK not found or empty: $APK"
SHA_FILE="$APK.sha256"
METADATA_FILE="$APK.metadata.txt"
[[ -s "$SHA_FILE" ]] || fail "SHA-256 file not found or empty: $SHA_FILE"
[[ -s "$METADATA_FILE" ]] || fail "metadata file not found or empty: $METADATA_FILE"

read_metadata() {
  local key="$1"
  awk -F= -v k="$key" '$1 == k {sub(/^[^=]*=/, ""); print; exit}' "$METADATA_FILE"
}

EXPECTED_SHA="$(awk 'NF {print $1; exit}' "$SHA_FILE")"
[[ "$EXPECTED_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || fail "invalid SHA-256 in $SHA_FILE"
ACTUAL_SHA="$(sha256sum "$APK" | awk '{print $1}')"
[[ "${ACTUAL_SHA,,}" == "${EXPECTED_SHA,,}" ]] \
  || fail "APK SHA-256 mismatch: expected $EXPECTED_SHA, got $ACTUAL_SHA"

METADATA_SHA="$(read_metadata apkSha256)"
[[ -n "$METADATA_SHA" ]] || fail 'metadata is missing apkSha256'
[[ "${ACTUAL_SHA,,}" == "${METADATA_SHA,,}" ]] \
  || fail "metadata apkSha256 mismatch: expected $METADATA_SHA, got $ACTUAL_SHA"

SOURCE_COMMIT="$(read_metadata sourceCommit)"
APP_ID="$(read_metadata applicationId)"
VERSION_CODE="$(read_metadata versionCode)"
VERSION_NAME="$(read_metadata versionName)"
CERT_SHA256="$(read_metadata certificateSha256)"
INVENTORY_NAME="$(read_metadata inventoryReport)"

[[ "$SOURCE_COMMIT" =~ ^[0-9a-fA-F]{40}$ ]] || fail 'metadata sourceCommit is missing or invalid'
[[ -n "$APP_ID" ]] || fail 'metadata applicationId is missing'
[[ "$VERSION_CODE" =~ ^[0-9]+$ ]] || fail 'metadata versionCode is missing or invalid'
[[ -n "$VERSION_NAME" ]] || fail 'metadata versionName is missing'
[[ "$CERT_SHA256" =~ ^[0-9a-fA-F]{64}$ ]] || fail 'metadata certificateSha256 is missing or invalid'
[[ -n "$INVENTORY_NAME" ]] || fail 'metadata inventoryReport is missing'

if git rev-parse --git-dir >/dev/null 2>&1; then
  git cat-file -e "$SOURCE_COMMIT^{commit}" 2>/dev/null \
    || fail "metadata sourceCommit is not available in this repository: $SOURCE_COMMIT"
fi

APKSIGNER="${APKSIGNER:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/build-tools/36.0.0/apksigner}"
if [[ ! -x "$APKSIGNER" ]]; then
  APKSIGNER="$(command -v apksigner || true)"
fi
[[ -n "$APKSIGNER" && -x "$APKSIGNER" ]] || fail 'apksigner not found; install Android Build Tools 36.0.0'

APKSIGNER_OUTPUT="$("$APKSIGNER" verify --verbose --print-certs "$APK")"
ACTUAL_CERT_SHA256="$(printf '%s\n' "$APKSIGNER_OUTPUT" | awk -F': ' '/Signer #1 certificate SHA-256 digest:/ {print $2; exit}')"
[[ -n "$ACTUAL_CERT_SHA256" ]] || fail 'could not read signer certificate SHA-256 digest'
[[ "${ACTUAL_CERT_SHA256,,}" == "${CERT_SHA256,,}" ]] \
  || fail "certificate SHA-256 mismatch: expected $CERT_SHA256, got $ACTUAL_CERT_SHA256"

find_apkanalyzer() {
  if command -v apkanalyzer >/dev/null 2>&1; then
    command -v apkanalyzer
    return 0
  fi

  local sdk_root candidate
  sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  if [[ -n "$sdk_root" ]]; then
    candidate="$sdk_root/cmdline-tools/latest/bin/apkanalyzer"
    if [[ -x "$candidate" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
    candidate="$(find "$sdk_root/cmdline-tools" -type f -path '*/bin/apkanalyzer' -perm -u+x 2>/dev/null | sort -V | tail -n 1 || true)"
    if [[ -n "$candidate" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  fi
  return 1
}

APKANALYZER="$(find_apkanalyzer || true)"
[[ -n "$APKANALYZER" && -x "$APKANALYZER" ]] || fail 'apkanalyzer not found; install Android SDK Command-Line Tools'

BUILT_APP_ID="$("$APKANALYZER" manifest application-id "$APK")"
BUILT_VERSION_CODE="$("$APKANALYZER" manifest version-code "$APK")"
BUILT_VERSION_NAME="$("$APKANALYZER" manifest version-name "$APK")"
[[ "$BUILT_APP_ID" == "$APP_ID" ]] || fail "APK applicationId '$BUILT_APP_ID' does not match metadata '$APP_ID'"
[[ "$BUILT_VERSION_CODE" == "$VERSION_CODE" ]] || fail "APK versionCode '$BUILT_VERSION_CODE' does not match metadata '$VERSION_CODE'"
[[ "$BUILT_VERSION_NAME" == "$VERSION_NAME" ]] || fail "APK versionName '$BUILT_VERSION_NAME' does not match metadata '$VERSION_NAME'"

case "$INVENTORY_NAME" in
  */*) INVENTORY_FILE="$INVENTORY_NAME" ;;
  *) INVENTORY_FILE="$(dirname "$APK")/$INVENTORY_NAME" ;;
esac
[[ -s "$INVENTORY_FILE" ]] || fail "inventory report not found or empty: $INVENTORY_FILE"
INVENTORY_APK_SHA="$(awk -F= '$1 == "apkSha256" {sub(/^[^=]*=/, ""); print; exit}' "$INVENTORY_FILE")"
[[ -n "$INVENTORY_APK_SHA" ]] || fail 'inventory report is missing apkSha256'
[[ "${ACTUAL_SHA,,}" == "${INVENTORY_APK_SHA,,}" ]] \
  || fail "inventory APK SHA-256 mismatch: expected $INVENTORY_APK_SHA, got $ACTUAL_SHA"

printf 'RuStore release artifact verified unchanged.\n'
printf 'APK: %s\n' "$APK"
printf 'Package: %s\n' "$APP_ID"
printf 'Version: %s (%s)\n' "$VERSION_NAME" "$VERSION_CODE"
printf 'Source: %s\n' "$SOURCE_COMMIT"
printf 'APK SHA-256: %s\n' "$ACTUAL_SHA"
printf 'Certificate SHA-256: %s\n' "$ACTUAL_CERT_SHA256"
printf 'Inventory: %s\n' "$INVENTORY_FILE"
