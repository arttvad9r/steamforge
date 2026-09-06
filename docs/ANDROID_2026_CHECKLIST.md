# Steamforge — Android 2026 Technical Checklist

**Актуальность:** 6 сентября 2026 года.  
**Основа:** текущий Steamforge `master` + accepted ADR 0001/0005.

## Текущий stack

- Kotlin / JDK 17.
- Jetpack Compose + Material 3/custom Steamforge design system.
- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`.
- Navigation 3.
- Preferences DataStore persistence.
- Pure Kotlin game/replay core.
- Optional bounded HTTPS Remote Config.
- Weekly server foundation in separate pure JVM/server modules.
- **No advertising SDK.**
- **No AppMetrica or user analytics SDK.**

## Platform / distribution

- [x] `targetSdk >= 36`.
- [x] AAB build path exists.
- [x] Release signing path exists without committing keys.
- [x] Android 17 / API 37 runtime smoke workflow exists.
- [x] 16 KiB structural/runtime checks exist.
- [ ] Final production artifact must be tested on physical devices before release.

## 64-bit / 16 KiB

Steamforge has no custom NDK/game-engine layer. Any future dependency that introduces native `.so` files must pass the existing 16 KiB checks.

- [x] `tools/check-android-16kb.sh` exists.
- [x] Android 17 / 16 KiB smoke workflow exists.
- [x] Advertising/analytics SDK native compatibility risk removed with those SDKs.
- [ ] Re-run compatibility checks after every dependency/toolchain update.

## Privacy / tracking

- [x] Advertising SDK dependency removed.
- [x] Advertising manifest metadata removed.
- [x] AppMetrica dependency/implementation removed.
- [x] Analytics API key and ad unit BuildConfig fields removed.
- [x] Startup consent dialog removed.
- [x] Analytics/advertising Settings surface removed.
- [x] Production release preflight no longer requests analytics/ad credentials.
- [x] App-level analytics implementation is a strict in-process no-op during compatibility cleanup.
- [ ] Before release, verify the final APK dependency/manifest inventory still contains no analytics/ad SDK.

Network permission remains intentional for non-telemetry product services (optional Remote Config / future Weekly backend).

## Reliability / lifecycle

- [x] Active normal run saves/restores after process death.
- [x] Deterministic RNG position is persisted.
- [x] Save format remains backward-readable.
- [x] Terminal finish persistence is retryable/idempotent.
- [x] Daily/contract reward claims are protected against duplicate application.
- [x] Lifecycle smoke covers recreation, Home/background, force-stop relaunch, screen-off/wake and offline continuation.

## Input / UI

- [x] Touch swipe input.
- [x] Keyboard arrows in gameplay.
- [x] Compact-landscape handling.
- [x] Expanded portrait/tablet board scaling.
- [x] Semantics/content descriptions in key custom controls.
- [x] Accessibility UI Smoke checks font scale 1.3, critical clickable targets >=48dp and runtime bounds.
- [ ] Manual TalkBack smoke on physical devices.
- [ ] Real-device safe-area/system-inset spot-checks.
- [ ] Visual Bible changes must keep high-tier/adaptive/accessibility gates green.

## Performance

For a 2048-style puzzle the target is stable response/frame pacing and low input latency, not maximal FPS.

- [x] Release-like Macrobenchmark harness exists.
- [x] Hosted frame-timing diagnostic exists.
- [ ] Record physical-device frame timing on low/mid/high Android devices.
- [ ] Run a 30–60 minute thermal/battery session after final visual/VFX changes.
- [ ] Ensure menus/background do not retain heavy animation workload.
- [ ] Add graphics-quality tiers only if measurements justify them.

## Weekly/backend boundary

- [x] Deterministic pure JVM replay authority exists.
- [x] Stable bounded ranking protocol exists.
- [x] Server-side replay validation exists.
- [x] PostgreSQL accepted-population store exists.
- [x] Bounded authenticated Ktor ranking route exists.
- [ ] Production identity/session issuance must be completed and reviewed before Weekly UI exposure.
- [ ] Production deployment/secrets/rate-abuse/observability must be defined before player exposure.
- [ ] Weekly must remain hidden while the client provider is unavailable.

## Release gate

Before final store build:

1. Unit/module tests and lint green.
2. Debug + release build and AAB green.
3. Android 17 / 16 KiB smoke green, with infrastructure failures distinguished from app failures.
4. Lifecycle, high-tier/input, adaptive-window and accessibility UI gates green.
5. Confirm final APK/AAB contains no advertising or analytics SDK/declarations.
6. Physical-device core/save/offline + TalkBack/large-text + performance/thermal spot-check.
7. Production signing credentials only outside git.
8. Signed artifact SHA-256 recorded; upload exactly the verified artifact.
9. No analytics key, ad unit ID or advertising/privacy-consent configuration is a release requirement.
