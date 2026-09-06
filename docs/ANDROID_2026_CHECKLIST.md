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
- [x] Daily/contract reward claims are protected against duplicate application.
- [x] Lifecycle smoke covers recreation, Home/background, force-stop relaunch, screen-off/wake and offline continuation.
- [ ] Keep these checks green through gameplay/visual refactors.

## Input / gameplay UI

- [x] Touch swipe input.
- [x] Keyboard arrows in gameplay.
- [x] Compact-landscape handling.
- [x] Expanded portrait/tablet board scaling.
- [x] Key custom controls have semantics/content descriptions.
- [x] Accessibility UI smoke checks font scale 1.3 and critical clickable geometry.
- [ ] Continue tuning swipe confidence/input latency from real gameplay use.
- [ ] Manually spot-check TalkBack/large text when major UI structure changes.
- [ ] Keep safe-area/system-inset behavior correct across target form factors.

## Visual quality

Source of truth: `docs/VISUAL_BIBLE.md`.

- [x] Gameplay visual hierarchy moved toward board-first composition.
- [x] Premium material tile pass exists.
- [x] Gameplay chrome has been reduced.
- [x] Expanded portrait/tablet board scaling exists.
- [ ] Validate tile readability across all high-tier values.
- [ ] Improve movement/merge/spawn/Overdrive visual sequencing.
- [ ] Bring Home and meta screens into one coherent material/typography/component system.
- [ ] Keep decorative workshop machinery away from critical gameplay space.
- [ ] Avoid chibi/mobile-cartoon drift and photoreal drift.

## Performance

For this puzzle the target is stable response/frame pacing and low input latency, not maximal FPS.

- [x] Macrobenchmark harness exists.
- [x] Hosted frame-timing diagnostic exists.
- [ ] Measure after significant animation/material/VFX changes.
- [ ] Ensure menus/backgrounds do not retain unnecessary continuous animation workload.
- [ ] Add graphics-quality tiers only if measurements justify them.

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