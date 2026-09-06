# Steamforge — Product & Development Plan

> **Status:** canonical product roadmap, updated 06.09.2026 against the tracking-free repository state.
>
> **Product decisions:** Steamforge is a **no-ads product** (`ADR_0001_NO_ADS.md`) and a **no-user-telemetry client** (`ADR_0005_NO_USER_TELEMETRY.md`). Do not add/restore advertising, advertising SDKs, AppMetrica, behavioral analytics SDKs, analytics consent UI or tracking credentials unless a later accepted ADR explicitly supersedes the relevant decision.

## 1. Target product

Steamforge should become:

> **a premium-feeling steampunk 2048/merge core surrounded by a living workshop, short contracts, collections and regular goals.**

Primary loop:

```text
2048 core
→ satisfying mechanical merges
→ run progress / rewards
→ contracts and milestones
→ workshop restoration
→ blueprint collection
→ daily / weekly reasons to return
→ back to the same core
```

The meta exists to create reasons to play the core, not to replace it.

## 2. Current repository state — 06.09.2026

Already implemented/current:

- pure Kotlin 4×4 `GameEngine`;
- replayable deterministic RNG;
- autosave/process-death restore and backward-readable save format;
- session statistics/state preservation across recreation;
- swipe + keyboard gameplay input;
- movement/merge feedback, SFX and haptics;
- Steam Pressure / Overdrive;
- Undo and Wrench;
- Workshop progression;
- achievements;
- Daily Challenge / daily reward;
- Contracts and initial Blueprint Collection;
- Workshop Parts economy/return-loop slices already merged into current master;
- offline-first Remote Config with compiled defaults, persistent cache and bounded HTTPS refresh;
- deterministic Weekly challenge/run/replay/ranking client foundations;
- pure JVM replay/ranking protocol/server modules;
- PostgreSQL accepted-population implementation and bounded Ktor ranking transport;
- release signing/preflight tooling;
- Android CI/emulator/lifecycle/accessibility/adaptive/16 KiB/performance diagnostics;
- gameplay visual pass aligned with the approved Visual Bible (#156–158);
- no advertising SDK/runtime;
- no AppMetrica/user analytics SDK/runtime;
- no analytics/ad consent or Settings surface.

Weekly is not exposed in normal player navigation until production identity/backend/deployment/client endpoint are complete.

## 3. Systems still missing from target architecture

- one universal reward application layer;
- richer Workshop restoration with multiple machines/zones;
- production Weekly identity/session/deployment/client ranking service;
- reusable LiveOps `EventSystem` if later justified;
- reusable `RewardTrack` if later justified;
- optional non-ad cosmetic/store monetization only after a separate decision;
- seasonal collections / Season Pass only after strong product justification;
- optional lightweight social layer.

## 4. Product principles

### Core first

If Workshop, Daily and collections disappear, the 2048 game should still feel good.

### Clean gameplay

Gameplay prioritizes:

1. board;
2. tiles/numbers;
3. score;
4. one current goal;
5. secondary controls.

### Premium through material, not clutter

Use light, bevel, material response and restrained animation. Decorative steampunk elements must not compete with the puzzle.

### Permanent trace

A useful session should leave visible long-term progress where possible, preferably in Workshop/Blueprint systems rather than only abstract counters.

### Few currencies

Do not add currency without a clear source, sink and player purpose.

### Offline-safe core

Normal gameplay/startup must remain usable without network access. Remote services must have bounded protocols and local-safe fallbacks where appropriate.

### No advertising

Advertising is prohibited by ADR 0001. Historical disabled ad code/assets/PRs are not backlog items.

### No user telemetry by default

The shipped client does not collect/send behavioral analytics. Internal typed events may exist temporarily as no-op architecture seams, but they are not a data-collection product feature.

If a future product decision genuinely requires telemetry, it needs a new ADR defining exact data, purpose, retention, identifiers and legal/store implications before implementation.

## 5. Technical architecture target

```text
Game Core
  ↓ typed state/events
Game Session / Feedback / Goals
  ↓
Reward Application
  ↓
Workshop / Economy / Collections
  ↓
Persistent Player Progress
```

Platform services stay separate:

- optional Remote Config;
- optional billing for future non-ad purchases;
- optional cloud save;
- Weekly ranking/backend.

Gameplay logic must not depend directly on store/network SDKs. Advertising and behavioral analytics SDKs are not part of the target architecture under current ADRs.

## 6. Sequential implementation plan

### Phase 0 — Consolidate V1

- keep `master` green;
- ensure release docs match code;
- keep Android 17 / 16 KiB and lifecycle gates green;
- physically remove advertising/user-telemetry SDK/runtime/configuration;
- protect tracking-free invariants in CI;
- finish physical-device release acceptance.

**Done when:** CI is green on current master, final APK inventory is tracking-free, and physical-device release gates are recorded.

### Phase 1 — Core quality gate

Improve only measured weaknesses in:

- input responsiveness;
- animation sequencing;
- tile readability;
- merge feedback hierarchy;
- SFX/haptic balance;
- game-over/restart flow;
- save/restore reliability.

Do not add a new engine layer for architecture fashion.

### Phase 2 — Visual Bible application

Current gameplay pass is substantially implemented. Continue incrementally:

1. board-first gameplay hierarchy;
2. restrained HUD/frame;
3. readable material tile progression;
4. shared typography/colors/components;
5. Workshop/Blueprint polish only after gameplay remains clear.

Generated concepts are references, not pixel-perfect specs. Historical Shop/Remove Ads/video elements must not return.

### Phase 3 — Session orchestration cleanup

Before large new meta systems, reduce `GameViewModel` responsibility without a big-bang rewrite.

Target boundaries:

```text
GameSessionController
├─ GameEngine
├─ RunPersistence
├─ Progression/Reward events
└─ CompetitiveRunRecorder

GameViewModel
└─ UI state + intents
```

Keep manual DI unless actual complexity justifies a DI framework.

### Phase 4 — Universal Reward layer

Create one authoritative reward application path for current/future systems.

Initial reward types:

- current soft resources;
- Workshop Parts;
- Blueprint Pieces;
- Cosmetic Unlocks.

```text
source
→ RewardSystem
→ validate/apply
→ persist
→ presentation
```

### Phase 5 — Workshop meta v2

Evolve Workshop from mostly numeric progression toward visible restoration:

- one zone first;
- 3–5 machines;
- several visible restoration stages;
- one clear cost path;
- machine completion gives a meaningful unlock/reward.

### Phase 6 — Contracts evolution

Keep contracts data-driven and fed by gameplay events. Avoid adding contract-specific branches inside `GameEngine`.

Target definitions include:

- reach tile;
- merge count;
- score / cumulative score;
- combo/multi-merge;
- runs played;
- moves survived.

### Phase 7 — Blueprint Collections

Build on the existing Steam Engine collection so completion visibly affects Workshop/world presentation.

### Phase 8 — Storage boundary review

Preferences DataStore remains acceptable for V1, but before event/history/reward ledgers grow substantially, choose the smallest appropriate standard solution:

- Proto DataStore for one typed profile/state model; or
- Room when relational records/history/querying/migrations become real requirements.

Do not invent a custom database abstraction without need.

### Phase 9 — Remote Config

Current provider/cache/bounded HTTPS foundation exists.

Configurable meta values may include:

- contract definitions/rewards;
- Workshop costs;
- reward multipliers;
- feature flags;
- event schedule/milestones;
- future non-ad store offer enablement.

Do not remotely mutate core board/spawn/RNG/save semantics that would break deterministic replay.

Remote Config must not become a behavioral telemetry channel.

### Phase 10 — Return loop

Use forgiving, modest return mechanics:

- Daily Contracts;
- small Workshop bonus;
- forgiving short streak;
- comeback presentation after absence.

Avoid punitive long streak loss.

### Phase 11 — Weekly production completion

Already implemented foundation:

- shared challenge definition;
- deterministic seed/rules;
- terminal replay validation;
- ranking wire/domain/runtime;
- server replay validation;
- PostgreSQL accepted population;
- Ktor transport.

Remaining before player exposure:

- production identity/auth flow;
- signed session integration on current master;
- deployment/secrets;
- Android client provider/endpoint;
- rate/abuse controls;
- operational observability that is server-service oriented rather than client behavioral analytics;
- privacy/store disclosure update for the actual backend data flow.

### Phase 12 — LiveOps framework only if content cadence exists

One reusable event definition should configure schedule, scoring, milestones, rewards, theme and optional collection.

The second event should mostly be data/assets, not a new architecture.

### Phase 13 — Optional non-ad monetization

Advertising remains prohibited.

If later justified by product/business needs, explore only non-ad direct purchases such as:

- tile cosmetics;
- Workshop themes;
- small cosmetic bundles.

Do not add energy/lives to gate the core.

### Phase 14 — Reward Track / Season Pass only after justification

Do not build a Season Pass simply because old branches contain one. First prove:

- stable content cadence;
- understandable economy;
- healthy player return behavior through non-invasive evidence/feedback;
- reusable event/reward infrastructure.

Any reintroduction of client telemetry for quantitative retention metrics requires explicitly superseding ADR 0005 first.

### Phase 15 — Social only if justified

Prefer lightweight asynchronous social:

- friend leaderboard;
- weekly rank/percentile;
- score sharing;
- partner challenge.

Avoid real-time PvP/guild-war scope initially.

## 7. Explicitly out of scope

Under current decisions:

- rewarded/interstitial/banner/native advertising;
- advertising SDKs;
- ad-driven rewards;
- Remove Ads;
- AppMetrica/user behavioral analytics SDKs;
- analytics consent/settings UI;
- energy/lives gating;
- gacha rarity economy;
- many new currencies;
- real-time PvP;
- guild wars;
- subscriptions without continuous value;
- multiple parallel passes;
- unrelated minigames.

## 8. Quality gates

### Core

- deterministic rules tested;
- save/restore correct;
- responsive input;
- readable tiles;
- stable animation;
- no known state duplication/loss.

### Privacy/runtime

- no advertising SDK/config/UI;
- no AppMetrica/user analytics SDK/config/UI;
- CI guard green;
- final APK/AAB inventory checked;
- network services explicit and bounded.

### Meta

- one reward path;
- Workshop progress understandable;
- Contracts consume gameplay events rather than fork core rules;
- economy sources/sinks remain inspectable through local/domain logic even without behavioral analytics.

### Release

- canonical CI green;
- Android 17 / 16 KiB green;
- physical-device lifecycle/performance/thermal/TalkBack checks complete;
- signing/key backups verified;
- privacy/store text matches actual production network/data behavior.

## 9. Final product formula

```text
PREMIUM 2048 CORE
+
MECHANICAL GAME FEEL
+
VISIBLE WORKSHOP PROGRESSION
+
CONTRACTS
+
BLUEPRINT COLLECTIONS
+
WEEKLY CHALLENGES
+
OPTIONAL REUSABLE LIVEOPS
+
OPTIONAL NON-AD COSMETICS
+
PRIVACY-MINIMAL CLIENT
```

Rule for every new feature:

1. What player/product problem does it solve?
2. How does it send the player back to the core?
3. Can it be implemented with a proven/simple standard approach?
4. Does it preserve offline/replay/reliability invariants?
5. Does it add new data collection or network behavior, and if so is that explicitly justified/documented?
6. Are we willing to remove it if it does not improve the product?
