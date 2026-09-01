# Steamforge

Оригинальная казуальная Android-игра: вариация механики 2048 в premium industrial-steampunk стилистике с прогрессией мастерской, достижениями, гемами, ежедневными механиками, рекламной монетизацией (Yandex Mobile Ads) и аналитикой (AppMetrica).

Текущая продуктовая цель и визуальный стандарт зафиксированы в `docs/PRODUCT_PLAN.md` и `docs/VISUAL_BIBLE.md`.

## Ядро

- Поле 4×4, свайпы/стрелки, объединение одинаковых деталей (level 1..11: Уголь → Механическое ядро, 2..2048).
- `GameEngine` — чистый Kotlin без Android/Compose: `GameState + Move → GameState`.
- Для обычной партии `GameViewModel` использует replayable PRNG: seed + позиция RNG сохраняются, поэтому следующий spawn после process death совпадает с непрерывной сессией.
- Steam Pressure / Overdrive живёт в `GameViewModel` (`ProgressionConfig`), движок не знает о мета-системах.
- Undo (2 бесплатных за партию, далее гемы), Wrench (удаление плитки ≤ 64 за гемы).
- Партия сохраняется в DataStore после каждого значимого изменения и полностью восстанавливается после process death.
- Формат сейва — **v4**: board + pressure/overdrive/undo + seed/RNG position + сессионные счётчики статистики. Чтение старых `v3/v2/v1` сохранено.
- Выход из незавершённой обычной партии сохраняет её без начисления XP; выход из Daily не выдаёт progression rewards.
- Завершённая партия получает уникальный `gameResultId`; rewarded-награда (x2 гемов) выдаётся идемпотентно на уровне репозитория.
- Daily Challenge награда атомарно защищена по `epochDay`: повторный вход/новый ViewModel не может выдать её второй раз.

## Структура

```text
app/src/main/java/com/steamforge/game/
├── core/         GameEngine, GameState, Tile, Elements — чистое ядро
├── progression/  XP/уровни, Steam Pressure, достижения, Daily Challenge
├── data/         DataStore repository, GameSaveCodec v4/v3/v2/v1,
│                 FinishedGameRecord и persistence safeguards
├── analytics/    Analytics + MutableAnalytics consent gate + AppMetrica
├── monetization/ AdsManager — Yandex rewarded + ограниченный interstitial
├── sound/        SfxPlayer / SoundPool
├── ui/           game / workshop / achievements / settings — Compose
└── theme/        Steamforge palette, typography and components
```

Gameplay renderer/UI сейчас построен на Compose/Canvas. Legacy Android `View` не является частью production gameplay architecture.

## Требования среды

- JDK 17 / Kotlin JVM toolchain 17
- Android SDK 36 (`compileSdk = 36`)
- `minSdk = 24`
- `targetSdk = 36`
- Android Studio или локальный Android SDK (`local.properties`, не коммитится)

## Конфигурация

Игровая конфигурация в текущем V1 сосредоточена в `ProgressionConfig`, `AdsConfig` и `GameRules`.

### Production credentials — только вне git

| Свойство | Назначение | Debug | Release без свойства |
|---|---|---|---|
| `steamforge.appmetricaApiKey` | AppMetrica API key | используется, если задан | Noop analytics |
| `steamforge.rewardedAdUnitId` | Rewarded ad unit | demo unit | формат отключён |
| `steamforge.interstitialAdUnitId` | Interstitial ad unit | demo unit | формат отключён |
| `steamforge.privacyPolicyUrl` | Privacy Policy URL | placeholder при пустом | placeholder при пустом |

Production ad IDs никогда не используются debug-сборкой.

## Privacy / consent

- При первом запуске пользователь принимает решение до активации analytics/ads flow.
- До решения AppMetrica не активируется и рекламный SDK не инициализируется приложением.
- Отказ отключает AppMetrica; реклама запрашивается как non-personalized, геолокация отключена.
- Выбор хранится локально и меняется в Settings.
- Reset progress удаляет игровые данные, но сохраняет privacy choice и пользовательские настройки.
- Черновик политики: `docs/PRIVACY_POLICY_DRAFT.md`.

## Команды

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew bundleRelease
python3 tools/gen_sounds.py
bash tools/check-android-16kb.sh
```

## CI / runtime checks

Основные workflows:

- **Android CI** — unit tests, lint, debug/release build, tooling/privacy guards, release checks;
- **UI Emulator Smoke** — основные экраны и compact layout;
- **Android 17 16 KB Smoke** — API 37 / 16 KiB runtime environment;
- **RuStore Store Assets** — реальные вертикальные store screenshots.

Android 17 / 16 KiB проверка важна и для Kotlin/Compose проекта, потому что сторонние ads/analytics SDK могут содержать native `.so`.

Проектный checklist: `docs/ANDROID_2026_CHECKLIST.md`.

## Signing / RuStore

Release key не хранится в репозитории. Полная инструкция: `docs/RELEASE_SIGNING.md`.

Для первого RuStore release используется production-signed APK, созданный через preflight:

```bash
bash tools/build-rustore-release.sh
```

Публиковать нужно именно проверенный `dist/Steamforge-<version>-vc<code>-rustore.apk` и соответствующий `.sha256`.

Store copy/assets: `docs/RUSTORE_LISTING.md`.
Release status: `docs/RELEASE_STATUS.md`.
Release notes: `docs/RELEASE_NOTES_V1.md`.

## Монетизация V1

- Rewarded — только по явному действию игрока; награда после reward callback и идемпотентно по `gameResultId`.
- Rewarded availability — observable state.
- Ad load failures используют ограниченный retry/backoff; offline startup не блокирует игру.
- Interstitial показывается только в естественных паузах и frequency-capped.
- После rewarded interstitial в той же паузе не навязывается.
- Игра полностью работает офлайн без рекламы.

## Документация

- `docs/PRODUCT_PLAN.md` — целевое развитие Steamforge и порядок систем.
- `docs/VISUAL_BIBLE.md` — **approved primary art direction**; concept screens являются reference, не pixel-perfect spec.
- `docs/GAME_RESEARCH.md` — общие паттерны top mobile games 2026, не feature-spec Steamforge.
- `docs/GAME_LOGIC_AUDIT_2026.md` — аудит game-state consistency.
- `docs/BRANCH_AUDIT_2026-09-01.md` — решения по консолидации веток.
- `docs/ANDROID_2026_CHECKLIST.md` — Android/platform checklist для проекта.
- `docs/RELEASE_STATUS.md` — фактический release baseline.
- `docs/PRIVACY_POLICY_DRAFT.md`, `docs/RELEASE_SIGNING.md`, `docs/RUSTORE_LISTING.md` — release/privacy/store документы.

## Visual direction

Основной концепт принят 01.09.2026:

> **premium stylized industrial steampunk 2048 with clean gameplay, painterly atmospheric backgrounds, brass/steel materials, muted teal accents and restrained ornament.**

HEXSTORM: Tears of Arcadia используется только как reference по polish, painterly stylization и atmospheric lighting. Gameplay/utility screens должны быть заметно чище showcase/meta screens.

## Известные ограничения V1

- Нет backend/account/cloud sync/global leaderboard.
- Anti-cheat не является server-authoritative.
- Идемпотентность rewards защищает локальные повторы, но не все возможные cross-device/old-backup сценарии.
- Billing/IAP/Remove Ads ещё не являются частью V1 production path.
- Текущие Daily/Workshop systems существуют, но будущая product architecture планирует их постепенно привести к unified RewardSystem → Contracts → Blueprints → reusable LiveOps.
