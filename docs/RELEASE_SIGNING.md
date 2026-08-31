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

### 1. Создать ключ подписи

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

## 2. Локальная конфигурация

Создать в корне проекта `keystore.properties` — файл уже исключён из git:

```properties
storeFile=/absolute/path/to/steamforge-release.jks
storePassword=...
keyAlias=steamforge
keyPassword=...
```

Production-параметры приложения задавать через `~/.gradle/gradle.properties` или `-P` параметры, а не коммитить в репозиторий:

```properties
steamforge.appmetricaApiKey=...
steamforge.rewardedAdUnitId=...
steamforge.interstitialAdUnitId=...
steamforge.privacyPolicyUrl=https://...
```

## 3. Собрать APK

```bash
./gradlew clean testDebugUnitTest lintDebug assembleRelease
```

Ожидаемый файл:

```text
app/build/outputs/apk/release/app-release.apk
```

## 4. Проверить подпись

```bash
apksigner verify --verbose --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

Перед первой публикацией отдельно сохранить SHA-256 отпечаток сертификата. Все последующие APK-версии Steamforge должны быть подписаны тем же ключом.

## 5. Финальный smoke-test

Устанавливать на устройство именно production APK, который будет отправлен в RuStore:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
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
- отсутствие demo/test identifiers в production flow.

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
