# Steamforge 1.0 — release status

Актуализировано: 31.08.2026.

Этот файл фиксирует текущее состояние первого релиза и отделяет технически завершённые задачи от данных/решений, которые нужны непосредственно перед production build.

## Технически готово

- `master` содержит release-hardening и финальную на текущем этапе steampunk UI-систему.
- Android CI run `33416313501` прошёл успешно: unit tests, lint, debug/release build и 16 KiB compatibility check.
- `UI Emulator Smoke` run `33415341176` успешно запускает приложение на Android 36 и проходит обычный и compact layout основных экранов.
- Отдельный `RuStore Store Assets` run `33416313529` успешно создаёт placeholder-free набор из трёх реальных вертикальных screenshot-ассетов:
  - `01-game-1080x1920.png`;
  - `02-workshop-1080x1920.png`;
  - `03-achievements-1080x1920.png`.
- Store pipeline автоматически проверяет PNG-формат, количество файлов и точный размер `1080×1920` (9:16); итоговые кадры дополнительно проверены визуально.
- Settings сознательно не включён в магазинный набор до появления production Privacy Policy URL: текущий экран иначе показывает placeholder-текст.
- Подготовлены тексты карточки RuStore: `docs/RUSTORE_LISTING.md`.
- Подготовлен черновик Privacy Policy: `docs/PRIVACY_POLICY_DRAFT.md`.
- Подготовлена инструкция по signing первого релиза: `docs/RELEASE_SIGNING.md`.
- Подготовлены release notes: `docs/RELEASE_NOTES_V1.md`.
- Для первого релиза выбран production-signed APK; AAB не нужен для V1.
- Production preflight усилен: он проверяет production credentials, опубликованную Privacy Policy, signing, 16 KiB compatibility, generated package/version metadata и создаёт отдельный checksum-фиксированный artifact в локальном `dist/`.

## Решение, которое нельзя оставлять на момент первой публикации

Текущий `applicationId`:

```text
com.steamforge.game
```

Менять его автоматически нельзя: после первой публикации package ID становится идентичностью приложения. До production build нужно либо оставить его как финальный, либо переименовать до релиза. Чтобы исключить случайную публикацию, preflight требует локальное явное подтверждение:

```properties
steamforge.confirmApplicationId=com.steamforge.game
```

## Перед production build нужны данные владельца

Обязательные:

- `steamforge.confirmApplicationId` после финального решения по package ID;
- `steamforge.appmetricaApiKey`;
- `steamforge.rewardedAdUnitId`;
- `steamforge.interstitialAdUnitId`;
- публичный HTTPS URL Privacy Policy (`steamforge.privacyPolicyUrl`);
- имя/наименование владельца для Privacy Policy;
- support/privacy e-mail;
- release keystore + пароли и независимый backup ключа;
- финальная store/launcher-иконка 512×512.

Секретные значения и keystore не должны коммититься в репозиторий.

## Финальный production gate

После получения production-данных:

1. Принять окончательное решение по `applicationId` и записать `steamforge.confirmApplicationId`.
2. Заполнить и опубликовать Privacy Policy по постоянному HTTPS URL.
3. Создать/подключить release keystore и сделать независимые резервные копии.
4. Запустить `bash tools/build-rustore-release.sh` с production AppMetrica/Yandex Ads IDs.
5. Использовать только созданный preflight-файл `dist/Steamforge-<version>-vc<code>-rustore.apk` и его `.sha256`.
6. Установить именно этот production APK на реальное устройство и/или чистый emulator.
7. Пройти smoke: первый запуск/consent, обычная партия, сохранение/восстановление, Daily, rewarded, interstitial, reset progress, offline запуск и открытие Privacy Policy.
8. Проверить, что AppMetrica не отправляет события до разрешения и начинает работать после разрешения.
9. Проверить production rewarded/interstitial на естественных точках показа и отсутствие блокировки игры при недоступной сети.
10. Подготовить финальную store/launcher-иконку и проверить читаемость в маленьком размере.
11. Повторно сверить SHA-256 production APK и загрузить именно проверенный файл вместе с карточкой и тремя store-скриншотами; для первого релиза использовать ручную публикацию после модерации.

## Не является blocker для V1

- backend;
- аккаунты и cloud sync;
- глобальный leaderboard;
- покупки/подписки;
- кросс-игровая валюта;
- multiplayer;
- выделение общего `game-kit` — делать после подтверждения повторного использования во второй игре.

## Текущее решение

Не расширять scope Steamforge 1.0 новыми системами. До первого релиза допустимы только:

- исправления реальных багов;
- release/privacy/signing работа;
- финальные store assets;
- package-name/иконка перед первой публикацией;
- минимальные изменения, подтверждённые smoke/CI.
