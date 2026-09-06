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
  mapfile -t release_apks < <(find app/build/outputs/apk/release -maxdepth 1 -type f -name '*.apk' | sort)
  if [[ ${#release_apks[@]} -ne 1 ]]; then
    printf 'ERROR: expected exactly one release APK, found %s\n' "${#release_apks[@]}" >&2
    printf '  %s\n' "${release_apks[@]:-}" >&2
    exit 1
  fi
  APK="${release_apks[0]}"
fi

AAB="${2:-app/build/outputs/bundle/release/app-release.aab}"
REPORT="${STEAMFORGE_INVENTORY_REPORT:-app/build/reports/release-inventory.txt}"

[[ -s "$APK" ]] || fail "release APK not found or empty: $APK"
[[ -s "$AAB" ]] || fail "release AAB not found or empty: $AAB"

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
[[ -n "$APKANALYZER" && -x "$APKANALYZER" ]] \
  || fail 'apkanalyzer not found; install Android SDK Command-Line Tools'

BUNDLETOOL_VERSION='1.18.3'
BUNDLETOOL_SHA256='a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29'
BUNDLETOOL_URL="https://github.com/google/bundletool/releases/download/${BUNDLETOOL_VERSION}/bundletool-all-${BUNDLETOOL_VERSION}.jar"

resolve_bundletool() {
  local jar cache_dir tmp actual
  if [[ -n "${BUNDLETOOL_JAR:-}" ]]; then
    jar="$BUNDLETOOL_JAR"
  else
    cache_dir="${XDG_CACHE_HOME:-$HOME/.cache}/steamforge/bundletool"
    jar="$cache_dir/bundletool-all-${BUNDLETOOL_VERSION}.jar"
    mkdir -p "$cache_dir"
    if [[ ! -s "$jar" ]]; then
      command -v curl >/dev/null 2>&1 || fail 'curl is required to fetch pinned bundletool'
      tmp="$jar.tmp.$$"
      rm -f "$tmp"
      curl --fail --location --retry 3 --proto '=https' --tlsv1.2 "$BUNDLETOOL_URL" -o "$tmp"
      actual="$(sha256sum "$tmp" | awk '{print $1}')"
      [[ "$actual" == "$BUNDLETOOL_SHA256" ]] \
        || { rm -f "$tmp"; fail "bundletool checksum mismatch: expected $BUNDLETOOL_SHA256, got $actual"; }
      mv "$tmp" "$jar"
    fi
  fi

  [[ -s "$jar" ]] || fail "bundletool jar not found or empty: $jar"
  actual="$(sha256sum "$jar" | awk '{print $1}')"
  [[ "$actual" == "$BUNDLETOOL_SHA256" ]] \
    || fail "bundletool checksum mismatch: expected $BUNDLETOOL_SHA256, got $actual"
  printf '%s\n' "$jar"
}

BUNDLETOOL_JAR_RESOLVED="$(resolve_bundletool)"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

inspect_apk() {
  local label="$1"
  local apk="$2"
  "$APKANALYZER" apk summary "$apk" > "$TMP_DIR/$label-summary.txt"
  "$APKANALYZER" manifest print "$apk" > "$TMP_DIR/$label-manifest.xml"
  "$APKANALYZER" manifest permissions "$apk" > "$TMP_DIR/$label-permissions.txt"
  "$APKANALYZER" files list "$apk" > "$TMP_DIR/$label-files.txt"
  "$APKANALYZER" dex packages --defined-only "$apk" > "$TMP_DIR/$label-dex-packages.txt"
}

inspect_apk direct-apk "$APK"

APKS="$TMP_DIR/from-aab.apks"
java -jar "$BUNDLETOOL_JAR_RESOLVED" build-apks \
  --bundle="$AAB" \
  --output="$APKS" \
  --mode=universal \
  --overwrite >/dev/null

mapfile -t universal_entries < <(unzip -Z1 "$APKS" | grep -E '(^|/)universal\.apk$')
[[ ${#universal_entries[@]} -eq 1 ]] \
  || fail "expected exactly one universal APK in bundletool output, found ${#universal_entries[@]}"
AAB_UNIVERSAL_APK="$TMP_DIR/from-aab-universal.apk"
unzip -p "$APKS" "${universal_entries[0]}" > "$AAB_UNIVERSAL_APK"
[[ -s "$AAB_UNIVERSAL_APK" ]] || fail 'bundletool produced an empty universal APK'
inspect_apk aab-universal "$AAB_UNIVERSAL_APK"

printf 'Resolving release runtime dependency inventory...\n'
./gradlew --no-daemon --console=plain :app:dependencies \
  --configuration releaseRuntimeClasspath > "$TMP_DIR/release-runtime-dependencies.txt"

forbidden_namespace_regex='io\.appmetrica|com\.yandex\.mobile\.ads|com\.google\.android\.gms\.ads|com\.google\.android\.gms\.analytics|com\.google\.firebase\.analytics|com\.appsflyer|com\.adjust\.sdk|com\.amplitude|com\.mixpanel|com\.facebook\.appevents|com\.segment\.analytics|com\.flurry\.android|io\.branch|com\.microsoft\.appcenter\.analytics|com\.kochava|com\.singular\.sdk|com\.tenjin'
forbidden_dependency_regex='com\.yandex\.android:mobileads|com\.google\.android\.gms:play-services-(ads|analytics)|com\.google\.firebase:firebase-analytics|appmetrica|appsflyer|adjust-android|amplitude.*analytics|mixpanel|flurry|branch.*android|appcenter-analytics|kochava|singular.*sdk|tenjin|privacysandbox.*ads'
forbidden_permission_regex='com\.google\.android\.gms\.permission\.AD_ID|android\.permission\.ACCESS_ADSERVICES_(AD_ID|ATTRIBUTION|TOPICS)'

artifact_files=(
  "$TMP_DIR/direct-apk-manifest.xml"
  "$TMP_DIR/direct-apk-permissions.txt"
  "$TMP_DIR/direct-apk-files.txt"
  "$TMP_DIR/direct-apk-dex-packages.txt"
  "$TMP_DIR/aab-universal-manifest.xml"
  "$TMP_DIR/aab-universal-permissions.txt"
  "$TMP_DIR/aab-universal-files.txt"
  "$TMP_DIR/aab-universal-dex-packages.txt"
)

if grep -Ein "$forbidden_namespace_regex" "${artifact_files[@]}"; then
  fail 'advertising or user-telemetry SDK namespace found in final release artifacts'
fi

if grep -Ein "$forbidden_permission_regex" \
  "$TMP_DIR/direct-apk-manifest.xml" \
  "$TMP_DIR/direct-apk-permissions.txt" \
  "$TMP_DIR/aab-universal-manifest.xml" \
  "$TMP_DIR/aab-universal-permissions.txt"; then
  fail 'advertising/attribution permission found in final release manifest'
fi

if grep -Ein "$forbidden_dependency_regex" "$TMP_DIR/release-runtime-dependencies.txt"; then
  fail 'advertising or user-telemetry dependency found in releaseRuntimeClasspath'
fi

mkdir -p "$(dirname "$REPORT")"
{
  printf 'Steamforge release artifact inventory\n'
  printf '===================================\n'
  printf 'sourceCommit=%s\n' "$(git rev-parse HEAD 2>/dev/null || printf unknown)"
  printf 'apk=%s\n' "$APK"
  printf 'apkSha256=%s\n' "$(sha256sum "$APK" | awk '{print $1}')"
  printf 'aab=%s\n' "$AAB"
  printf 'aabSha256=%s\n' "$(sha256sum "$AAB" | awk '{print $1}')"
  printf 'bundletoolVersion=%s\n' "$BUNDLETOOL_VERSION"
  printf '\nDirect release APK summary\n'
  cat "$TMP_DIR/direct-apk-summary.txt"
  printf '\nDirect release APK permissions\n'
  cat "$TMP_DIR/direct-apk-permissions.txt"
  printf '\nAAB-derived universal APK summary\n'
  cat "$TMP_DIR/aab-universal-summary.txt"
  printf '\nAAB-derived universal APK permissions\n'
  cat "$TMP_DIR/aab-universal-permissions.txt"
  printf '\nResolved releaseRuntimeClasspath\n'
  cat "$TMP_DIR/release-runtime-dependencies.txt"
} > "$REPORT"

printf 'Release artifact inventory OK: APK + AAB-derived universal APK contain no prohibited ad/analytics namespaces or permissions.\n'
printf 'Inventory report: %s\n' "$REPORT"
