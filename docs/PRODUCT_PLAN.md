# Steamforge — Product & Development Plan

> **Status:** canonical product roadmap, updated 05.09.2026 against the repository state.
>
> **Product decision:** Steamforge is a **no-ads product**. In-game advertising and advertising development are frozen. Do not add, restore or expand rewarded/interstitial/banner/native ads, ad-driven rewards, ad-specific monetization analytics, ad SDKs or a Remove Ads purchase unless a later accepted ADR explicitly supersedes [`ADR_0001_NO_ADS.md`](ADR_0001_NO_ADS.md).
>
> The roadmap describes the target product. Already shipped V1 systems are not removed merely because a cleaner future architecture places them later in the sequence. Every new layer must preserve or improve the 2048 core.

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

The meta exists to create new reasons to play the core, not to replace it.

## 2. Current repository state — 05.09.2026

Already implemented in V1 / current master:

- pure Kotlin 4×4 `GameEngine`;
- replayable deterministic RNG for normal runs;
- autosave after meaningful state changes and process-death restore;
- backward-compatible save migration and stable run analytics IDs;
- preservation of session statistics across process death;
- swipe and keyboard gameplay input;
- movement/merge animations, SFX and haptics;
- Steam Pressure / Overdrive;
- Undo and Wrench mechanics;
- Workshop progression;
- achievements;
- Daily Challenge / daily reward flow;
- gems/economy used by current V1 mechanics;
- AppMetrica integration behind privacy/consent handling;
- legacy Yandex rewarded/interstitial infrastructure present from V1 but product use is disabled/frozen by ADR 0001 and must not be extended;
- release signing/preflight tooling;
- Android CI, UI emulator smoke and RuStore store-asset generation;
- Android 17 / 16 KiB hardening workflow;
- first-run onboarding with progressive disclosure;
- data-driven Contracts and initial Blueprint Collection;
- deterministic core balance simulation baseline for measured difficulty/spawn tuning.

This means Steamforge is **not a blank prototype**. The next steps are consolidation and evolution of existing systems into a clearer long-term architecture.

## 3. Systems not yet implemented in the target architecture

The long-term product still lacks:

- a single universal `RewardSystem`;
- richer Workshop restoration with multiple machines/zones;
- Remote Config abstraction;
- deterministic Weekly Challenge shared across players;
- backend-validated leaderboard;
- reusable LiveOps `EventSystem`;
- reusable `RewardTrack`;
- optional non-ad cosmetic/store monetization, only if later justified;
- seasonal collections and, only if justified, Season Pass;
- optional social/friend layer.

## 4. Product principles

### Core first

If Workshop, Daily and achievements disappear, the 2048 game should still feel good.

### Clean gameplay

Gameplay screen prioritizes:

1. board;
2. tiles/numbers;
3. score;
4. one current goal;
5. secondary controls.

### Permanent trace

A useful session should leave visible long-term progress, preferably in the Workshop/Blueprint systems rather than only increasing an abstract counter.

### Few currencies

Do not add a new currency without a clear source, sink and player purpose. Existing V1 gems stay because they are already part of live mechanics, but future systems should not automatically create more currencies.

### One reusable framework before many events

One good configurable event framework is preferable to several unrelated minigames.

### No advertising

Advertising is not a dormant roadmap item waiting to be resumed. It is explicitly out of scope under ADR 0001. Existing legacy ad code must not be used as justification to restart ad product work.

## 5. Technical architecture target

```text
Game Core
  ↓ typed state/events
Feedback / Quests / Events / Analytics
  ↓
Rewards
  ↓
Workshop / Economy / Collections
  ↓
Persistent Player Progress
```

Platform services remain separate:

- analytics;
- billing, only if non-ad purchases are later implemented;
- remote config;
- cloud save;
- leaderboard/backend.

Gameplay logic must not depend directly on a store or analytics SDK. Advertising SDKs are not part of the target architecture while ADR 0001 is active.

## 6. Sequential implementation plan

### Phase 0 — Stabilize the existing V1 baseline

- consolidate active useful branches;
- keep master green;
- finish Android 17 / 16 KiB smoke reliability;
- ensure release docs match actual code;
- keep process-death/save regression tests green;
- do not add unrelated product scope during release hardening.

**Done when:** unit/lint/build CI is green, emulator/runtime failures are triaged, and `master` is the single source of truth.

### Phase 1 — Core quality gate

Keep improving only measured weaknesses in:

- move responsiveness;
- animation sequencing;
- tile readability;
- merge feedback hierarchy;
- SFX/haptic balance;
- game-over/restart flow;
- save/restore reliability.

Do not add a new engine layer just for architecture fashion.

**Done when:** core remains pleasant across long repeated sessions and has no known state-consistency defects.

### Phase 2 — Apply the approved Visual Bible

Implement the accepted art direction incrementally:

1. gameplay-clean pass first;
2. lighter board frame and HUD;
3. simpler/readable tile set;
4. consistent typography/colors/components;
5. Workshop/Blueprint meta screens after gameplay is proven.

Generated concept screens are references, not exact implementation specs.

**Done when:** a real-device gameplay screenshot is readable immediately and still conveys premium industrial steampunk.

### Phase 3 — Universal Reward layer

Create one reward path for gameplay/meta systems.

Target reward types initially:

- current soft/economy resources;
- Workshop Parts;
- Blueprint Pieces;
- Cosmetic Unlocks.

Flow:

```text
source
→ RewardSystem
→ validate/apply
→ persist
→ RewardPresentation
```

