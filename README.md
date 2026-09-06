# Steamforge

Steamforge — Android-игра на основе механики 2048 в premium industrial-steampunk стилистике с прогрессией мастерской, контрактами, коллекциями и Daily/Weekly foundation.

**Продуктовый baseline:** без рекламы, без рекламных SDK, без AppMetrica и без пользовательской телеметрии. Игра не показывает consent/analytics/ad UI и не требует рекламных или аналитических credentials.

Текущая продуктовая цель и визуальный стандарт зафиксированы в `docs/PRODUCT_PLAN.md` и `docs/VISUAL_BIBLE.md`.

## Ядро

- Поле 4×4, свайпы/стрелки, объединение одинаковых деталей (2..2048+).
- `GameEngine` — чистый Kotlin без Android/Compose: `GameState + Move → MoveResult`.
- Replayable PRNG: seed + позиция RNG сохраняются для normal run и используются deterministic Weekly replay.
- Steam Pressure / Overdrive, Undo и Wrench находятся выше чистого движка.
- Normal run сохраняется в DataStore после значимых изменений и восстанавливается после process death.
- Save codec backward-readable; terminal result persistence идемпотентна и имеет retry/recovery path.
- Daily reward/contract claims защищены от повторной выдачи.
- Weekly replay authority вынесена в pure JVM-модули для повторной серверной валидации.

## Модули

```text
:app
  Android / Compose UI, DataStore, progression, Remote Config client

:weekly-core
  общий pure Kotlin GameEngine + ReplayableRandom + Weekly replay primitives

:weekly-protocol
  стабильный Weekly ranking domain/wire protocol

:weekly-server-core
  server-side replay/submission validation and ranking service boundary

:weekly-server-postgres
  accepted ranking population persistence

:weekly-server-ktor
  bounded authenticated HTTP transport

:macrobenchmark
  release-like gameplay benchmark harness
```

`weekly-core` исторически содержит общий `GameEngine`; это имя не означает, что normal gameplay зависит от сетевого Weekly backend.

## Privacy / tracking policy

Steamforge не содержит:

- рекламных SDK;
- rewarded/interstitial/banner/native рекламы;
- AppMetrica или другого analytics SDK;
- advertising identifiers;
- consent-диалога для рекламы/аналитики;
- analytics/ad switches в Settings;
- ad unit IDs или analytics API keys в release configuration.

Существующие внутренние typed event schemas временно могут оставаться как **in-process no-op compatibility layer** для безопасной поэтапной разгрузки ViewModel/прогрессии. Они никуда не отправляются, не сохраняются и не логируются.

Сетевые разрешения остаются нужны для опционального Remote Config и будущего Weekly backend; это не рекламный/аналитический трафик.

## Android stack

- JDK 17 / Kotlin JVM toolchain 17
- `compileSdk = 36`
- `targetSdk = 36`
- `minSdk = 24`
- Jetpack Compose + Material 3/custom Steamforge UI
- Navigation 3
- Preferences DataStore
- R8/resource shrinking для release
- Macrobenchmark

## Конфигурация

Единственный сетевой app-level BuildConfig endpoint в текущем клиенте:

| Свойство | Назначение | Обязательное |
|---|---|---|
| `steamforge.remoteConfigUrl` | опциональный HTTPS Remote Config | нет; без него используются compiled defaults |

Release signing credentials находятся только вне git. См. `docs/RELEASE_SIGNING.md`.

## Команды

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew bundleRelease
bash tools/check-android-16kb.sh
bash tools/build-rustore-release.sh
```

## CI / runtime checks

Основные workflows:

- **Android CI** — unit tests, lint, debug/release APK, AAB и module tests;
- **UI Emulator Smoke** — production UI;
- **Android 17 16 KB Smoke** — API 37 / 16 KiB runtime environment;
- **Active Run Lifecycle Smoke** — recreation/background/process-death/offline recovery;
- **Adaptive Gameplay Window Smoke** — portrait/expanded/landscape;
- **Accessibility UI Smoke** — large text/touch geometry;
- **High Tier Tile Smoke** — tile readability/input;
- **Frame Timing Diagnostic Smoke** — release-like rendering diagnostic;
- **RuStore Store Assets** — store screenshots/assets.

Physical-device performance, thermal and TalkBack checks остаются отдельным production gate.

## Release

Release key не хранится в репозитории. Первый RuStore APK создаётся через:

```bash
bash tools/build-rustore-release.sh
```

Preflight больше не требует AppMetrica key, Privacy Policy URL или ad unit IDs. Он проверяет source cleanliness, package confirmation, signing, tests/lint/build, 16 KiB compatibility, APK signature и SHA-256.

## Документация

- `docs/PRODUCT_PLAN.md` — product/development roadmap.
- `docs/VISUAL_BIBLE.md` — approved primary art direction.
- `docs/ADR_0001_NO_ADS.md` — обязательное no-ads решение.
- `docs/GAME_LOGIC_AUDIT_2026.md` — game-state consistency audit.
- `docs/BRANCH_AUDIT_2026-09-01.md` — historical branch consolidation decisions.
- `docs/ANDROID_2026_CHECKLIST.md` — Android/platform release checklist.
- `docs/RELEASE_STATUS.md` — фактический release baseline.
- `docs/RELEASE_SIGNING.md` — signing/release key process.
- `docs/RUSTORE_LISTING.md` — store listing/assets.

## Visual direction

> **premium stylized industrial steampunk 2048 with clean gameplay, painterly atmospheric backgrounds, brass/steel materials, muted teal accents and restrained ornament.**

Gameplay/utility screens должны оставаться чище showcase/meta surfaces. Concept screens — art-direction references, а не pixel-perfect production specs.

## Известные ограничения

- Weekly backend foundation существует, но production identity/deployment/client endpoint ещё не завершены; Weekly остаётся скрыт из обычной навигации.
- Remote Config опционален и всегда имеет offline-safe compiled fallback.
- Physical-device performance/thermal/TalkBack acceptance должен быть выполнен перед production release.
- Общий orchestration layer (`GameViewModel`, repository) требует дальнейшей поэтапной декомпозиции перед крупным LiveOps expansion; `GameEngine` переписывать не требуется.
