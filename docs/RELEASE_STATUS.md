# Steamforge 1.0 — release status

**Актуализировано:** 01.09.2026.

Этот файл фиксирует фактическое состояние первого релиза. Product roadmap и будущие системы находятся в `PRODUCT_PLAN.md`; они не должны смешиваться с V1 release gate.

## Consolidated baseline

Основная линия после текущей consolidation должна содержать:

- latest launcher/header/gameplay fixes из `master`;
- Android release hardening 2026;
- Android 17 / API 37 + 16 KiB runtime smoke workflow;
- более устойчивый emulator boot/wait для hosted CI;
- game-state consistency fixes;
- `GameSaveCodec` **v4** с backward read `v3/v2/v1`;
- сохранение сессионных счётчиков статистики через process death;
- regression tests для game-state/save consistency;
- актуализированные product/visual/platform docs.

Полный branch decision log: `docs/BRANCH_AUDIT_2026-09-01.md`.

## Технически готово / существует

- Pure Kotlin 4×4 `GameEngine` покрыт unit tests.
- Normal run использует replayable deterministic RNG; seed/position сохраняются.
- Active run сохраняется в DataStore и восстанавливается после process death.
- Save codec v4 сохраняет board/meta/RNG + session statistics и читает старые форматы.
- Rewarded x2 защищён от повторной выдачи по `gameResultId`.
- Daily reward защищён по `epochDay`.
- Android CI проверяет unit tests, lint, debug/release build и release/privacy tooling.
- UI Emulator Smoke существует для основных экранов/compact behavior.
- RuStore Store Assets создаёт реальные вертикальные screenshot assets.
- Yandex Mobile Ads automatic initialization отключён в manifest; analytics/ads flow контролируется privacy decision.
- Release signing/preflight tooling существует.
- `targetSdk = 36`, `compileSdk = 36`, `minSdk = 24`, JDK 17.
- Финальная launcher icon интегрирована.
- Package ID подтверждён: `com.steamforge.game`.

## Android 17 / 16 KiB status

Добавлена отдельная runtime smoke-проверка Android 17 / API 37 в 16 KiB environment.

Важное различие:

- отсутствие emulator/device в `adb` или падение hosted emulator boot — **CI infrastructure failure**, а не автоматически app failure;
- app/runtime compatibility считается подтверждённой только когда emulator действительно загрузился, verified page size/API level и приложение установилось/запустилось.

До production Google Play release этот workflow должен иметь стабильный зелёный baseline либо проверка должна быть повторена на контролируемом emulator/real-device environment.

Project-specific platform checklist: `docs/ANDROID_2026_CHECKLIST.md`.

## Visual status

Основной visual concept принят:

> premium stylized industrial steampunk + painterly atmosphere + clean puzzle readability + restrained ornament.

`docs/VISUAL_BIBLE.md` является source of truth. Последние generated screens — art-direction references, не pixel-perfect production layouts.

Для release V1 не требуется сейчас переписывать все экраны под новый concept. Перед следующими visual changes приоритет: gameplay readability и минимальный risk polish.

## Package ID

```text
com.steamforge.game
```

До production build в локальный `~/.gradle/gradle.properties` добавить подтверждение, требуемое release tooling:

```properties
steamforge.confirmApplicationId=com.steamforge.game
```

После первой публикации package ID не менять.

## До production build нужны данные владельца

Обязательные значения вне git:

- `steamforge.appmetricaApiKey`;
- `steamforge.rewardedAdUnitId`;
- `steamforge.interstitialAdUnitId`;
- публичный HTTPS Privacy Policy URL (`steamforge.privacyPolicyUrl`);
- owner/legal name для Privacy Policy;
- support/privacy e-mail;
- release keystore + passwords;
- минимум две независимые backup-копии release key.

Секретные значения, keystore и passwords не коммитятся.

## Production gate — RuStore V1

1. Убедиться, что consolidation/master CI зелёный: unit/lint/build.
2. Проверить Android 17/16 KiB runtime smoke или явно зафиксировать infrastructure-only failure и повторить runtime check в контролируемой среде.
3. Заполнить/опубликовать Privacy Policy по постоянному HTTPS URL.
4. Подключить production keystore и сделать backups.
5. Добавить локально production AppMetrica/Yandex Ads IDs и Privacy Policy URL.
6. Запустить `bash tools/build-rustore-release.sh`.
7. Использовать только `dist/Steamforge-<version>-vc<code>-rustore.apk` и его `.sha256`.
8. Установить именно этот APK и пройти real-device smoke: consent, обычная партия, process-death restore, Game Over/Restart, Daily, rewarded, interstitial, reset progress, offline, Privacy Policy.
9. Проверить AppMetrica до/после consent и production ad placements.
10. Повторно сверить SHA-256 и загрузить проверенный APK + утверждённые store assets.
11. Для первого релиза использовать ручную публикацию после модерации.

## Не blocker для Steamforge 1.0

- backend;
- accounts/cloud sync;
- global leaderboard;
- billing/IAP/subscription;
- Blueprint Collection;
- generic Contracts system;
- Remote Config;
- LiveOps/Season Pass;
- multiplayer/social layer.

Эти направления остаются post-V1 roadmap, а не причиной задерживать текущую стабилизацию.

## Scope rule до первого релиза

До V1 допустимы:

- real bug fixes;
- state/reliability fixes;
- privacy/signing/release fixes;
- CI/runtime compatibility fixes;
- final store assets;
- минимальный visual polish, подтверждённый smoke.

Не расширять V1 новыми крупными meta/LiveOps systems до первого production release.