**Done when:** contracts/events/collections do not mutate economy independently.

### Phase 4 — Workshop meta v2

Evolve the existing Workshop from numeric progression toward visible restoration.

Initial scope:

- one workshop zone;
- 3–5 machines;
- several visible restoration stages per machine;
- one clear resource cost path;
- machine completion gives a real unlock/reward.

**Done when:** after a run the player can see a permanent physical change in the world.

### Phase 5 — Contracts

Create data-driven quest definitions that consume gameplay events.

Initial types:

- make/reach tile;
- merge count;
- score / total score;
- combo count;
- runs played;
- moves survived.

Initial player-facing scope:

- 3 Daily Contracts;
- 1 Weekly Contract.

The current Daily Challenge can be migrated/absorbed rather than duplicated.

**Done when:** a new contract can be added through data/config rather than new gameplay code.

### Phase 6 — Blueprint Collection

First collection example:

```text
Steam Engine
- Boiler
- Piston
- Valve
- Flywheel
- Regulator
- Pressure Gauge
```

Pieces come from milestones/contracts/events. Completing the set unlocks/restores a Workshop machine or meaningful cosmetic.

**Done when:** collection completion visibly affects the game world.

### Phase 7 — Analytics cleanup around the new loops

Keep/extend the existing analytics abstraction with stable product events:

- run start/end/restart;
- milestone/new highest tile;
- contract progress/completion;
- Workshop upgrade;
- blueprint obtained/set completed;
- event participation;
- purchase start/complete when billing exists.

Do not add advertising events while ADR 0001 is active.

Core metrics:

- sessions/user;
- run duration;
- restart rate;
- D1/D3/D7/D30;
- time to Workshop milestones;
- contract participation/completion;
- collection progression;
- payer conversion only if non-ad purchases are later implemented.

### Phase 8 — Remote Config

Make configurable without a client build:

- contract definitions/rewards;
- Workshop costs;
- reward multipliers;
- feature flags;
- event schedule/milestones;
- non-ad store offer enable/disable only if such offers later exist.

Local defaults remain sufficient for offline start/gameplay.

### Phase 9 — Return loop

Use a soft return structure:

- Daily Contracts;
- modest daily Workshop bonus;
- forgiving short streak;
- simple comeback presentation after absence.

Avoid punitive long streak loss.

### Phase 10 — Weekly deterministic challenge

Use a shared challenge definition:

```text
challengeId
seed
rules
start/end
```

Players receive equivalent deterministic spawn conditions. If a public leaderboard becomes important, send replay/move data or another verifiable representation so the backend can validate scores.

### Phase 11 — LiveOps framework v1

One event definition should configure:

- schedule;
- scoring rule;
- milestones;
- rewards;
- theme;
- optional collection.

First event should reuse normal gameplay rather than becoming a separate minigame.

**Critical gate:** the second event should mostly be new data/assets, not a new architecture.

### Phase 12 — Optional monetization without advertising

Advertising is explicitly out of scope. Do not re-enable Yandex ads, add another ad SDK, build rewarded/interstitial placements, create ad-driven rewards or add a Remove Ads product.

If retention and product data later justify monetization, only non-ad paths may be explored under the current decision, for example:

- tile cosmetics;
- Workshop themes;
- small cosmetic/starter bundles;
- other direct-purchase cosmetic content that does not gate the 2048 core.

Do not add energy/lives to block the core.

### Phase 13 — Reward Track / Season Pass only after retention proof

Create reusable `RewardTrack` first. Consider Season Pass only after:

- D7/D30 are measured and stable enough;
- events work;
- content production cadence exists;
- economy is understood.

### Phase 14 — Social only if justified

Prefer light asynchronous social:

- friend leaderboard;
- weekly percentile;
- score sharing;
- partner challenge.

Do not start with guild wars or real-time PvP.

## 7. Explicitly out of scope until a later decision supports it

- all in-game advertising: rewarded, interstitial, banner and native;
- ad-driven rewards and reward multipliers;
- ad-specific monetization analytics;
- Remove Ads;
- new ad SDK integrations or migrations;
- energy/lives gating;
- gacha/character rarity;
- many new currencies;
- guild/clan wars;
- real-time PvP;
- subscriptions without continuous value;
- several parallel passes;
- large narrative campaign;
- unrelated minigames.

The advertising items above are additionally governed by ADR 0001 and may not be resumed merely because other out-of-scope items become justified.

## 8. Quality gates

### Core gate

- deterministic rules tested;
- save/restore correct;
- responsive input;
- readable tiles;
- stable animation;
- no known state duplication/loss.

### Meta gate

- one reward path;
- Workshop progress understandable;
- Contracts use gameplay events;
- economy sources/sinks observable;
- analytics already running.

### Retention gate

- Daily/Weekly loops measured;
- one reusable event framework;
- second event does not require architectural rewrite.

### Optional monetization gate

- advertising remains disabled and frozen under ADR 0001;
- free core remains complete;
- direct purchases, if ever implemented, restore correctly;
- additional currency only introduced for a proven need.

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
ONE REUSABLE LIVEOPS FRAMEWORK
+
OPTIONAL NON-AD MONETIZATION
+
DATA-DRIVEN ITERATION
```

The rule for every new feature:

1. What player/product problem does it solve?
2. How does it send the player back to the core?
3. How will we measure it?
4. Can the same problem be solved more simply?
5. Are we willing to remove it if the data says it does not work?
