# Steamforge — Product & Development Plan

> **Status:** canonical development roadmap.  
> **Priority:** first bring the game itself and its visual presentation to the target quality level. Store publication, signing, store listings, moderation, release-candidate handoff and other publication work are intentionally out of scope until a later explicit decision.
>
> **Product decisions:** Steamforge is a **no-ads product** (`ADR_0001_NO_ADS.md`) and a **no-user-telemetry client** (`ADR_0005_NO_USER_TELEMETRY.md`). Do not restore advertising, AppMetrica, behavioral analytics SDKs, analytics consent UI, tracking credentials or ad-driven rewards.

## 1. Target product

Steamforge should become:

> **a premium-feeling stylized industrial-steampunk 2048/merge game with excellent core feel, highly readable material tiles and a living workshop/meta layer that visibly grows from play.**

Primary loop:

```text
2048 core
→ satisfying mechanical movement / merges
→ clear score and pressure feedback
→ rewards / contracts
→ visible workshop restoration
→ blueprint collection
→ reasons to return
→ back to the same core
```

The meta exists to strengthen the core, not replace it.

## 2. Current repository state

Already implemented/current:

- pure Kotlin 4×4 `GameEngine`;
- replayable deterministic RNG;
- autosave/process-death restore and backward-readable save format;
- save write format v6 without analytics/correlation identifiers;
- swipe + keyboard gameplay input;
- turn-sequenced animated input with one latest buffered direction (#179);
- movement/merge feedback, SFX and haptics;
- cold-start SFX load reliability (#177);
- Steam Pressure / Overdrive;
- Undo and Wrench, including visual-turn lockout while sequencing settles (#179);
- terminal-save presentation that keeps short persistence out of the final-move presentation (#181);
- Workshop progression with visible restoration/world changes (#182);
- achievements;
- Daily Challenge / daily reward;
- Contracts;
- dedicated Blueprints route/catalog for the persisted Steam Engine collection (#183);
- Workshop Parts / return-loop slices;
- authoritative `RewardSystem` for Workshop Parts, Gems, Blueprint Pieces and cosmetic unlocks;
- current game-finish, Daily Challenge, Contracts and Daily Reward positive grants already use `RewardSystem`;
- offline-safe Remote Config foundation;
- deterministic Weekly/replay/ranking foundations in separate modules;
- Android CI, lifecycle, accessibility, adaptive-window, high-tier, 16 KiB and performance diagnostics;
- gameplay visual passes #156–158: premium materials, reduced chrome and expanded portrait/tablet board;
- focused meta visual passes across Contracts/Daily, Home, Profile, Achievements, Workshop and Blueprints (#172–176, #182–184);
- Home stops continuous core animation when the Animations setting is disabled (#184);
- Settings has dedicated production visual-regression coverage with blank-capture rejection (#188);
- historical Contracts-specific save/finish persistence APIs were removed after normal transactions absorbed that accounting (#189);
- Daily Challenge reward claims retry transient I/O while preserving idempotency across ambiguous commit outcomes (#190);
- advertising SDK/runtime completely removed;
- AppMetrica/user analytics SDK/runtime completely removed;
- analytics/ad consent and Settings surfaces removed;
- no-tracking CI guard.

Weekly remains hidden from normal player navigation. It is not a near-term priority while the core and visual layer are still being raised to target quality.

### Current acceptance boundary

Automated gameplay/visual/lifecycle baselines are substantially stronger than the original roadmap state, but they do not replace physical-device acceptance. Issue #185 is the remaining manual game-readiness gate for swipe feel/latency, long-session high-tier readability, SFX/haptic balance, physical lifecycle restore and TalkBack/large-text spot checks.

The current Blueprint catalog intentionally contains only the Steam Engine collection. Do not add breadth merely to fill a screen; new collections should create or explain visible Workshop/world progression.

## 3. Product principles

### Core first

If Workshop, Daily and collections disappeared, the 2048 game should still feel polished and satisfying.

### Board first

Gameplay hierarchy:

1. board;
2. tiles/numbers;
3. movement and merge response;
4. score / Steam Pressure;
5. one current objective;
6. secondary controls.

### Premium through materials and light, not decoration density

Use bevel, material response, controlled highlights, soft atmospheric depth and restrained animation. Decorative steampunk elements must never compete with the board.

### Stylized, not cheap-cartoon and not photoreal

Target: premium stylized industrial steampunk with slightly simplified forms, high-quality materials and lighting. Avoid chibi/mobile-cartoon proportions and generic fantasy-steampunk motifs.

### Clean gameplay, richer meta screens

Gameplay should be the cleanest screen. Workshop, Blueprints and collection screens may carry more environmental detail because the environment itself is part of their function.

### No advertising / no behavioral telemetry

These are current product constraints, not temporary feature flags.

## 4. Current development order

### Phase 0 — Remove publication work from the active line

- no store-specific workflows or assets;
- no store listing / signing / publication plans;
- no release-candidate publication checklist;
- keep only engineering checks that directly protect the game: tests, lint, debug/minified build, lifecycle, accessibility, adaptive layout, performance, 16 KiB compatibility and no-tracking.

**Status:** current active line follows this boundary.

### Phase 1 — Core game feel

Polish the actual 2048 interaction before adding broad new meta systems.

Focus:

- swipe responsiveness and gesture confidence;
- movement timing;
- merge sequencing;
- spawn timing;
- merge hierarchy for single vs multi-merge moves;
- Steam Pressure / Overdrive readability;
- Undo/Wrench clarity;
- game-over and restart flow;
- SFX/haptic balance;
- eliminate visual or input latency that makes the core feel soft or cheap.

Quality gates:

- deterministic logic tests stay green;
- one gesture produces one command;
- no save/state regression;
- animation never makes board state ambiguous;
- interaction remains clear on compact and expanded layouts.

**Status:** the automated sequencing/SFX/terminal-persistence baseline is implemented in #177, #179 and #181. Physical touch feel and subjective audio/haptics remain open in #185.

### Phase 2 — Gameplay visual target

Use `docs/VISUAL_BIBLE.md` and approved project references as the source of truth.

Target:

> premium stylized industrial-steampunk, dark workshop atmosphere, warm brass/copper, restrained teal/patina accents, large readable tiles, high-quality materials and lighting, low decorative noise around the board.

Work order:

1. board proportions and spacing;
2. tile silhouette/bevel/material hierarchy;
3. number typography and contrast across all values;
4. merge/spawn/Overdrive visual feedback;
5. HUD hierarchy and score/pressure presentation;
6. background depth and edge machinery without cluttering the board;
7. phone portrait;
8. expanded/tablet portrait;
9. compact landscape;
10. high-tier tiles and long-session readability.

Do not reproduce concept screens pixel-for-pixel. Preserve their art direction while improving gameplay hierarchy where necessary.

**Status:** automated board/material/adaptive/high-tier baselines exist. Long-session high-tier readability on a real device remains an acceptance item in #185.

### Phase 3 — Whole-app visual system

After gameplay reaches the target level, bring the rest of the product to the same language.

Priority:

1. Home;
2. Workshop;
3. Blueprints / collections;
4. Contracts / Daily;
5. Profile;
6. Settings and utility surfaces.

Create/reuse one coherent system for:

- typography;
- panels;
- buttons;
- metallic materials;
- spacing;
- icons;
- atmospheric backgrounds;
- states/selection/disabled feedback;
- transitions.

Avoid adding permanent bottom navigation or decorative frames to gameplay if they weaken board dominance.

**Status:** focused passes are merged for Home, Workshop, Blueprints, Contracts/Daily, Profile and Achievements. Settings uses the shared components and now has dedicated production visual-regression coverage (#188); final whole-app acceptance remains open.

### Phase 4 — Workshop presentation v2

Make Workshop progression visibly meaningful without turning it into a second game.

Target:

- one workshop zone;
- 3–5 machines/objects;
- several restoration states per object;
- visible world change when progress is applied;
- simple costs/rewards;
- no extra currencies unless clearly necessary.

The goal is visible permanent trace from play, not a spreadsheet of levels.

**Status:** the current three mechanisms now produce visible bay/restoration changes in #182. Add further world breadth only if it strengthens the play → restoration loop.

### Phase 5 — Session orchestration cleanup

Refactor only after the user-facing quality baseline is established, unless architecture blocks the visual/gameplay work earlier.

Target boundaries:

```text
GameSessionController
├─ GameEngine
├─ RunPersistence
├─ Progression/Reward events
└─ CompetitiveRunRecorder

GameViewModel
└─ UI state + user intents
```

No big-bang rewrite. Keep manual DI unless actual complexity justifies something heavier. Prefer small behavior-preserving extractions that remove demonstrated duplication or risk. Historical Contracts-specific save/finish repository API variants were removed in #189 after their behavior moved into the normal transactions.

### Phase 6 — Reward layer consolidation

The authoritative positive-reward application path already exists:

```text
source
→ RewardSystem
→ validate/apply
→ persist
→ presentation
```

Current reward domain includes Workshop Parts, Gems, Blueprint Pieces and cosmetic unlocks. Current major positive-grant paths already use it: game finish, Daily Challenge, Contracts and Daily Reward.

Before expanding progression systems:

- keep future positive grants on `RewardSystem` where that improves consistency;
- keep claim/source idempotency in repository transactions; Daily Challenge now also retries transient/ambiguous persistence I/O without double-granting (#190);
- do not introduce parallel reward-application paths;
- extend the existing reward domain only when a current product feature requires it.

### Phase 7 — Contracts / Blueprints evolution

Keep contracts data-driven and fed by gameplay-domain events. Keep collections tied to visible workshop/world changes.

Do not branch core 2048 rules for individual meta features. The current single Steam Engine collection is acceptable until another collection has a concrete permanent-world payoff.

### Phase 8 — Storage boundary review

Preferences DataStore remains acceptable while state stays manageable. Before reward/history/event records become complex, choose the smallest standard solution that fits:

- Proto DataStore for one typed profile/state model; or
- Room for relational/history/query-heavy data.

Do not invent a custom database abstraction without need.

### Phase 9 — Return loop

Only after core + visual + Workshop quality are established:

- Daily goals;
- modest Workshop bonus;
- forgiving streak/comeback presentation;
- no punitive energy/lives gating.

Existing Daily/Contracts/Workshop reward slices already provide the current return-loop baseline. Do not expand this before #185 and final whole-app acceptance are closed.

### Phase 10 — Weekly/backend later

The existing replay/server foundation may remain in the repository, but further production identity/deployment/client integration is deferred. Do not expose Weekly UI until the base game and visual/meta experience justify expanding this surface.

### Phase 11 — LiveOps/social/monetization only after a new explicit product decision

Do not build these merely because historical branches contain foundations for them. Reassess only after the core game, visual presentation and Workshop/meta loop are strong.
