# Steamforge

Steamforge — Android-игра на основе механики 2048 в premium stylized industrial-steampunk стилистике с прогрессией мастерской, контрактами, коллекциями и Daily/Weekly foundation.

**Текущий приоритет:** довести саму игру, game feel и визуальную систему до целевого качества. Работа по публикации в магазины, signing/upload, store listing, store assets и moderation сейчас не ведётся и не входит в активный roadmap.

**Продуктовый baseline:** без рекламы, без рекламных SDK, без AppMetrica и без пользовательской телеметрии. В интерфейсе нет consent/analytics/ad UI.

Главные источники направления:

- `docs/PRODUCT_PLAN.md` — порядок разработки;
- `docs/VISUAL_BIBLE.md` — утверждённое визуальное направление;
- `docs/DEVELOPMENT_STATUS.md` — текущее техническое состояние.

## Ядро

- Поле 4×4, свайпы/стрелки, объединение одинаковых деталей (2..2048+).
- `GameEngine` — чистый Kotlin без Android/Compose: `GameState + Move → MoveResult`.
- Replayable PRNG: seed + позиция RNG сохраняются для normal run и используются deterministic Weekly replay.
- Steam Pressure / Overdrive, Undo и Wrench находятся выше чистого движка.
- Normal run сохраняется в DataStore после значимых изменений и восстанавливается после process death.
- Текущий save codec — **v6**, без analytics/correlation identifier; старые `v5/v4/v3/v2/v1` читаются совместимо.
- Terminal result persistence идемпотентна и имеет retry/recovery path.
- Daily reward/contract claims защищены от повторной выдачи.

## Модули

```text
:app
  Android / Compose UI, DataStore, progression, Remote Config client

:weekly-core
  общий pure Kotlin GameEngine + ReplayableRandom + Weekly replay primitives

:weekly-protocol
  Weekly ranking domain/wire protocol

:weekly-server-core
  server-side replay/submission validation

:weekly-server-postgres
  accepted ranking population persistence

:weekly-server-ktor
  bounded authenticated HTTP transport

:macrobenchmark
  gameplay benchmark harness
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
- runtime analytics package;
- ad manager;
- rewarded-ad repository API;
- новых analytics correlation identifiers в сохранениях.

Сетевые разрешения используются только для явно выделенных product services, например опционального Remote Config и возможного будущего Weekly backend; это не рекламный/аналитический трафик.

## Android stack

- JDK 17 / Kotlin JVM toolchain 17
- `compileSdk = 36`
- `targetSdk = 36`
- `minSdk = 24`
- Jetpack Compose + Material 3/custom Steamforge UI
- Navigation 3
- Preferences DataStore
- R8/resource shrinking в minified build
- Macrobenchmark

## Конфигурация

Единственный сетевой app-level BuildConfig endpoint в текущем клиенте:

| Свойство | Назначение | Обязательное |
|---|---|---|
| `steamforge.remoteConfigUrl` | опциональный HTTPS Remote Config | нет; без него используются compiled defaults |

## Основные команды разработки

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew lintRelease
./gradlew assembleDebug
./gradlew assembleRelease
bash tools/check-android-16kb.sh
bash tools/check-no-tracking.sh
```

`assembleRelease` здесь используется как minified/R8 quality check, а не как шаг подготовки публикации.

## CI / runtime checks

Основные workflows:

- **Android CI** — unit/module tests, lint, debug/minified build, Macrobenchmark compilation и 16 KiB check;
- **UI Emulator Smoke** — фактический UI;
- **Android 17 / 16 KB Smoke** — API 37 / 16 KiB runtime environment;
- **Active Run Lifecycle Smoke** — recreation/background/process-death/offline recovery;
- **Adaptive Gameplay Window Smoke** — portrait/expanded/landscape;
- **Accessibility UI Smoke** — large text/touch geometry;
- **High Tier Tile Smoke** — tile readability/input;
- **Frame Timing Diagnostic Smoke** — rendering diagnostic;
- **Core Balance Simulation** — deterministic balance simulation.

## Visual direction

> **premium stylized industrial steampunk 2048 with clean gameplay, painterly atmospheric backgrounds, brass/steel materials, muted teal/patina accents and restrained ornament.**

Ключевые правила:

- gameplay — самый чистый экран;
- board и tiles доминируют над HUD/decor;
- крупные читаемые числа и плитки;
- глубина через свет, материал, bevel и тени, а не через визуальный шум;
- слегка стилизованные формы, но не chibi/mobile-cartoon;
- не уходить в photoreal;
- никакого generic fantasy-steampunk вроде дирижаблей/летающих островов;
- Workshop/Blueprints могут быть богаче gameplay по окружению и деталям.

Последние gameplay passes #156–158 уже двигают production UI в эту сторону, но визуальный уровень всей игры ещё считается незавершённым.

## Текущий порядок работы

1. core game feel и input/merge feedback;
2. gameplay visual до уровня Visual Bible;
3. общий visual system для Home/Workshop/Blueprints/Contracts/Profile/Settings;
4. Workshop presentation v2 с видимым восстановлением;
5. только затем — архитектурная разгрузка, reward layer и расширение meta;
6. Weekly/LiveOps/social/другие дальние системы — позже.

## Документация

- `docs/PRODUCT_PLAN.md` — активный product/development roadmap.
- `docs/VISUAL_BIBLE.md` — основной art-direction standard.
- `docs/DEVELOPMENT_STATUS.md` — фактический development baseline.
- `docs/ADR_0001_NO_ADS.md` — no-ads решение.
- `docs/ADR_0005_NO_USER_TELEMETRY.md` — no-user-telemetry решение.
- `docs/GAME_LOGIC_AUDIT_2026.md` — game-state consistency audit.
- `docs/ANDROID_2026_CHECKLIST.md` — технический Android checklist разработки.
- `docs/GAMEPLAY_VISUAL_POLISH_V1.md` — gameplay visual notes.

## Известные технические направления

- `GameViewModel`/repository требуют поэтапной декомпозиции, но не раньше, чем это реально помогает текущей работе над gameplay/visual.
- `weekly-core` исторически владеет общим `GameEngine`; semantic module split можно сделать позже.
- Preferences DataStore подходит текущему состоянию, но storage boundary нужно пересмотреть до существенного роста history/reward/event data.
- Weekly backend foundation существует, но его дальнейшее расширение сейчас не приоритет.

## Не делаем сейчас

- публикацию в любой app store;
- store listing / screenshots / moderation tooling;
- signing/upload pipeline;
- publication/release-candidate checklist;
- рекламу;
- пользовательскую аналитику;
- premature LiveOps/Season Pass/social infrastructure;
- архитектурные переписывания без прямой пользы для текущего gameplay/visual.