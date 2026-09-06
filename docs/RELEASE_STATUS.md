# Steamforge 1.0 — release status

**Актуализировано:** 06.09.2026.

Этот файл фиксирует фактический V1 baseline после gameplay visual pass #156–158, tracking cleanup #159–160 и final artifact inventory gate #161.

## Product/privacy baseline

Steamforge является:

- **no-ads product** по `ADR_0001_NO_ADS.md`;
- **no-user-telemetry product** по `ADR_0005_NO_USER_TELEMETRY.md`.

Production Android client не должен содержать рекламный SDK, AppMetrica/другой user analytics SDK, ad unit IDs, analytics API keys, advertising/analytics consent UI, соответствующий Settings surface или runtime compatibility API для рекламы/behavioral analytics.

После #160 runtime analytics package, `AdsManager`, rewarded/interstitial wiring, ad-driven reward repository API, analytics consent state и новые analytics correlation identifiers физически отсутствуют из production source/runtime.

## Current technical baseline

В `master` / текущей release line уже находятся:

- pure Kotlin 4×4 `GameEngine`;
- deterministic replayable RNG;
- normal active-run autosave и process-death restore;
- backward-readable save codec; текущая запись — v6 без analytics/correlation ID, старые v5/v4/v3/v2/v1 читаются совместимо;
- сохранение session counters / stable run identity без telemetry identity;
- terminal result persistence с retry/idempotency/recovery;
- Steam Pressure / Overdrive;
- Undo и Wrench;
- Workshop progression;
- achievements;
- Daily Challenge / daily reward;
- Contracts и initial Blueprint Collection;
- forgiving return-loop pieces / Workshop Parts economy slices, внесённые в current master;
- offline-first Remote Config abstraction с compiled defaults, cache и bounded HTTPS refresh;
- deterministic Weekly challenge/run/replay/ranking foundations;
- pure JVM Weekly modules для client/server replay authority;
- server-side Weekly validation, PostgreSQL accepted population и bounded Ktor ranking transport;
- Weekly по-прежнему скрыт из обычной player navigation, пока production identity/session/deployment/client endpoint не завершены;
- Compose/Canvas gameplay с board-first visual hierarchy;
- gameplay premium material pass, reduced chrome и expanded portrait/tablet board scaling;
- Navigation 3;
- DataStore persistence;
- Macrobenchmark harness;
- release signing/preflight tooling;
- RuStore store-assets pipeline.

## Android baseline

- `compileSdk = 36`
- `targetSdk = 36`
- `minSdk = 24`
- JDK/Kotlin toolchain 17
- AGP 9.0.1
- Kotlin 2.3.20
- Compose + Material 3/custom Steamforge design system

## Advertising / analytics removal status

Cleanup #159 удалил из production configuration/runtime:

- Yandex Mobile Ads dependency;
- AppMetrica dependency/implementation;
- Yandex Ads manifest auto-init metadata;
- AppMetrica/ad BuildConfig credentials;
- startup analytics/ad consent dialog;
- Settings analytics/advertising controls;
- interstitial session policy;
- real advertising runtime code;
- consent/session AppMetrica wiring from `MainActivity`/`AppContainer`;
- release requirements for AppMetrica/ad credentials.

Cleanup #160 завершил source-level removal:

- удалён runtime analytics package и analytics event taxonomy;
- удалён `AdsManager`;
- удалены rewarded/interstitial gameplay wiring и rewarded-video UI;
- удалены `claimDoubleReward` / `rewardedClaimed`;
- удалён локальный `analyticsConsent` model/persistence state;
- `GameViewModel`, Workshop и Contracts больше не зависят от analytics/ad abstractions;
- save codec пишет v6 без analytics correlation ID, при этом legacy v5 ID безопасно игнорируется при чтении;
- старые неизвестные поля `FinishedGameRecord` остаются backward-readable через `ignoreUnknownKeys`.

CI включает `tools/check-no-tracking.sh`, который запрещает возвращать prohibited SDK/config/runtime/UI references.

Gate #161 добавляет **artifact-level** проверку финального release output:

