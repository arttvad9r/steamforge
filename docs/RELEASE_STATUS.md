# Steamforge 1.0 — release status

**Актуализировано:** 03.09.2026.

Этот файл фиксирует фактическое состояние первого релиза в `master`. Product roadmap и поздние feature-ветки находятся отдельно; наличие старого PR не означает, что функция входит в V1 baseline.

## Consolidated baseline

`master` является основной V1-линией. В него уже перенесены и зафиксированы отдельными понятными merge/squash-коммитами:

- production Compose baseline и release hardening;
- Android CI для обычных и stacked pull requests;
- Android 17 / API 37 + 16 KiB runtime smoke;
- release AAB build и structural validation;
- `GameSaveCodec` v4 с backward read старых форматов;
- deterministic replayable RNG и сохранение session counters;
- low-storage active-run autosave recovery;
- terminal Game Over persistence recovery с retry того же result ID и идемпотентным `applyGameFinish`;
- process recreation smoke через production UI и `am force-stop`;
- Activity recreation, Home/background-resume и screen-off/wake lifecycle gates;
- offline active-run lifecycle gate: production app стартует без outbound connectivity, восстанавливает durable run, принимает новый ход и сохраняет его через повторный offline process recreation;
- high-tier tile contrast regression и production `BoardView` capture;
- swipe/touchSlop instrumentation: sub-slop drag не делает ход, один жест отправляет не более одного move;
- semantic gameplay haptics и реальный Undo SFX только после успешной отмены;
- tiered merge feedback: low/mid/high SFX, restrained combo pitch и tier-dependent merge pop без изменения animation durations;
- release-like AndroidX Macrobenchmark harness для production `BoardView` + `GameEngine`;
- hosted API 36 frame-timing execution diagnostic; его числа являются диагностикой, а не performance SLA;
- adaptive production `GameScreen`: portrait baseline + compact-landscape layout с board слева и HUD/Undo/Wrench справа;
- отдельный adaptive-window smoke для 16:9 portrait, ~19.5:9 portrait и 16:9 landscape;
- large-font Accessibility UI Smoke: production Home/Game при font scale 1.3, critical touch targets не меньше 48dp и runtime bounds внутри display;
- curated V1 visual clean pass: спокойные Workshop/Achievements/Settings surfaces и выборочно адаптированный gameplay chrome без отката поздних contrast/input/feedback/adaptive fixes;
- Home как Navigation3 root с одним Play/Continue CTA и отдельными входами в Workshop, Daily, Contracts, Collection и Settings;
- ежедневные Contracts: детерминированный набор из трёх контрактов, high-water прогресс поверх autosave и атомарные идемпотентные gem claims.

Полный branch decision log: `docs/BRANCH_AUDIT_2026-09-01.md`.

## Технически готово / существует

- Pure Kotlin 4×4 `GameEngine` покрыт unit tests.
- Normal run использует replayable deterministic RNG; seed/position сохраняются.
- Active run сохраняется в DataStore и восстанавливается после process death.
- Transient/low-storage `IOException` обычной autosave не уничтожает текущую in-memory партию; следующая успешная autosave догоняет durable state.
- Terminal finish хранит один pending result; retry использует тот же result ID и не должен повторно начислять progression/reward.
- Ambiguous terminal I/O после фактического commit восстанавливает persisted finish effects вместо повторного начисления.
- Save codec v4 сохраняет board/meta/RNG + session statistics и читает старые форматы.
- Home является production root; незавершённая normal run возвращается через `ПРОДОЛЖИТЬ`.
- Contracts имеют отдельный экран и persistent daily ledger; progress записывается внутри существующих save/finish транзакций без второго DataStore write на каждый swipe.
- Rewarded x2 защищён от повторной выдачи по `gameResultId`.
- Daily reward защищён по `epochDay`.
- Android CI проверяет unit tests, `lintDebug`, `lintRelease`, debug/release APK, Macrobenchmark compile, 16 KiB APK, `bundleRelease` и структуру release AAB.
- Release AAB gate требует один непустой `.aab`, валидный ZIP, base manifest/resources и DEX payload.
- UI Emulator Smoke существует для основных production экранов.
- Active Run Lifecycle Smoke покрывает Activity recreation, background/resume, process recreation, screen-off/wake и offline process recreation с локальным autosave.
- High Tier Tile Smoke покрывает contrast/render и production swipe detector.
- Frame Timing Diagnostic Smoke исполняет release-like dense-merge workload на hosted emulator; physical-device `FrameTimingMetric` остаётся обязательным для performance acceptance.
- Adaptive Gameplay Window Smoke проверяет production gameplay bounds на трёх window shapes.
- Accessibility UI Smoke запускает production app на API 36 при font scale 1.3, проходит Privacy → Home → Game и проверяет runtime touch geometry критичных controls, включая Undo/Wrench, а также board tile bounds.
- Yandex Mobile Ads automatic initialization отключён в manifest; analytics/ads flow контролируется privacy decision.
- AppMetrica активируется только после положительного privacy-решения; crash reporting оставлен включённым по умолчанию SDK, ANR monitoring явно включён, а location/advertising-identifier tracking для аналитики отключён.
- Release signing/preflight tooling существует.
- `targetSdk = 36`, `compileSdk = 36`, `minSdk = 24`, JDK 17.
- Package ID: `com.steamforge.game`.

## Active-run lifecycle status

Lifecycle/recovery больше не является отдельным незавершённым стеком: соответствующие V1 gates перенесены в `master`.

Baseline проверяет:

