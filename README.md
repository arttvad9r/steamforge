# Steamforge

Оригинальная казуальная Android-игра: вариация механики 2048 в стимпанк-стилистике
с прогрессией мастерской, достижениями, гемами, ежедневными механиками и
рекламной монетизацией (Yandex Mobile Ads), аналитика (AppMetrica).

## Ядро

- Поле 4×4, свайпы/стрелки, объединение одинаковых деталей (level 1..11: Уголь → Механическое ядро, 2..2048).
- `GameEngine` — чистый Kotlin без Android/Compose: `GameState + Move → GameState`.
- Для обычной партии `GameViewModel` использует replayable PRNG: seed + позиция RNG сохраняются, поэтому следующий spawn после process death совпадает с непрерывной сессией.
- Steam Pressure / Overdrive живёт в `GameViewModel` (`ProgressionConfig`), движок не знает о мета-системах.
- Undo (2 бесплатных за партию, далее гемы), Wrench (удаление плитки ≤ 64 за гемы).
- Партия (доска + pressure/overdrive/undo/seed/RNG position) сохраняется в DataStore после каждого хода и полностью восстанавливается после process death. Формат сейва — v3 с чтением старых v2/v1-сейвов.
- Выход из незавершённой обычной партии сохраняет её без начисления XP; выход из Daily не выдаёт progression rewards.
- Завершённая партия получает уникальный `gameResultId`; rewarded-награда (x2 гемов) выдаётся идемпотентно на уровне репозитория.
- Daily Challenge награда атомарно защищена по `epochDay`: повторный вход/новый ViewModel не может выдать её второй раз.

## Структура

```
app/src/main/java/com/steamforge/game/
├── core/         GameEngine, GameState, Tile, Elements (чистое ядро, покрыто тестами)
├── progression/  XP/уровни, Steam Pressure, достижения, Daily Challenge (чистый Kotlin)
├── data/         DataStore-репозиторий (DataRepo + SteamforgeRepository),
│                 GameSaveCodec (v3/v2/v1), FinishedGameRecord (идемпотентность наград)
├── analytics/    Analytics (Noop) + MutableAnalytics (consent gate) + AppMetrica
├── monetization/ AdsManager (Yandex Mobile Ads: rewarded + ограниченный interstitial)
├── sound/        SfxPlayer (SoundPool + сгенерированные WAV)
├── ui/           game / workshop / achievements / settings (Compose + M3)
└── theme/        стимпанк-палитра и типографика
```

## Требования среды

- JDK 17 (Kotlin toolchain 17)
- Android SDK 36 (`compileSdk`), `minSdk 24`, `targetSdk 36`
- Android Studio или локальный Android SDK (`local.properties`, не коммитится)

## Конфигурация

Вся игровая настройка — `ProgressionConfig` (pressure/XP/undo/wrench/daily reward),
`AdsConfig` (частота interstitial), `GameRules` (спавн/победа).

### Production credentials (gradle properties / -P флаги, НЕ в git)

| Свойство | Назначение | Debug | Release без свойства |
|---|---|---|---|
| `steamforge.appmetricaApiKey` | AppMetrica API key | ключ используется, если задан | Noop-аналитика |
| `steamforge.rewardedAdUnitId` | Rewarded ad unit | всегда официальный demo-юнит Яндекса | **формат отключён** |
| `steamforge.interstitialAdUnitId` | Interstitial ad unit | всегда официальный demo-юнит Яндекса | **формат отключён** |
| `steamforge.privacyPolicyUrl` | URL Privacy Policy | плейсхолдер при пустом URL | плейсхолдер при пустом URL |

Production ad IDs никогда не используются debug-сборкой. Пустой production-ID в release просто отключает соответствующий рекламный формат.

