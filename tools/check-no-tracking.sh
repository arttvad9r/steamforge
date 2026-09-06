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

if grep -RInE "$forbidden_regex" "${search_targets[@]}"; then
  fail 'advertising or user-telemetry SDK/configuration reference found in production sources'
fi

# Compatibility runtime code is no longer allowed to exist.
[[ ! -f app/src/main/java/com/steamforge/game/monetization/AdsManager.kt ]] \
  || fail 'AdsManager compatibility shell must not exist'
[[ ! -d app/src/main/java/com/steamforge/game/analytics ]] \
  || fail 'runtime analytics package must not exist'

if grep -RInE 'AdsManager|com\.steamforge\.game\.analytics|claimDoubleReward|rewardedClaimed|analyticsConsent' app/src/main/java; then
  fail 'legacy advertising, analytics or consent API reference found in runtime sources'
fi

# Player-facing application code must not contain analytics/ad controls or rewarded-video offers.
# Do not ban the generic word "rewarded": RewardSystem uses it for ordinary local game rewards.
ui_targets=(
  app/src/main/java/com/steamforge/game/MainActivity.kt
  app/src/main/java/com/steamforge/game/Navigation.kt
  app/src/main/java/com/steamforge/game/ui
)
if grep -RInE 'AppMetrica|аналитик|реклам|showRewarded|rewardedAvailable|rewardDoubled|interstitial|за видео|УДВОИТЬ ГЕМЫ|privacy.*consent|analyticsConsent' "${ui_targets[@]}"; then
  fail 'player-facing analytics/advertising/consent UI reference found'
fi

# New save/runtime code must not create analytics correlation identifiers. GameSaveCodec may only
# mention analyticsRunId while decoding the legacy v5 format for backward compatibility.
if grep -RInE --exclude='GameSaveCodec.kt' 'analyticsRunId|runAnalyticsId' app/src/main/java; then
  fail 'analytics correlation identifier found outside the legacy save decoder'
fi

# Release tooling must reject obsolete credentials rather than require them.
for key in \
  steamforge.appmetricaApiKey \
  steamforge.rewardedAdUnitId \
  steamforge.interstitialAdUnitId; do
  grep -Fq "$key" tools/build-rustore-release.sh || fail "release preflight must explicitly reject obsolete property: $key"
done

printf 'No-tracking guard OK: no advertising SDK/runtime, analytics runtime, tracking credentials, legacy ad APIs or player-facing tracking controls found.\n'
