# Steamforge — Android 2026 Technical Checklist

**Назначение:** техническая проверка разработки. Это не checklist публикации и не план выхода в магазин.

## Stack

- Kotlin / JDK 17.
- Jetpack Compose + Material 3/custom Steamforge design system.
- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`.
- Navigation 3.
- Preferences DataStore persistence.
- Pure Kotlin game/replay core.
- Optional bounded HTTPS Remote Config.
- Weekly foundation in separate JVM/server modules.
- No advertising SDK.
- No AppMetrica or user analytics SDK.

## Platform compatibility

- [x] `targetSdk = 36`.
- [x] Android 17 / API 37 runtime smoke exists.
- [x] 16 KiB structural/runtime checks exist.
- [x] Minified `assembleRelease` build is exercised as a code-quality/R8 check.
- [ ] Re-run compatibility checks after dependency/toolchain changes.

## Privacy / tracking invariant

- [x] Advertising SDK dependency removed.
- [x] Advertising manifest metadata removed.
- [x] AppMetrica dependency/implementation removed.
- [x] Runtime analytics package/event taxonomy removed.
- [x] Analytics/ad BuildConfig credentials removed.
- [x] Startup consent dialog removed.
- [x] Analytics/advertising Settings surface removed.
- [x] Rewarded/interstitial gameplay paths removed.
- [x] Local analytics consent/correlation state removed.
- [x] `tools/check-no-tracking.sh` protects these invariants in CI.

`INTERNET` remains intentional for explicit non-telemetry product services such as optional Remote Config and possible future Weekly networking.

## Reliability / lifecycle

- [x] Active normal run saves/restores after process death.
- [x] Deterministic RNG position is persisted.
- [x] Save format remains backward-readable.
- [x] Terminal finish persistence is retryable/idempotent.
- [x] Short terminal writes no longer flash a blocking save modal; slow/failing writes retain visible feedback and retry (#181).
- [x] Daily/contract reward claims are protected against duplicate application.
- [x] Daily Challenge claim retries transient I/O and remains idempotent when a write may already have committed (#190).
- [x] Lifecycle smoke covers recreation, Home/background, force-stop relaunch, screen-off/wake and offline continuation.
- [ ] Keep these checks green through gameplay/visual refactors.
- [ ] Repeat representative lifecycle checks on a physical device before declaring game-readiness (#185).

## Input / gameplay UI

- [x] Touch swipe input.
- [x] Keyboard arrows in gameplay.
- [x] One latest direction is buffered while an animated visual turn settles (#179).
- [x] Undo/Wrench cannot rewrite the board mid-turn while animations are settling (#179).
- [x] Compact-landscape handling.
- [x] Expanded portrait/tablet board scaling.
- [x] Key custom controls have semantics/content descriptions.
- [x] Accessibility UI smoke checks font scale 1.3 and critical clickable geometry.
- [ ] Tune swipe confidence/input latency from real physical-device play (#185).
- [ ] Manually spot-check TalkBack/large text after the current whole-app visual pass (#185).
- [ ] Keep safe-area/system-inset behavior correct across target form factors.

## Visual quality

Source of truth: `docs/VISUAL_BIBLE.md`.

- [x] Gameplay visual hierarchy moved toward board-first composition.
- [x] Premium material tile pass exists.
- [x] Gameplay chrome has been reduced.
- [x] Expanded portrait/tablet board scaling exists.
- [x] Movement/merge/spawn/Overdrive visual sequencing has an automated baseline (#179).
- [x] Home, Workshop, Contracts/Daily, Profile, Achievements and Blueprints have focused visual-system passes (#172–176, #182–184).
- [x] Workshop progression creates visible world/restoration changes (#182).
- [x] Settings has dedicated production visual-regression coverage with suspiciously blank captures rejected (#188).
- [ ] Validate tile readability across high-tier values during long real sessions (#185).
- [ ] Complete final utility-surface/whole-app consistency acceptance.
- [ ] Complete final whole-app atmosphere vs gameplay-cleanliness acceptance.
- [ ] Avoid chibi/mobile-cartoon drift and photoreal drift.

## Performance

For this puzzle the target is stable response/frame pacing and low input latency, not maximal FPS.

- [x] Macrobenchmark harness exists.
- [x] Hosted frame-timing diagnostic exists.
- [x] Home does not create its infinite gear transition when Animations is disabled (#184).
- [ ] Measure after significant future animation/material/VFX changes.
- [ ] Continue checking menus/backgrounds for unnecessary continuous animation workload.
- [ ] Add graphics-quality tiers only if measurements justify them.

## Reward / progression invariant

- [x] Game-finish positive rewards use `RewardSystem`.
- [x] Daily Challenge positive rewards use `RewardSystem`.
- [x] Contract positive rewards use `RewardSystem`.
- [x] Daily Reward Gems / Workshop Parts / cosmetic unlock use `RewardSystem`.
- [ ] Keep future positive grants on this path instead of adding parallel reward mutation logic.

## Manual game-readiness gate

Issue #185 tracks the checks that hosted emulator CI cannot close honestly:

- physical swipe confidence and latency;
- long-session high-tier readability;
- SFX/haptic balance and fatigue;
- physical lifecycle restore;
- TalkBack / large-text / gesture-inset spot checks.

Do not mark those items complete from screenshot or hosted-emulator success alone.

## Weekly/backend boundary

- [x] Deterministic pure JVM replay authority exists.
- [x] Stable bounded ranking protocol exists.
- [x] Server-side replay validation exists.
- [x] PostgreSQL accepted-population store exists.
- [x] Bounded authenticated Ktor ranking route exists.
- [x] Weekly remains hidden while client provider is unavailable.
- [ ] Do not prioritize further Weekly deployment/client exposure until core gameplay and visual/meta quality reach the target level.

## Development gate for major gameplay/visual changes

Before merging a substantial gameplay or visual change:

1. Unit/module tests and lint green.
2. Debug + minified build green.
3. Android 17 / 16 KiB smoke green where relevant.
4. Lifecycle smoke green if state/UI flow changed.
5. High-tier/adaptive/accessibility smoke green if gameplay visuals changed.
6. Frame timing diagnostic reviewed if animation/VFX/material workload increased.
7. No-tracking guard green.
8. Visual result checked against `VISUAL_BIBLE.md`, not against historical store/publication concepts.
9. Physical-device acceptance items remain tracked separately in #185.
