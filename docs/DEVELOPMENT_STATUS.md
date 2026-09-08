# Steamforge — Development Status

**Purpose:** factual development baseline. This document intentionally excludes app-store publication planning.

## Product constraints

- No advertising.
- No advertising SDKs.
- No AppMetrica/user behavioral analytics.
- No analytics/ad consent UI.
- No ad-driven rewards.
- Store publication work is deferred until an explicit future decision after the game and visuals reach the target quality.

## Current gameplay foundation

Implemented:

- pure Kotlin 4×4 `GameEngine`;
- deterministic replayable RNG;
- autosave/process-death restore;
- backward-readable save codec, current write format v6;
- Steam Pressure / Overdrive;
- Undo and Wrench;
- movement/merge feedback, SFX and haptics;
- Daily Challenge / daily rewards;
- Contracts;
- Workshop progression;
- Blueprint Collection foundation;
- authoritative `RewardSystem` for positive gameplay/meta rewards;
- achievements;
- offline-safe core behavior.

The core engine is not the current rewrite target.

## Current visual state

Source of truth: `VISUAL_BIBLE.md`.

Merged gameplay passes:

- #156 — premium tile/material system and calmer background/panel treatment;
- #157 — reduced gameplay chrome and clutter;
- #158 — larger gameplay column/board on expanded portrait/tablet layouts.

Still unfinished:

- final movement/merge/spawn visual sequencing;
- high-tier tile polish/readability across long play;
- full typography/component consistency;
- Home visual pass;
- Workshop presentation with visible restoration/world change;
- Blueprints/Contracts/Profile/Settings alignment to one visual system;
- final balance of atmosphere vs gameplay cleanliness.

## Current technical foundation

- `compileSdk = 36`;
- `targetSdk = 36`;
- `minSdk = 24`;
- JDK/Kotlin toolchain 17;
- AGP 9.0.1;
- Kotlin 2.3.20;
- Compose + custom Material 3 design system;
- Navigation 3;
- Preferences DataStore;
- Macrobenchmark harness;
- Android 17 / 16 KiB checks;
- lifecycle/accessibility/adaptive/high-tier/frame-timing CI;
- no-tracking CI guard.

## Advertising / analytics removal

Completed in #159–160:

- Yandex Mobile Ads dependency removed;
- AppMetrica dependency/implementation removed;
- advertising manifest metadata removed;
- ad/analytics BuildConfig credentials removed;
- startup consent surface removed;
- Settings analytics/ad controls removed;
- `AdsManager` removed;
- runtime analytics package/event taxonomy removed;
- rewarded/interstitial gameplay wiring removed;
- ad-driven reward repository API removed;
- local analytics consent/correlation state removed;
- save v6 no longer writes analytics correlation identifiers.

`tools/check-no-tracking.sh` prevents these surfaces from returning unnoticed.

## Reliability status

Automated coverage includes:

- Activity recreation;
- Home/background/resume;
- process force-stop/relaunch;
- screen-off/wake;
- offline startup/gameplay/autosave/recreation;
- low-storage save failure/recovery;
- terminal finish retry/idempotency;
- gesture touchSlop / one-command-per-gesture;
- high-tier tile checks;
- expanded/portrait/compact-landscape geometry.

These checks should remain green through gameplay and visual refactors.

## Weekly/backend status

Foundation exists:

- deterministic challenge/replay model;
- shared replayable RNG;
- server-side replay validation;
- shared protocol;
- PostgreSQL accepted population;
- bounded Ktor ranking transport.

Weekly remains hidden from normal navigation. Further identity/deployment/client exposure work is deferred while the base game and visual/meta experience are the priority.

## Current architecture debt

Known but not immediately dominant:

- `GameViewModel` owns too much session/persistence/progression orchestration;
- shared `GameEngine` lives historically under `:weekly-core`;
- Preferences DataStore may become insufficient if reward/history/event data grows substantially.

Do not perform a broad architecture rewrite before it is needed by current gameplay/visual work.

## Active priority order

1. Core interaction feel and movement/merge feedback.
2. Gameplay visual quality to the Visual Bible target.
3. Whole-app visual system consistency.
4. Workshop visible restoration/meta presentation.
5. Targeted architecture cleanup where it reduces friction for the above work.
6. Consolidate remaining reward call sites on the existing `RewardSystem`, then evolve richer meta systems.
7. Weekly/LiveOps/social only later.

## Explicitly deferred

- any app-store publication;
- store listing/assets;
- signing/upload/moderation pipelines;
- publication privacy/legal drafts;
- release-candidate/store checklists;
- advertising;
- behavioral analytics;
- Season Pass / broad LiveOps expansion;
- social/PvP infrastructure.
