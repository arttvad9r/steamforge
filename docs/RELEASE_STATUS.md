# Steamforge 1.0 — release status

**Актуализировано:** 03.09.2026.

Этот файл фиксирует фактическое состояние первого релиза. Product roadmap и будущие системы находятся в `PRODUCT_PLAN.md`; они не должны смешиваться с V1 release gate.

## Consolidated baseline

`master` сейчас является основной V1-линией и содержит:

- launcher/header/gameplay fixes из актуальной production Compose-архитектуры;
- Android release hardening 2026;
- Android 17 / API 37 + 16 KiB runtime smoke workflow;
- устойчивый hosted-emulator boot/wait;
- game-state consistency fixes;
- `GameSaveCodec` **v4** с backward read `v3/v2/v1`;
- сохранение сессионных счётчиков статистики через process death;
- deterministic replayable RNG с сохранением seed/position;
- low-storage active-run autosave recovery: `IOException` не уничтожает in-memory run, а следующая успешная запись догоняет durable state;
- terminal Game Over persistence recovery с повтором того же `gameResultId` и идемпотентным `applyGameFinish`;
- regression coverage для pre-commit и ambiguous post-commit terminal I/O failure, включая process recreation;
- release AAB build/structural validation в Android CI;
- реальный API 36 Process Recreation Smoke: production UI → успешный swipe → `am force-stop` → launcher relaunch → точное совпадение semantic board signature;
- актуализированные product/visual/platform docs.

Полный branch decision log: `docs/BRANCH_AUDIT_2026-09-01.md`.

## Технически готово / существует

- Pure Kotlin 4×4 `GameEngine` покрыт unit tests.
- Normal run использует replayable deterministic RNG; seed/position сохраняются.
- Active run сохраняется в DataStore и восстанавливается после process death.
- При transient/low-storage `IOException` обычная autosave-запись не завершает и не откатывает текущую in-memory партию; recovery фиксируется на следующей успешной autosave.
- Terminal finish хранит один pending result и при retry использует тот же result ID; повтор durable transaction не должен повторно начислять progression/reward.
- После ambiguous terminal I/O, когда commit мог уже пройти, ViewModel восстанавливает persisted finish effects вместо повторного начисления.
- Save codec v4 сохраняет board/meta/RNG + session statistics и читает старые форматы.
- Rewarded x2 защищён от повторной выдачи по `gameResultId`.
- Daily reward защищён по `epochDay`.
- Android CI работает и для stacked pull requests, проверяет unit tests, `lintDebug`, `lintRelease`, debug/release APK build, release/privacy tooling, 16 KiB APK check, `bundleRelease` и структуру release AAB.
- Release AAB gate требует один непустой `.aab`, валидный ZIP, base manifest/resources и DEX payload.
- UI Emulator Smoke существует для основных экранов/compact behavior.
- Process Recreation Smoke на API 36 использует только production UI и OS-level `am force-stop`; точная semantic/bounds signature игровой доски должна сохраниться после relaunch.
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

## Active-run lifecycle status

Уже подтверждён и включён в `master` реальный process-recreation путь на API 36: активная normal run проходит production UI, получает реальный swipe, затем приложение принудительно останавливается через `am force-stop`; после launcher relaunch сохранённая доска должна восстановиться с теми же tile semantics и bounds.

Дополнительные Activity recreation / background-resume / screen-off-wake gates развиваются отдельно и не считаются частью готового baseline, пока их текущие V1 pull requests не пройдут собственные проверки и не будут слиты.

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

1. Убедиться, что `master` CI зелёный: unit tests, debug/release lint, debug/release APK build, 16 KiB check, `bundleRelease` и release AAB validation.
2. Проверить Android 17/16 KiB runtime smoke или явно зафиксировать infrastructure-only failure и повторить runtime check в контролируемой среде.
3. Проверить реальный active-run process recreation; дополнительные lifecycle gates учитывать только после их зелёного merge в `master`.
4. Заполнить/опубликовать Privacy Policy по постоянному HTTPS URL.
5. Подключить production keystore и сделать backups.
6. Добавить локально production AppMetrica/Yandex Ads IDs и Privacy Policy URL.
7. Запустить `bash tools/build-rustore-release.sh`.
8. Использовать только `dist/Steamforge-<version>-vc<code>-rustore.apk` и его `.sha256`.
9. Установить именно этот APK и пройти real-device smoke: consent, обычная партия, autosave/recovery, process-death restore, Game Over persistence/retry, Restart, Daily, rewarded, interstitial, reset progress, offline, Privacy Policy.
10. Проверить AppMetrica до/после consent и production ad placements.
11. Повторно сверить SHA-256 и загрузить проверенный APK + утверждённые store assets.
12. Для первого релиза использовать ручную публикацию после модерации.

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
- минимальный visual/game-feel polish, подтверждённый smoke.

Не расширять V1 новыми крупными meta/LiveOps systems до первого production release.
