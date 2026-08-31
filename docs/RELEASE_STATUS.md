# Steamforge 1.0 — release status

Актуализировано: 31.08.2026.

Этот файл фиксирует текущее состояние первого релиза и отделяет технически завершённые задачи от данных, которые должен предоставить владелец приложения.

## Технически готово

- `master` содержит release-hardening и новый steampunk UI.
- Android CI на кодовом состоянии `b70e478c7f6874ce7b2a4427b4825218862b0661` прошёл успешно: unit tests, lint, debug build и release build.
- Последующие коммиты до текущего `master` меняют только документацию и не меняют Android-код приложения.
- `UI Emulator Smoke` успешно запускает приложение на Android 36 и проходит основные экраны.
- Отдельный RuStore pipeline создаёт три реальные вертикальные screenshot-ассета:
  - `01-workshop-1080x1920.png`;
  - `02-game-1080x1920.png`;
  - `03-achievements-1080x1920.png`.
- Pipeline автоматически проверяет PNG-формат и точный размер каждого store-кадра: `1080×1920` (9:16).
- Подготовлены тексты карточки RuStore: `docs/RUSTORE_LISTING.md`.
- Подготовлен черновик Privacy Policy: `docs/PRIVACY_POLICY_DRAFT.md`.
- Подготовлена инструкция по signing первого релиза: `docs/RELEASE_SIGNING.md`.
- Подготовлены release notes: `docs/RELEASE_NOTES_V1.md`.
- Для первого релиза выбран простой путь: production-signed APK; AAB не является обязательным для V1.

## Перед production build нужны данные владельца

Обязательные:

- `steamforge.appmetricaApiKey`;
- `steamforge.rewardedAdUnitId`;
- `steamforge.interstitialAdUnitId`;
- публичный HTTPS URL Privacy Policy (`steamforge.privacyPolicyUrl`);
- имя/наименование владельца для Privacy Policy;
- support/privacy e-mail;
- release keystore + пароли и независимый backup ключа;
- финальная store-иконка 512×512.

Эти значения не должны коммититься в репозиторий.

## Финальный production gate

После получения production-данных:

1. Заполнить и опубликовать Privacy Policy.
2. Собрать подписанный release APK с production AppMetrica/Yandex Ads IDs.
3. Проверить подпись через `apksigner verify --print-certs`.
4. Установить именно production APK на реальное устройство и/или чистый emulator.
5. Пройти smoke: первый запуск/consent, обычная партия, сохранение/восстановление, Daily, rewarded, interstitial, reset progress, offline запуск.
6. Проверить, что AppMetrica не отправляет события до разрешения и начинает работать после разрешения.
7. Проверить production rewarded/interstitial на естественных точках показа и отсутствие блокировки игры при недоступной сети.
8. Финально просмотреть три RuStore-скриншота глазами и убедиться, что нет debug/demo-индикаторов, системных панелей Android, обрезанного текста и случайных переходных кадров.
9. Подготовить финальную store-иконку 512×512 и проверить читаемость в маленьком размере.
10. Загрузить APK, карточку и минимум три store-скриншота в RuStore; для первого релиза использовать ручную публикацию после модерации.

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
- минимальные изменения, подтверждённые smoke/CI.