1. `ActivityScenario.recreate()` с сохранением production Game route/state;
2. Home/background → launcher resume без потери active run;
3. `am force-stop` → launcher relaunch → Home `ПРОДОЛЖИТЬ` → восстановление durable run;
4. screen-off/wake через UI Automator;
5. low-storage autosave failure/recovery;
6. terminal finish retry/idempotency при I/O failure;
7. network-off startup (`airplane_mode_on=1`, outbound ping недоступен) → offline `ПРОДОЛЖИТЬ` → реальный swipe/autosave → повторный offline process recreation с точным восстановлением нового durable board state.

Interrupted rewarded/interstitial уже после показа остаётся отдельным manual real-device release gate; offline lifecycle smoke не считается его заменой.

При изменениях persistence/navigation/game UI эти workflows должны снова проходить на новом `master` head.

## Gameplay quality status

В `master` уже находятся минимальные Gate A улучшения, не меняющие правила 2048:

- semantic `CONFIRM/REJECT` haptic feedback;
- Undo SFX только для фактически выполненного Undo;
- merge SFX tiers 2–16 / 32–128 / 256+;
- restrained multi-merge pitch escalation;
- merge-pop hierarchy с неизменными slide/merge/spawn durations;
- 1024 contrast fix и regression test;
- touchSlop / one-command-per-gesture instrumentation;
- adaptive landscape gameplay layout;
- compact gameplay chrome с board-first hierarchy, единым спокойным HUD, restrained board frame и premium glow только на high tiers.

## Accessibility status

Автоматизированный accessibility baseline теперь включает:

- production Home/Game launch при font scale 1.3;
- проверку критичных Home controls и gameplay Undo/Wrench как реальных clickable targets не меньше 48dp;
- проверку, что эти runtime bounds остаются внутри display;
- проверку, что semantic gameplay tile остаётся видимым внутри display;
- сохранение screenshots, UI XML и geometry diagnostics как CI artifact.

Этот gate не заменяет ручной TalkBack smoke и real-device проверку дополнительных размеров/системных inset-вариантов перед production release.

## Performance status

Macrobenchmark harness находится в `master` и использует production `BoardView` + `GameEngine` на детерминированной dense-merge fixture.

Разделение доказательств:

- hosted emulator — проверяет, что benchmark build устанавливается, запускается и исполняет workload;
- physical Android 12+ device — нужен для реального `FrameTimingMetric` performance acceptance.

Hosted-emulator frame numbers не являются release SLA.

Physical command:

```bash
./gradlew :macrobenchmark:connectedBenchmarkAndroidTest
```

## Visual status

Source of truth: `docs/VISUAL_BIBLE.md`.

Принятое направление:

> premium stylized industrial steampunk + painterly atmosphere + clean puzzle readability + restrained ornament.

Generated screens остаются art-direction references, а не pixel-perfect production layouts.

Curated visual clean pass из исторического PR #9 теперь интегрирован в V1 baseline выборочно:

- Workshop, Achievements, Settings, typography и shared Steam UI уже соответствуют одобренному clean-pass направлению;
- gameplay chrome перенесён отдельным PR #66 поверх актуального reliability/accessibility baseline;
- palette не откатывалась к старому состоянию PR #9: более поздние high-tier contrast/accessibility fixes сохранены;
- исторический PR #9 закрыт как superseded и не должен raw-merge'иться поверх текущего `master`.

## Android 17 / 16 KiB status

Отдельный runtime smoke проверяет API 37 / Android 17 в 16 KiB environment.

Важно:

- failure загрузки hosted emulator — infrastructure failure, пока app не был реально установлен/запущен;
- app compatibility подтверждается только успешным boot + verified API/page size + install/launch.

Project-specific checklist: `docs/ANDROID_2026_CHECKLIST.md`.

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

Секреты и keystore в git не коммитятся.

## Production gate — RuStore V1

1. Получить зелёный canonical CI на актуальном `master`: Android CI, UI smoke, Android 17/16 KiB, lifecycle, high-tier/input, adaptive-window, accessibility UI и применимые performance diagnostics.
2. Выполнить physical-device Macrobenchmark и сохранить реальные frame-timing результаты.
3. Опубликовать Privacy Policy по постоянному HTTPS URL.
4. Подключить production keystore и проверить backups.
5. Добавить локально production AppMetrica/Yandex Ads IDs и Privacy Policy URL.
6. Запустить `bash tools/build-rustore-release.sh`.
7. Использовать только `dist/Steamforge-<version>-vc<code>-rustore.apk` и его `.sha256`.
8. Установить именно этот APK и пройти real-device smoke: consent, normal run, autosave/recovery, lifecycle/process-death restore, Game Over persistence/retry, Restart, Daily, rewarded, interstitial, reset progress, offline, Privacy Policy, TalkBack/large text spot-check.
9. Проверить AppMetrica analytics + crash/ANR reporting после consent и production ad placements.
10. Повторно сверить SHA-256 и загрузить проверенный APK + утверждённые store assets.
11. Для первого релиза использовать ручную публикацию после модерации.

## Не входят в текущий V1 baseline

Следующие старые stacked PR существуют в истории, но пока не считаются частью consolidated V1 `master`:

- Blueprint Collection;
- Weekly Challenge;
- forgiving streak extension;
- generic LiveOps framework;
- onboarding redesign;
- Remote Config;
- seasonal/event presentation;
- tile milestone reveals;
- Remove Ads / paid cosmetics;
- Reward Track / Season Pass readiness;
- rotating events и retention funnel extensions.

Их перенос должен быть отдельными читаемыми commits/PR поверх текущего `master`, а не raw merge старого cumulative integration branch.
