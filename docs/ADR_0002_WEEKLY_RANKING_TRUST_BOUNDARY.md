# ADR 0002 — Weekly rankings are server-authoritative and backend-vendor-neutral

- **Status:** Accepted
- **Date:** 2026-09-06
- **Scope:** Weekly Challenge submissions, ranking, leaderboard/percentile transport and backend validation

## Context

Steamforge Weekly Challenge uses a deterministic challenge id, seed, rules and move sequence. The Android client can reproduce a run locally, but any competitive result that accepts a client-provided score as authoritative is trivially forgeable by a modified client.

The repository already defines a backend-neutral `WeeklyRankingProvider`, a stable `WeeklyRunSubmission` wire payload and deterministic replay validation. A concrete ranking backend and player-identity provider have not been selected yet.

## Decision

Weekly ranking is server-authoritative.

A production ranking service must never accept `finalScore`, percentile or leaderboard position solely because the Android client supplied them. For a ranked attempt the service must:

1. resolve the canonical Weekly challenge by `challengeId`;
2. compare the submitted protocol version and seed with the canonical challenge;
3. decode and replay the submitted move sequence using the canonical deterministic rules;
4. require terminal validation equivalent to `WeeklyRunReplay.validateTerminal()`;
5. derive the accepted score from the replayed state and reject mismatches;
6. compute percentile/rank only from server-side accepted results.

Client-side `WeeklyRunRecorder.verifiedSubmission()` remains a useful fail-closed guard and catches runtime drift before transport, but it is not a security boundary against a modified client.

## Client contract

The Android client remains backend-vendor-neutral:

- gameplay/core code must not depend on a concrete backend SDK;
- `WeeklyRankingProvider` is the runtime boundary;
- the default/offline provider returns `UNAVAILABLE` and never fabricates percentile or rank;
- ranking/network failure must not break the completed local Weekly run;
- transport must use HTTPS with bounded request/response sizes and explicit timeouts;
- coroutine cancellation must propagate normally;
- the signed 64-bit Weekly `seed` must cross the JSON boundary as a decimal string, never as a JSON number, so IEEE-754/JavaScript backends cannot lose replay precision;
- no backend secret, administrator key or signing secret may be embedded in the APK.

The existing bounded HTTPS pattern used by Remote Config should be preferred unless a selected backend requires a stronger official client transport for authentication or integrity.

## Identity and abuse prevention

Ranking requires a server-recognized player/session identity so the backend can deduplicate attempts and rate-limit abuse. The concrete identity provider is intentionally not selected in this ADR.

Whatever provider is chosen must satisfy these rules:

- account/player creation that requires privileged credentials happens server-side;
- secrets stay outside the APK;
- a client-generated identifier alone is not treated as proof of legitimacy;
- identity is used for ownership/deduplication, never as a substitute for replay validation;
- privacy policy and data disclosures are updated before production if a persistent or pseudonymous server identifier is transmitted.

## Backend choice

PlayFab, Firebase or a custom HTTPS service may implement `WeeklyRankingProvider`, but the gameplay and ranking domain contracts must not change merely because the vendor changes.

A vendor is selected only when deployment, identity/authentication, persistence, privacy and operational ownership are defined together. Adding a network endpoint without those decisions is explicitly out of scope.

## Consequences

- A fake client score cannot directly become a ranked score.
- Percentile cannot be produced honestly until the backend has a real accepted-result population.
- Weekly may remain hidden or show ranking as unavailable while no production ranking service is configured.
- A future backend implementation must share or exactly reproduce the deterministic replay contract; divergence between client and server replay is a correctness bug.

## Superseding this decision

A later ADR may choose the concrete backend/identity stack, but it must preserve server-authoritative replay validation unless it replaces it with a demonstrably stronger trust model. Any proposal to allow direct competitive score writes from the client requires an explicit superseding ADR and security rationale.
