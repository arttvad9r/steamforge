#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

forbidden_regex='io\.appmetrica|com\.yandex\.android:mobileads|com\.yandex\.mobile\.ads|steamforge\.appmetricaApiKey|steamforge\.rewardedAdUnitId|steamforge\.interstitialAdUnitId|APPMETRICA_API_KEY|REWARDED_AD_UNIT_ID|INTERSTITIAL_AD_UNIT_ID|AUTOMATIC_SDK_INITIALIZATION'

# Production/runtime/build configuration must stay free of tracking and advertising SDKs.
search_targets=(
  app/build.gradle.kts
  app/src/main/AndroidManifest.xml
  gradle/libs.versions.toml
  gradle.properties
  app/src/main/java
)

if grep -RInE --exclude='AdsManager.kt' "$forbidden_regex" "${search_targets[@]}"; then
  fail 'advertising or user-telemetry SDK/configuration reference found in production sources'
fi

# The temporary AdsManager compatibility shell is allowed only while it remains strictly inert and SDK-free.
compat='app/src/main/java/com/steamforge/game/monetization/AdsManager.kt'
if [[ -f "$compat" ]]; then
  if grep -Eq 'com\.yandex|AdRequest|RewardedAd|InterstitialAd|YandexAds|BuildConfig\.(REWARDED|INTERSTITIAL)' "$compat"; then
    fail 'AdsManager compatibility shell contains advertising SDK/runtime code'
  fi
  grep -Fq 'val enabled: Boolean = false' "$compat" || fail 'AdsManager compatibility shell must stay permanently disabled'
  grep -Fq 'MutableStateFlow(false)' "$compat" || fail 'rewarded readiness must stay false'
fi

# User-facing application code must not advertise analytics/ads controls or rewarded-video offers.
ui_targets=(
  app/src/main/java/com/steamforge/game/MainActivity.kt
  app/src/main/java/com/steamforge/game/Navigation.kt
  app/src/main/java/com/steamforge/game/ui/settings
)
if grep -RInE 'AppMetrica|аналитик|реклам|rewarded|interstitial|privacy.*consent|analyticsConsent' "${ui_targets[@]}"; then
  fail 'player-facing analytics/advertising/consent UI reference found'
fi

# Release tooling must reject obsolete credentials rather than require them.
for key in \
  steamforge.appmetricaApiKey \
  steamforge.rewardedAdUnitId \
  steamforge.interstitialAdUnitId; do
  grep -Fq "$key" tools/build-rustore-release.sh || fail "release preflight must explicitly reject obsolete property: $key"
done

printf 'No-tracking guard OK: no advertising SDK, AppMetrica SDK, tracking credentials or player-facing tracking controls found.\n'
