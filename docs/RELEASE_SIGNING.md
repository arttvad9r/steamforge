# Steamforge — release signing для RuStore

**Актуализировано:** 06.09.2026.

## Baseline

Для первого релиза Steamforge используется production-signed APK. Реклама и пользовательская аналитика отсутствуют: release build не требует AppMetrica key, ad unit IDs или consent/privacy configuration для SDK.

Текущий package ID:

```text
com.steamforge.game
```

После первой публикации package ID нельзя менять без создания нового приложения, поэтому production preflight требует отдельного подтверждения.

## 1. Подтвердить applicationId

В локальном `~/.gradle/gradle.properties`:

```properties
steamforge.confirmApplicationId=com.steamforge.game
```

Это не секрет. Не добавляйте obsolete AppMetrica/ad properties: они не используются и release tooling рассматривает их как устаревшую конфигурацию.

## 2. Создать и сохранить signing key

```bash
keytool -genkeypair -v \
  -keystore steamforge-release.jks \
  -alias steamforge \
  -keyalg RSA \
  -keysize 2048 \
  -validity 36500
```

Сохранить:

- `steamforge-release.jks`;
- store password;
- alias;
- key password.

Сделать минимум две независимые резервные копии ключа и паролей.

## 3. Локальная signing-конфигурация

Создать `keystore.properties` в корне проекта; файл исключён из git:

```properties
storeFile=/absolute/path/to/steamforge-release.jks
storePassword=...
keyAlias=steamforge
keyPassword=...
```

Не коммитить keystore, passwords или generated release artifacts.

## 4. Production preflight

Рабочее дерево должно быть чистым. Запустить:

```bash
bash tools/build-rustore-release.sh
```

Preflight проверяет:

- clean git state и фиксирует source `HEAD`;
- `steamforge.confirmApplicationId`;
- отсутствие obsolete tracking/ad properties в tracked `gradle.properties`;
- наличие и git-safety keystore;
- unit tests и lint;
- signed release APK;
- `applicationId`, `versionCode`, `versionName` generated metadata;
- 16 KiB compatibility;
- APK signature через `apksigner`;
- APK SHA-256 и certificate SHA-256.

Tracking/ad credentials **не являются release input**. В частности, не требуются:

```text
steamforge.appmetricaApiKey
steamforge.rewardedAdUnitId
steamforge.interstitialAdUnitId
steamforge.privacyPolicyUrl   # больше не является SDK/preflight credential
```

Privacy/store policy может всё равно потребовать опубликованную страницу с информацией о данных/сети; это store/legal artifact, а не параметр приложения или analytics SDK.

Успешный preflight создаёт:

```text
dist/Steamforge-<version>-vc<code>-rustore.apk
dist/Steamforge-<version>-vc<code>-rustore.apk.sha256
dist/Steamforge-<version>-vc<code>-rustore.apk.metadata.txt
```

После этого не пересобирать APK между device smoke и загрузкой: проверять и загружать тот же файл из `dist/`.

## 5. Финальный physical-device smoke

Установить production APK из `dist/` и проверить минимум:

- приложение сразу открывает Home без analytics/ad consent gate;
- normal run и восстановление active run;
- process death / background / screen-off restore;
- Game Over persistence/retry и Play Again;
- Daily / Contracts / Workshop / Collection;
- Settings и reset progress;
- offline startup/gameplay;
- отсутствие AppMetrica/advertising/consent/rewarded-video UI;
- отсутствие неожиданных сетевых запросов при пустом Remote Config endpoint;
- TalkBack / large text spot-check;
- frame timing/thermal acceptance на реальном устройстве.

После smoke повторно проверить SHA-256 и загрузить именно проверенный APK.

## AAB — если будет выбран позже

При переходе на AAB соблюдать актуальную процедуру RuStore для app signing key и upload key. Не менять production signing key после первого опубликованного релиза без отдельной миграционной процедуры.
