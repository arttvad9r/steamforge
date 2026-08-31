# Steamforge 1.0 — release status

Актуализировано: 01.09.2026.

Этот файл фиксирует текущее состояние первого релиза и отделяет технически завершённые задачи от production-данных владельца.

## Технически готово

- `master` содержит release-hardening и финальную на текущем этапе steampunk UI-систему.
- Android CI проверяет unit tests, lint, debug/release build, release-tooling/privacy guards и 16 KiB compatibility.
- `UI Emulator Smoke` проходит основные экраны в обычном и compact layout.
- `RuStore Store Assets` создаёт три реальные вертикальные screenshot-ассета 1080×1920: Gameplay, Workshop, Achievements.
- Yandex Mobile Ads automatic SDK initialization отключён в manifest; SDK и AppMetrica инициализируются приложением в соответствии с privacy-решением пользователя.
- Подготовлены тексты карточки RuStore: `docs/RUSTORE_LISTING.md`.
- Подготовлен черновик Privacy Policy: `docs/PRIVACY_POLICY_DRAFT.md`.
- Подготовлена инструкция по signing: `docs/RELEASE_SIGNING.md`.
- Подготовлены release notes: `docs/RELEASE_NOTES_V1.md`.
- Для V1 выбран production-signed APK.
- Production preflight проверяет credentials, Privacy Policy URL, signing, 16 KiB compatibility, package/version metadata и SHA-256 итогового APK.
- Финальная художественная launcher icon утверждена и интегрирована в Android launcher/adaptive icon. Android CI run `33439826555` на commit `8db851b3bd78c0bf07b0f7aed39f3f1d4fab1901` завершился успешно; `generateLauncherIcon`, `assembleDebug` и `assembleRelease` прошли.
- Отдельный RuStore store asset подготовлен из того же утверждённого арта: `Steamforge-RuStore-512.png`, 512×512, без новой генерации.

## Package ID — решение подтверждено

Владелец подтвердил финальный `applicationId` для первого релиза:

```text
com.steamforge.game
```

До production build в локальный `~/.gradle/gradle.properties` нужно добавить явное подтверждение:

```properties
steamforge.confirmApplicationId=com.steamforge.game
```

После первой публикации package ID считается идентичностью приложения и не должен меняться.

## Перед production build нужны данные владельца

Обязательные:

- `steamforge.appmetricaApiKey`;
- `steamforge.rewardedAdUnitId`;
- `steamforge.interstitialAdUnitId`;
- публичный HTTPS URL Privacy Policy (`steamforge.privacyPolicyUrl`);
- имя/наименование владельца для Privacy Policy;
- support/privacy e-mail;
- release keystore + пароли и независимый backup ключа.

Секретные значения, keystore и пароли не должны коммититься в репозиторий.

## Финальный production gate

После получения production-данных:

1. Добавить локально `steamforge.confirmApplicationId=com.steamforge.game`.
2. Заполнить и опубликовать Privacy Policy по постоянному HTTPS URL.
3. Создать/подключить release keystore и сделать минимум две независимые резервные копии.
4. Добавить локально production AppMetrica/Yandex Ads IDs и Privacy Policy URL.
5. Запустить `bash tools/build-rustore-release.sh`.
6. Использовать только созданный preflight-файл `dist/Steamforge-<version>-vc<code>-rustore.apk` и его `.sha256`.
7. Установить именно этот production APK и пройти smoke: consent, партия, save/restore, Daily, rewarded, interstitial, reset progress, offline и Privacy Policy.
8. Проверить AppMetrica до/после consent и production rewarded/interstitial.
9. Повторно сверить SHA-256 и загрузить проверенный APK, финальную store-иконку и три store-скриншота в RuStore.
10. Для первого релиза использовать ручную публикацию после модерации.

## Не является blocker для V1

- backend;
- аккаунты и cloud sync;
- глобальный leaderboard;
- покупки/подписки;
- кросс-игровая валюта;
- multiplayer;
- выделение общего `game-kit`.

## Текущее решение

Не расширять scope Steamforge 1.0 новыми системами. До первого релиза допустимы только реальные bug fixes, release/privacy/signing работа, финальные store assets и минимальный polish, подтверждённый smoke/CI.
