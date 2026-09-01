# Steamforge game logic audit — 2026

This note records correctness findings from the release hardening pass. It intentionally separates bug fixes from balance changes.

## Fixed in `fix/game-state-consistency`

- **Undo now rewinds deterministic RNG position.** Replaying the same move after Undo produces the same spawn instead of silently changing the future random sequence.
- **Undo restores session counters produced by the cancelled move.** Merge, high-merge, max-merge and overdrive statistics no longer count an undone move. The Undo itself still increments the Undo counter.
- **Active saves persist all session counters.** Process recreation no longer resets merge/overdrive/undo counters used by end-of-game statistics and achievements.
- **Save format is upgraded to v4 with v3/v2/v1 read compatibility.** Existing saves remain readable.
- **Wrench actions are enforced by the ViewModel, not only by recomposition timing.** Removal requires an active removal mode, stale second tile clicks are ignored, finished games cannot accept wrench actions, and removal mode cannot be entered without enough gems.
- **Paid power-up balance is reflected in UI immediately and clamped at zero when persisted.** This closes the short window in which another paid action could still observe the pre-spend gem balance.

## Reviewed but intentionally not changed automatically

### Overdrive scoring granularity

`overdriveRemaining` is counted in merges, while the current engine accepts a single score multiplier for the whole move. If a move creates more merges than the remaining Overdrive count, all merges in that move currently receive the multiplier while the counter clamps to zero afterward.

Changing this would alter scoring balance, progression speed and existing high-score expectations. It should be treated as a game-design decision rather than a correctness migration. Recommended options for a later balance pass:

1. Keep the current whole-move bonus and rename/reword the UI so the rule is explicit; or
2. Change the engine to apply the multiplier only to the first N merge events covered by `overdriveRemaining`.

Do not silently change this rule in a release-hardening patch.

### Local-clock daily challenges

Daily challenge selection and claim use the device-local day. This is acceptable for the current offline-first architecture but is not an anti-cheat mechanism. Server-authoritative time would require backend/product work and is outside the current release scope.
