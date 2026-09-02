# Steamforge — Android 2026 Technical Checklist

**Актуальность:** 1 сентября 2026 года.  
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
- [ ] Сохранять smoke coverage для Home/background/foreground, process kill, offline и interrupted ad flow.

## Input / UI

- [x] Touch swipe input.
- [x] Keyboard arrows в gameplay.
- [x] Compact UI handling.
- [x] Semantics/content descriptions в ключевых custom controls.
- [ ] Продолжать проверять 48dp touch targets, large font, TalkBack и safe areas при изменениях UI.
- [ ] Visual Bible changes не должны ухудшать gameplay readability.

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
4. Real-device smoke core/save/privacy/ads.
5. Production credentials только вне git.
6. Signed artifact SHA-256 зафиксирован; загружается тот же artifact.
7. Crash/ANR monitoring включён для production.
