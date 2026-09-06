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
- signed release APK и release AAB из одной source revision;
- `applicationId`, `versionCode`, `versionName` generated APK metadata;
- final APK/AAB-derived dependency/manifest inventory через `tools/check-release-inventory.sh`;
- merged manifest/permissions/DEX packages финального APK через Android `apkanalyzer`;
- AAB-derived universal APK через pinned official `bundletool`, проверенный по закреплённому SHA-256;
- resolved `releaseRuntimeClasspath` на отсутствие advertising/analytics SDK families;
- отсутствие AD_ID / AdServices advertising identifier permissions;
- 16 KiB compatibility APK;
- APK signature через `apksigner`;
- APK SHA-256 и certificate SHA-256;
- согласованность exact `dist/` APK, SHA file, metadata, certificate, package/version и inventory report через `tools/verify-rustore-release-artifact.sh`.

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
dist/Steamforge-<version>-vc<code>-inventory.txt
```

Inventory report содержит source commit, SHA-256 проверенных APK/AAB, artifact summaries/permissions и resolved release dependency graph. Release AAB собирается как дополнительный verification input; текущим RuStore upload artifact остаётся APK из `dist/`.

После этого не пересобирать APK между device smoke и загрузкой: проверять и загружать тот же файл из `dist/`.

## 5. Перед physical-device smoke

Ещё раз проверить exact artifact:

```bash
bash tools/verify-rustore-release-artifact.sh \
  dist/Steamforge-<version>-vc<code>-rustore.apk
```

Verifier не пересобирает приложение. Он повторно сверяет:

- APK SHA-256 с `.sha256` и `.metadata.txt`;
- certificate SHA-256 через `apksigner`;
- package/version через Android `apkanalyzer`;
- source commit metadata;
- APK SHA-256 с сохранённым inventory report.

Создайте локальную копию `docs/PHYSICAL_DEVICE_ACCEPTANCE.md` и заполняйте её фактическими устройствами/результатами. Не помечайте ручные пункты пройденными на основании emulator/hosted CI.

## 6. Финальный physical-device smoke

Установить production APK из `dist/` и проверить минимум:

- приложение сразу открывает Home без analytics/ad consent gate;
- normal run и восстановление active run;
- process death / background / screen-off restore;
- Game Over persistence/retry и Play Again;
- Daily / Contracts / Workshop / Collection;
- Settings и reset progress;
- offline startup/gameplay;
- отсутствие AppMetrica/advertising/consent/rewarded-video UI;
- отсутствие неожиданных сетевых зависимостей при пустом Remote Config endpoint;
- TalkBack / large text / safe-area spot-check;
- frame timing и 30–60 minute thermal/battery acceptance на реальном устройстве согласно release plan.

Фактические device/build/result/evidence записываются в `PHYSICAL_DEVICE_ACCEPTANCE.md` или его локальную release-candidate копию.

## 7. Перед загрузкой

После smoke, не пересобирая APK, повторить:

```bash
bash tools/verify-rustore-release-artifact.sh \
  dist/Steamforge-<version>-vc<code>-rustore.apk
```

SHA-256 должен совпасть с acceptance record и preflight metadata. Загружать только этот exact APK. Любая пересборка создаёт новый release candidate и требует повторной привязки inventory/device acceptance к новому SHA-256.

## AAB — если будет выбран как store upload позже

Production preflight уже собирает AAB для artifact-level verification, но текущая публикационная процедура использует APK. Если RuStore upload будет переведён на AAB, отдельно подтвердить актуальную процедуру store signing/upload key и не менять production signing key после первого опубликованного релиза без отдельной миграционной процедуры.