```bash
./gradlew assembleRelease \
  -Psteamforge.appmetricaApiKey=XXXXXXXX \
  -Psteamforge.rewardedAdUnitId=R-M-XXXXXX-Y \
  -Psteamforge.interstitialAdUnitId=R-M-YYYYYY-Z \
  -Psteamforge.privacyPolicyUrl=https://example.com/privacy
```

Либо храните значения в `~/.gradle/gradle.properties` (вне репозитория).

### Privacy / consent

- При первом запуске показывается диалог выбора; опубликованную Privacy Policy можно открыть прямо из него до решения.
- До решения AppMetrica не активируется и рекламный SDK не инициализируется.
- Отказ отключает AppMetrica; реклама остаётся неперсонализированной (`YandexAds.setUserConsent(false)`), геолокация отключена.
- Выбор хранится локально и меняется в Настройках.
- «Сбросить прогресс» удаляет только игровые данные и сохраняет privacy choice, звук, вибрацию и анимации.
- Черновик политики: `docs/PRIVACY_POLICY_DRAFT.md`.
- Пакет карточки RuStore: `docs/RUSTORE_LISTING.md`.

## Команды

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew assembleRelease
python3 tools/gen_sounds.py
```

## Signing (release)

Ключ НЕ хранится в репозитории. Создайте `keystore.properties` в корне проекта
(в `.gitignore`) или передайте через CI-секреты:

```properties
storeFile=/absolute/path/to/steamforge-release.jks
storePassword=...
keyAlias=steamforge
keyPassword=...
```

Создание ключа:

```bash
keytool -genkeypair -v -keystore steamforge-release.jks \
  -alias steamforge -keyalg RSA -keysize 2048 -validity 10000
```

- Если `keystore.properties` существует → release подписывается им.
- Если нет → release собирается unsigned.

```bash
./gradlew assembleRelease
./gradlew bundleRelease
```

Проверка подписи: `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`

## Монетизация (политика)

- Rewarded — только по нажатию игрока (x2 гемов за партию), награда строго после `onRewarded`, идемпотентно по `gameResultId`.
- Rewarded availability — observable state; кнопка появляется, если реклама догрузилась уже на экране результата.
- Ошибки загрузки рекламы повторяются с ограниченным exponential backoff; offline startup не требует перезапуска процесса после восстановления сети.
- Interstitial — после 3-й завершённой партии и далее с шагом `AdsConfig.interstitialEveryGames`; рекламный момент хранится как pending и потребляется в следующей естественной паузе (`Заново`/выход).
- После успешно полученного rewarded interstitial в той же паузе не показывается; pending переносится дальше.
- Игра полностью работает офлайн и не блокируется отсутствием/ошибкой рекламы.

## Публикация в RuStore — pre-release checklist

Код и сборка:

- [ ] `./gradlew testDebugUnitTest` — зелёный
- [ ] `./gradlew lintDebug` — без ошибок
- [ ] `./gradlew assembleDebug assembleRelease` — зелёные
- [ ] Smoke test: consent, продолжение партии после process death, два Game Over подряд через «Заново», Daily idempotency, rewarded, interstitial, reset progress
- [ ] Release APK/AAB подписан production-ключом

Данные владельца (вне git):

- [ ] `steamforge.appmetricaApiKey`
- [ ] `steamforge.rewardedAdUnitId`, `steamforge.interstitialAdUnitId`
- [ ] `steamforge.privacyPolicyUrl`
- [ ] `keystore.properties` + независимый backup keystore
- [ ] Финальная иконка 512×512; минимум 3 реальных скриншота одного типа/ориентации (до 10), описания, возрастной рейтинг, категория
- [ ] E-mail поддержки в карточке и политике

## Известные ограничения V1

- Без backend: античит и синхронизация прогресса не предусмотрены.
- Идемпотентность наград защищает от повторов внутри устройства; восстановление из старого облачного бэкапа теоретически может повторить дневные награды.
- Смена consent для рекламы влияет на новые запросы; уже загруженная реклама обновится при следующей загрузке.
