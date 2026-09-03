# Steamforge — Android 2026 Technical Checklist

**Актуальность:** 3 сентября 2026 года.  
**Основа:** проектный Android standard, применённый к фактическому Steamforge.

## Текущий stack

- Kotlin / JDK 17.
- Jetpack Compose + Material 3/custom Steamforge design system.
- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`.
- Navigation 3.
- DataStore persistence.
- AppMetrica + Yandex Mobile Ads behind privacy/consent handling.
- Pure Kotlin game core; gameplay UI/rendering — Compose/Canvas, не отдельный native game engine.

## Platform / distribution

- [x] `targetSdk >= 36`.
- [x] AAB build path exists.
- [x] Release signing path exists without committing keys.
- [ ] Android 17 / API 37 runtime compatibility должна оставаться зелёной в CI и быть повторно проверена перед Google Play release.
- [ ] Production artifact должен быть протестирован на реальных устройствах, не только emulator/CI.

## 64-bit / 16 KiB

Steamforge itself does not contain a custom NDK/game-engine layer, но third-party SDKs могут поставлять native `.so` libraries.

- [x] Есть `tools/check-android-16kb.sh`.
- [x] Есть Android 17 / 16 KiB smoke workflow в release-hardening линии.
- [ ] Каждая production-версия ads/analytics SDK должна оставаться совместимой с 64-bit и 16 KiB page size.
- [ ] Финальный release artifact повторно проверяется после dependency updates.

## Reliability / lifecycle

- [x] Active normal run сохраняется и восстанавливается после process death.
- [x] Deterministic RNG state сохраняется.
- [x] Save format backward-readable.
- [x] Rewarded result idempotent on-device.
- [x] Daily reward защищён от повторной выдачи в тот же день.
- [x] CI smoke покрывает Home/background/foreground, process kill и production-app offline startup/recovery/autosave при отключённой outbound connectivity.
- [ ] Перед production release проверить на реальном устройстве interrupted rewarded/interstitial flow уже после показа fullscreen ad.

## Observability / privacy

- [x] AppMetrica не активируется до положительного privacy-решения пользователя.
- [x] AppMetrica crash reporting оставлен включённым по умолчанию SDK.
- [x] AppMetrica ANR monitoring явно включён через `withAnrMonitoring(true)`; стандартный timeout SDK не переопределяется.
- [x] AppMetrica location tracking и advertising identifiers tracking отключены.
- [ ] Перед production release подтвердить, что crash/ANR reports реально поступают и читаются в production AppMetrica project.

## Input / UI

- [x] Touch swipe input.
- [x] Keyboard arrows в gameplay.
- [x] Compact UI handling.
- [x] Semantics/content descriptions в ключевых custom controls.
- [x] Accessibility UI Smoke проверяет production Home/Game при font scale 1.3, critical clickable targets >=48dp и runtime bounds внутри display.
- [ ] Перед production release выполнить ручной TalkBack smoke и spot-check дополнительных real-device safe-area/system-inset вариантов.
- [ ] Visual Bible changes не должны ухудшать gameplay readability; при изменениях gameplay UI сохранять зелёными high-tier, adaptive-window и accessibility gates.

## Performance

Для 2048-style puzzle цель — не максимальный FPS любой ценой, а стабильный response/frame pacing и низкая input latency.

- [ ] Профилировать release/release-like build.
- [ ] Записать frame time/stutter на low/mid/high Android devices.
- [ ] После расширения visual/VFX провести 30–60 minute thermal/battery session.
- [ ] Не держать тяжёлую animation/workload активной в static menus/background.
- [ ] Добавлять graphics quality tiers только если измерения показывают необходимость.

## Release gate

Перед финальным store build:

1. Unit tests и lint зелёные.
2. Debug + release build зелёные.
3. Android 17 / 16 KiB smoke зелёный либо инфраструктурный сбой явно отделён от app failure.
4. Accessibility UI Smoke зелёный на актуальном UI baseline.
5. Real-device smoke core/save/privacy/ads + TalkBack/large-text spot-check.
6. Production credentials только вне git.
7. Signed artifact SHA-256 зафиксирован; загружается тот же artifact.
8. Crash/ANR monitoring включён для production и проверен в AppMetrica.
