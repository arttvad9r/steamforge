# Gameplay Visual Polish V1

This pass moves Steamforge closer to the approved premium industrial-steampunk direction without changing 2048 rules, economy, ads, analytics, or progression.

## Intent

- gameplay remains the cleanest surface in the product;
- large tile values stay dominant and immediately readable;
- premium feel comes from material, bevel, lighting and restrained motion rather than decorative clutter;
- low tiers read as quiet machined metal;
- copper and antique gold build through the mid tiers;
- 512 shifts into oxidized green metal, 1024 into deep teal, and 2048 becomes the energized teal core from the approved gameplay concept;
- merge feedback should feel mechanical and weighted, not bouncy or arcade-like.

## Implemented in this pass

- deeper charcoal workshop base palette;
- less saturated brass/copper/patina accents;
- tile progression aligned to the approved gameplay concept: pale metal → copper → antique gold → oxidized green → deep teal → energized teal;
- denser multi-step tile bevel for a machined plate appearance;
- reduced merge scale amplitude;
- reduced combo pitch escalation while preserving tier hierarchy.

## Lifecycle boundary

An experimental shared `SteamUi.kt` material pass was tested during development but caused Activity recreation instability in emulator CI. That shared-shell change was removed from this PR; the proven master implementation remains in place. Shared panels, backdrop, buttons and pressure dial therefore are not part of Gameplay Visual Polish V1.

## Explicit non-goals

- no GameEngine changes;
- no spawn/scoring/balance changes;
- no ad or analytics changes;
- no new meta systems;
- no decorative gears, fantasy-steampunk props, or board clutter;
- no shared app-shell redesign in this pass.

## Next visual pass

Recompose the gameplay HUD and board framing around the approved minimal hierarchy, then polish Workshop/Home and shared surfaces in isolated, lifecycle-safe passes.
