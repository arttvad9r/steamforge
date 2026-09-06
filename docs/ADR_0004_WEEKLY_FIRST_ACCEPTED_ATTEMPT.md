# ADR 0004 — Weekly ranking uses the first accepted attempt

- **Status:** Accepted
- **Date:** 2026-09-06
- **Scope:** Weekly competitive attempt ownership and deduplication
- **Supersedes:** only the **Accepted-attempt policy** subsection of ADR 0003

## Context

ADR 0003 selected VK ID, Ktor, PostgreSQL and Yandex Cloud and initially proposed keeping the best replay-accepted score per authenticated principal/challenge.

The existing Stage 16 wire/runtime trust boundary intentionally binds a ranked response to the exact submitted run:

- `WeeklyRankingSnapshot` carries one score;
- `WeeklyRankingResult.verifiedFor(submission)` accepts `RANKED` only when `challengeId` and `score` match that exact submission;
- this prevents a response for another run from being presented as the result of the current submission.

A best-attempt population conflicts with that invariant after a player has already stored a higher result and later submits a lower valid run. The honest stored competitive score would be the earlier best score, while the current submission has a different score. Returning the stored best would correctly fail client verification; returning a hypothetical ranking for the lower score would no longer describe the accepted population.

The protocol could be expanded later to represent both `submittedScore` and `acceptedBestScore`, but Stage 16 does not need that complexity to establish a secure first production leaderboard.

## Decision

For Weekly ranking V1, each authenticated principal has **one competitive accepted attempt per `challengeId`**.

The first submission that:

1. decodes under the current protocol;
2. resolves to the canonical challenge;
3. passes deterministic terminal replay validation; and
4. reaches the persistence transaction successfully

becomes that principal's ranked attempt for the challenge.

Later submissions for the same authenticated principal/challenge do not replace the stored competitive result and do not change participant count.

The server should return `REJECTED` for a later duplicate competitive submission. It must not return a `RANKED` payload for the stored first attempt in response to a different later submission, because that would violate exact-submission binding.

Malformed/replay-rejected submissions do **not** consume the one accepted attempt. A transient failure before persistence commits also does not consume it.

## Rank and percentile

ADR 0003 rank/percentile semantics remain unchanged and operate on the deduplicated first-accepted-attempt population:

- `rank = 1 + count(score > S)`;
- equal scores share competition rank;
- `percentile = 100.0 * count(score <= S) / N`;
- `participantCount = N`.

## Product consequence

Before Weekly ranking is exposed in UI, the player-facing flow must make the one-ranked-attempt rule clear before the competitive run begins.

A future casual/unranked replay can be offered separately without changing the accepted leaderboard population.

If product testing later requires best-attempt retries, introduce an explicit protocol revision that can distinguish the current submitted run from the participant's accepted leaderboard result. Do not weaken `verifiedFor(submission)` to make mismatched scores pass.

## Persistence requirement

PostgreSQL enforces uniqueness on `(challenge_id, authenticated_subject_key)`.

The accepted-attempt insert is first-write-wins. Duplicate-key handling must distinguish an already-consumed accepted attempt from infrastructure failure.

The rank query and successful insert should be transactionally coherent enough that the returned snapshot belongs to the exact newly accepted run.

## Security consequence

This policy keeps all existing Stage 16 trust guarantees intact:

- client score is never authoritative;
- identity is server-recognized;
- one principal cannot inflate participant count by resubmitting;
- ranked response remains bound to the exact verified submission;
- no fake percentile/rank is needed for duplicate attempts.
