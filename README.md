# Steamforge

Оригинальная казуальная Android-игра: вариация механики 2048 в стимпанк-стилистике
с прогрессией мастерской, достижениями, гемами, ежедневными механиками и
рекламной монетизацией (Yandex Mobile Ads), аналитика (AppMetrica).

## Ядро

- Поле 4×4, свайпы/стрелки, объединение одинаковых деталей (level 1..11: Уголь → Механическое ядро, 2..2048).
- `GameEngine` — чистый Kotlin без Android/Compose: `GameState + Move → GameState`, детерминированный `kotlin.random.Random(seed)`.
- Steam Pressure / Overdrive живёт в `GameViewModel` (`ProgressionConfig`), движок не знает о мета-системах.
- Undo (2 бесплатных за партию, далее гемы), Wrench (удаление плитки ≤ 64 за гемы).
- Партия (доска + pressure/overdrive/undo/seed) сохраняется в DataStore после каждого хода и
  полностью восстанавливается после process death. Формат сейва — v2 с чтением старых v1-сейвов.
- Завершённая партия получает уникальный `gameResultId`; rewarded-награда (x2 гемов) выдаётся
  идемпотентно на уровне репозитория — повторный callback SDK, повторный вход на экран и
  пересоздание процесса не выдают награду второй раз.

## Структура

```
app/src/main/java/com/steamforge/game/
├── core/         GameEngine, GameState, Tile, Elements (чистое ядро, покрыто тестами)
├── progression/  XP/уровни, Steam Pressure, достижения, Daily Challenge (чистый Kotlin)
├── data/         DataStore-репозиторий (DataRepo + SteamforgeRepository),
│                 GameSaveCodec (v2/v1), FinishedGameRecord (идемпотентность наград)
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

| Свойство | Назначение | Debug без свойства | Release без свойства |
|---|---|---|---|
| `steamforge.appmetricaApiKey` | AppMetrica API key | Noop-аналитика | Noop-аналитика |
| `steamforge.rewardedAdUnitId` | Rewarded ad unit | demo-юнит Яндекса | **формат отключён** |
| `steamforge.interstitialAdUnitId` | Interstitial ad unit | demo-юнит Яндекса | **формат отключён** |
| `steamforge.privacyPolicyUrl` | URL Privacy Policy (Настройки) | плейсхолдер-текст | плейсхолдер-текст |

Принцип безопасности: **demo-ID не подставляются в release автоматически**. Пустой
production-ID в release просто отключает соответствующий рекламный формат (игра не ломается).
Задать свойства можно одним из способов:

```bash
# 1) -P флаги
./gradlew assembleRelease \
  -Psteamforge.appmetricaApiKey=XXXXXXXX \
  -Psteamforge.rewardedAdUnitId=R-M-XXXXXX-Y \
  -Psteamforge.interstitialAdUnitId=R-M-YYYYYY-Z \
  -Psteamforge.privacyPolicyUrl=https://example.com/privacy

# 2) ~/.gradle/gradle.properties (домашний, вне git) — НЕ в gradle.properties проекта
```

### Privacy / consent

- При первом запуске показывается диалог выбора: статистика + персонализированная реклама — да/нет.
- До решения: AppMetrica не активируется, рекламный SDK не инициализируется, события не отправляются.
- Отказ: аналитика выключена, реклама — неперсонализированная (`YandexAds.setUserConsent(false)`), геолокация отключена.
- Выбор хранится локально и меняется в Настройках; геймплей не зависит от этого выбора.
- Черновик политики: `docs/PRIVACY_POLICY_DRAFT.md` (заполнить плейсхолдеры и опубликовать).

## Команды

```bash
./gradlew testDebugUnitTest   # юнит-тесты (ядро, прогрессия, VM, идемпотентность наград)
./gradlew lintDebug           # lint
./gradlew assembleDebug       # debug-сборка
./gradlew assembleRelease     # release-сборка (R8, resource shrinking)
python3 tools/gen_sounds.py   # регенерация звуков в res/raw
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

Создание ключа (один раз, хранить в безопасном месте + backup):

```bash
keytool -genkeypair -v -keystore steamforge-release.jks \
  -alias steamforge -keyalg RSA -keysize 2048 -validity 10000
```

- Если `keystore.properties` существует → release подписывается им.
- Если нет → release собирается unsigned (пригоден для CI и статических проверок).

Получение signed APK/AAB:

```bash
./gradlew assembleRelease        # app/build/outputs/apk/release/app-release.apk
./gradlew bundleRelease          # app/build/outputs/bundle/release/app-release.aab
```

Проверка подписи: `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`

## Монетизация (политика)

- Rewarded — только по нажатию игрока (x2 гемов за партию), награда строго после `onRewarded`,
  идемпотентно по `gameResultId`.
- Interstitial — после 3-й завершённой партии и далее каждую 5-ю, только в естественной паузе
  (выход с экрана партии после её завершения), никогда внутри активной партии и не при первом запуске.
- Игра полностью работает офлайн и не блокируется отсутствием/ошибкой рекламы.
- Некорректный production-ID (пусто) в release отключает формат, не ломая приложение.

## Публикация в RuStore — pre-release checklist

Код и сборка:

- [ ] `./gradlew testDebugUnitTest` — зелёный
- [ ] `./gradlew lintDebug` — без ошибок
- [ ] `./gradlew assembleDebug assembleRelease` — зелёные
- [ ] Smoke test на устройстве/эмуляторе: первый запуск (consent-диалог), партия, сворачивание/восстановление,
      game over → rewarded (demo) → x2, interstitial на 3-й партии, настройки, сброс прогресса
- [ ] Release APK: подписан production-ключом, `apksigner verify` проходит

Данные владельца (всё это — вне git):

- [ ] `steamforge.appmetricaApiKey` — production API key (AppMetrica → создать приложение)
- [ ] `steamforge.rewardedAdUnitId`, `steamforge.interstitialAdUnitId` — production ad units (Партнёрский интерфейс Яндекса)
- [ ] `steamforge.privacyPolicyUrl` — постоянный публичный URL опубликованной политики
- [ ] `keystore.properties` + физический backup keystore
- [ ] Финальная иконка 512×512 PNG (RuStore), скриншоты (мин. 2, JPEG/PNG 320–3840 px, 16:9/9:16),
      описания (короткое ≤80, полное ≤4000 знаков), возрастной рейтинг, категория
- [ ] Контакты: e-mail поддержки в карточке и в политике

В RuStore Console (не код):

- [ ] Создать приложение, заполнить карточку, загрузить AAB/APK
- [ ] Указать признак сбора данных (AppMetrica/Яндекс Ads) в анкете данных
- [ ] Пройти модерацию (политика должна быть доступна по URL без авторизации)
- [ ] Настроить страны распространения (или ограничение гео, если требование согласия отличается)

## Известные ограничения V1

- Без backend: античит и синхронизация прогресса не предусмотрены (progress локальный).
- Идемпотентность наград защищает от повторов внутри устройства; восстановление из старого
  облачного бэкапа теоретически может повторить дневные награды (без сервера неустранимо).
- Смена согласия для рекламы применяется к новым запросам сразу; для уже загруженной
  рекламы — к следующей загрузке.