- официальный Android `apkanalyzer` читает merged manifest, permissions, packaged files и defined DEX packages release APK;
- официальный pinned `bundletool` строит universal APK из release AAB, после чего тот проверяется тем же artifact analyzer;
- resolved `releaseRuntimeClasspath` проверяется на advertising/analytics SDK families;
- advertising identifier / AdServices permissions запрещены;
- production preflight сохраняет inventory report рядом с финальным APK в `dist/`.

`INTERNET` остаётся intentional для optional Remote Config и будущего explicit Weekly backend. Эти services не должны использоваться как behavioral analytics channel.

## Reliability / lifecycle status

Automated baseline covers:

1. Activity recreation;
2. Home/background/resume;
3. process force-stop/relaunch and durable run restoration;
4. screen-off/wake;
5. offline startup/continue/move/autosave/recreation;
6. low-storage autosave failure/recovery;
7. terminal finish retry/idempotency;
8. touchSlop / one-command-per-gesture;
9. high-tier tile readability;
10. expanded/portrait/compact-landscape gameplay geometry.

## CI gates

Canonical workflows include:

- Android CI — tests/lint/APK/AAB, 16 KiB checks и final release artifact inventory;
- UI Emulator Smoke;
- Android 17 / 16 KB Smoke;
- Active Run Lifecycle Smoke;
- High Tier Tile Smoke;
- Adaptive Gameplay Window Smoke;
- Accessibility UI Smoke;
- Frame Timing Diagnostic Smoke;
- RuStore Store Assets.

Tracking-free UI smoke дополнительно требует прямой Home startup и отсутствие AppMetrica/analytics-ad/consent/rewarded-video copy на проверяемых production surfaces.

## Visual status

Source of truth: `docs/VISUAL_BIBLE.md`.

Current direction:

> premium stylized industrial steampunk + painterly atmosphere + clean puzzle readability + restrained ornament.

Последние gameplay passes:

- #156 — premium tile/material system и calmer workshop/UI palette;
- #157 — reduced gameplay chrome/onboarding clutter;
- #158 — expanded portrait/tablet gameplay column/board scale.

Generated concept screens остаются art-direction references. Исторические Shop/Remove Ads/rewarded-video элементы не являются product spec и не должны возвращаться в production UI.

## Weekly status

Уже реализовано:

- deterministic challenge definition;
- shared replayable RNG;
- terminal replay recording/validation;
- shared wire protocol;
- server-side canonical replay validation;
- accepted-population store in PostgreSQL;
- bounded authenticated Ktor ranking route.

Ещё не production-ready:

- production identity/provider exchange;
- signed application session integration на актуальном master;
- deployment/secrets/runtime configuration;
- client ranking endpoint/provider;
- rate/abuse/operations/observability;
- privacy update до фактического Weekly player exposure.

До закрытия этих пунктов Weekly остаётся скрыт.

## Remaining production blockers

Перед первым store release необходимы:

1. зелёный canonical CI на финальном `master`;
2. успешный final signed APK/AAB dependency/manifest inventory gate и сохранённый inventory report для release candidate;
3. physical-device smoke normal/save/process-death/offline/settings/reset;
4. manual TalkBack/large-text/safe-area spot-check;
5. physical-device Macrobenchmark/frame-timing acceptance;
6. 30–60 minute thermal/battery session после final visual/VFX state;
7. production signing key + минимум две независимые backup-копии;
8. заполненная/опубликованная privacy/store disclosure по фактической production network configuration;
9. final signed APK из `tools/build-rustore-release.sh`, проверенный SHA-256 и загрузка именно этого artifact.

## Owner data required outside git

- final owner/legal name для store/privacy page;
- support/privacy contact e-mail;
- release keystore/passwords;
- backup copies of signing key;
- package ID confirmation;
- при включении Remote Config — production HTTPS endpoint и server-log/privacy details.

Не требуются:

- AppMetrica key;
- rewarded/interstitial ad IDs;
- advertising consent configuration.

## Architecture follow-up after V1 consolidation

Не является причиной переписывать core перед релизом, но до крупного LiveOps expansion рекомендуется:

- разгрузить `GameViewModel` от persistence/progression/competitive orchestration;
- переименовать/выделить общий `GameEngine` ownership из исторического `:weekly-core` в semantic game/simulation core module;
- определить storage boundary (Proto DataStore/Room) до роста event/reward/history state.

`GameEngine` и текущую reliability foundation радикально переписывать не требуется.
