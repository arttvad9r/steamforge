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
- cold-start SFX load gating so early gameplay feedback is not silently lost (#177);
- turn-sequenced visual input with one latest buffered direction while animations are active (#179);
- Undo/Wrench lockout while a visual turn is settling (#179);
- merge/Overdrive/win feedback aligned with the visible turn rather than raw state mutation (#179);
- terminal-result persistence presentation that avoids flashing a save modal for short writes while retaining retry/failure handling (#181);
- Daily Challenge / daily rewards;
- Contracts;
- Workshop progression;
- Blueprint Collection foundation and a dedicated Blueprints catalog route;
- authoritative `RewardSystem` for positive gameplay/meta rewards;
- achievements;
- offline-safe core behavior.

The core engine is not the current rewrite target.

## Current visual state

Source of truth: `VISUAL_BIBLE.md`.

Merged gameplay passes:

- #156 — premium tile/material system and calmer background/panel treatment;
- #157 — reduced gameplay chrome and clutter;
- #158 — larger gameplay column/board on expanded portrait/tablet layouts;
- #179 — movement/merge/spawn turn sequencing and feedback timing baseline;
- #181 — calmer terminal-save presentation over the final board state;
- #200 — machined tile bevel lighting refined without changing the established palette; high-tier contrast coverage now includes 4096/8192 and High Tier screenshot capture rejects visually blank artifacts.

Merged app/meta passes:

- #172 — Contracts as an industrial task list;
- #173 — Daily objective strip in gameplay;
- #174 — unlocked Home navigation deck;
- #175 — Profile permanent-stat ledger;
- #176 — Achievements collection registry;
- #182 — visible Workshop restoration/world change across mechanism stages;
- #183 — dedicated Blueprints catalog for the persisted Steam Engine collection;
- #184 — Home respects the animation setting and stays static when animations are disabled.
- #188 — Settings has dedicated production visual-regression coverage, including blank-capture rejection.
- #193 — Meta Visual screenshots use validated Activity-decor capture after Compose redraw/system-framebuffer capture proved unreliable; exported production PNGs were manually checked.
- #199 — permanent Profile is reachable from the unlocked Home status rail without adding extra Home chrome; emulator coverage now traverses normal production navigation across Blueprints, Contracts, Profile, Achievements, Workshop and Settings.

Still unfinished / requires acceptance:

- physical-device swipe confidence and input-latency tuning;
- subjective SFX/haptic balance during real play;
- long-session high-tier tile readability beyond static smoke captures;
- final whole-app atmosphere vs gameplay-cleanliness acceptance;
- Blueprint catalog breadth is intentionally still small: one Steam Engine collection. Expand only when new collections have a clear visible Workshop/world payoff.

Physical-device acceptance is tracked in #185. Automated green checks are necessary but do not close those tactile/readability items by themselves.

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
- Daily Challenge claim retry/idempotency across transient and ambiguous I/O failures (#190);
- Settings, Contracts and Workshop visible meta writes preserve the last durable state instead of crashing on DataStore-style I/O failures (#192, #194);
- obsolete Contracts-specific save/finish repository API variants removed after normal transactions absorbed that accounting (#189);
- obsolete direct contract-claim repository API and its unused `GameSummary` conversion helper removed after compile/test validation (#195);
- gesture touchSlop / one-command-per-gesture;
- high-tier tile checks through 8192, including visual-content validation of the screenshot artifact (#200);
- normal production-navigation traversal across the currently exposed permanent meta routes (#199);
- expanded/portrait/compact-landscape geometry;
- hosted frame-timing diagnostics.

These checks should remain green through gameplay and visual refactors. Physical-device lifecycle/accessibility spot checks remain part of #185.

## Reward application status

Current positive-grant paths use the existing `RewardSystem`:

- game-finish Workshop Parts and level/achievement Gems;
- Daily Challenge Gems and achievement Gems;
- Contracts Workshop Parts / Blueprint Pieces;
- Daily Reward Gems / Workshop Parts / cosmetic unlock.

Spending paths such as Undo, Wrench and Workshop upgrades are not positive rewards and intentionally remain outside `RewardSystem`.

Do not create a second reward-application layer.

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

- `GameViewModel` still owns broad session/persistence/progression orchestration;
- shared `GameEngine` lives historically under `:weekly-core`;
- Preferences DataStore may become insufficient if reward/history/event data grows substantially.

Use small behavior-preserving extractions when they remove duplication or reduce current friction. Do not perform a broad architecture rewrite before it is needed.

## Active priority order

1. Close physical-device core-feel/readability/audio/accessibility acceptance in #185.
2. Complete final whole-app consistency acceptance.
3. Continue targeted `GameViewModel`/session orchestration cleanup only where it reduces concrete duplication or risk.
4. Evolve Blueprint collections only when tied to visible Workshop/world changes.
5. Preserve the existing `RewardSystem` as the single positive-reward path.
6. Weekly/LiveOps/social only later.

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
