# Steamforge — release signing для RuStore

Актуализировано: 31.08.2026.

## Рекомендация для первого релиза

Для V1 Steamforge предпочтителен **подписанный APK**.

Причины:

- текущая release-сборка около 5 МБ, поэтому выигрыш AAB по размеру для первой версии невелик;
- RuStore принимает и APK, и AAB;
- APK требует только стабильный ключ подписи приложения;
- AAB в RuStore требует отдельной загрузки подписи приложения и сертификата upload key, поэтому добавляет лишний операционный шаг перед первым релизом.

После того как первый релиз стабильно опубликован, при необходимости можно перейти на AAB по официальной процедуре RuStore.

## APK: схема первого релиза

### 1. Сначала подтвердить `applicationId`

Текущий Android package ID:

```text
com.steamforge.game
```

После первой публикации идентификатор приложения менять нельзя без создания нового приложения в магазине. Поэтому production preflight требует отдельного явного подтверждения. После финального решения добавить в локальный `~/.gradle/gradle.properties`:

```properties
steamforge.confirmApplicationId=com.steamforge.game
```

Если package ID будет изменён до первого релиза, сначала изменить `applicationId`/`namespace`, прогнать CI и только затем записать новое значение `steamforge.confirmApplicationId`.

### 2. Создать ключ подписи

Ключ создаёт и хранит владелец приложения. Не добавлять его в git и не передавать в публичные сервисы.

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
- alias (`steamforge`);
- key password.

Сделать минимум две независимые резервные копии keystore и паролей.

### 3. Локальная конфигурация

Создать в корне проекта `keystore.properties` — файл уже исключён из git:

```properties
storeFile=/absolute/path/to/steamforge-release.jks
storePassword=...
keyAlias=steamforge
keyPassword=...
```

Production-параметры приложения задавать через `~/.gradle/gradle.properties`, а не коммитить в репозиторий:

```properties
steamforge.confirmApplicationId=com.steamforge.game
steamforge.appmetricaApiKey=...
steamforge.rewardedAdUnitId=...
steamforge.interstitialAdUnitId=...
steamforge.privacyPolicyUrl=https://...
```

`steamforge.confirmApplicationId` не является секретом; остальные значения всё равно рекомендуется хранить только локально. Keystore и generated release artifacts исключены из git.

### 4. Собрать и проверить production APK

Для реального релиза использовать guarded preflight-скрипт:

```bash
bash tools/build-rustore-release.sh
```

До сборки он проверяет:

- явное совпадение `steamforge.confirmApplicationId` с текущим `applicationId`;
- наличие keystore и всех signing-полей;
- что keystore не отслеживается git, если расположен внутри репозитория;
- наличие production AppMetrica/Yandex Ads параметров;
- отсутствие очевидных `demo`/`placeholder` значений;
- HTTPS Privacy Policy URL;
- что опубликованная Privacy Policy реально открывается и не содержит типовых placeholder-маркеров.

После этого preflight автоматически выполняет:

- unit tests;
- Android lint;
- signed release build;
- сверку `applicationId`, `versionCode` и `versionName` с generated release metadata;
- 16 KiB ZIP/ELF compatibility check;
- APK signature verification через `apksigner`;
- SHA-256 итогового APK.

Рабочий Gradle output остаётся здесь:

```text
app/build/outputs/apk/release/app-release.apk
```

Но файл, предназначенный для последнего smoke-test и загрузки, preflight копирует в отдельную локальную директорию `dist/`, например:

```text
dist/Steamforge-1.0-vc1-rustore.apk
dist/Steamforge-1.0-vc1-rustore.apk.sha256
```

`dist/` исключён из git. После успешного preflight не пересобирать APK между smoke-test и загрузкой в RuStore: использовать один и тот же файл из `dist/`.

Обычный `./gradlew assembleRelease` намеренно остаётся доступен без production secrets для CI и технической проверки release-конфигурации. **Не использовать такой CI APK для публикации.**

Перед первой публикацией отдельно сохранить SHA-256 отпечаток сертификата. Все последующие APK-версии Steamforge должны быть подписаны тем же ключом.

### 5. Финальный smoke-test

Устанавливать на устройство именно production APK из `dist/`, который будет отправлен в RuStore:

```bash
adb install -r dist/Steamforge-1.0-vc1-rustore.apk
```

Проверить минимум:

- первый запуск и privacy flow;
- новую и восстановленную партию;
- Game Over → «Заново» → второй Game Over;
- Daily Challenge / Daily Reward;
- rewarded;
- interstitial;
- AppMetrica после согласия;
- работу без сети;
- Settings → reset progress;
- отсутствие demo/test identifiers в production flow;
- открытие Privacy Policy по production URL.

После smoke-test повторно проверить SHA-256 файла и загружать именно его.

## AAB — если решим использовать позже

RuStore поддерживает AAB, но схема отличается от APK:

1. Создаётся/используется **app signing key**.
2. В RuStore загружается подпись приложения через PEPK согласно инструкции Консоли.
3. Создаётся отдельный **upload key**.
4. Из upload key экспортируется PEM-сертификат.
5. AAB подписывается upload key.
6. В RuStore загружаются необходимые signing materials и сам AAB.
7. RuStore генерирует APK и подписывает их app signing key.

Не путать app signing key и upload key. Не менять подпись после первого опубликованного релиза без отдельной миграционной процедуры.

Официальные страницы RuStore, которые нужно открыть непосредственно перед созданием production-подписи:

- «Публикация приложений»;
- «Работа с подписями APK и AAB»;
- «Как загрузить AAB-файлы для публикации».
