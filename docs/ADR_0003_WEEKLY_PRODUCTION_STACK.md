# ADR 0003 — Weekly production stack uses VK ID, Ktor and PostgreSQL

- **Status:** Accepted
- **Date:** 2026-09-06
- **Scope:** Production Weekly ranking identity, transport, persistence, ranking population and deployment

## Context

ADR 0002 requires server-authoritative Weekly ranking, a server-recognized identity, replay-derived accepted scores, a real accepted population for percentile/rank, bounded HTTPS transport, and no privileged secret in the APK.

The shared repository now contains:

- deterministic replay authority in `:weekly-core`;
- stable ranking JSON in `:weekly-protocol`;
- server-side replay validation and authenticated ranking service boundaries in `:weekly-server-core`.

The remaining gap is a concrete production identity, persistence and deployment stack.

Steamforge is distributed through RuStore. The ranking stack therefore must not require installation from Google Play and must not assume Google Play services are available to every legitimate player.

## Decision

### Identity — VK ID OAuth 2.1 authorization-code flow with PKCE

VK ID is the production identity provider for Weekly ranking.

The ranked identity flow uses an authorization-code/PKCE design in which the backend owns the verifier and state:

1. the backend creates a cryptographically random login transaction, `state` and PKCE `code_verifier`;
2. only the derived S256 `code_challenge`, public transaction id and state are exposed to the Android client;
3. Android starts VK ID authorization with that externally supplied `code_challenge`;
4. Android receives `AuthCodeData` and sends only the authorization code, VK device id and login transaction id to the Steamforge backend;
5. the backend verifies the transaction/state and exchanges the code using its stored `code_verifier`;
6. the backend resolves VK ID user info and maps the stable VK subject to `WeeklyAuthenticatedPrincipal`;
7. ranking requests use a short-lived Steamforge server session derived from that verified principal.

The Android client must never be allowed to construct the ranking principal directly from a request-body user id.

The normal VK ID Android sample configuration exposes a value named `client_secret`; Steamforge must not treat that sample path as permission to ship a privileged credential. Production integration must use the external-code path described above. If the selected VK ID client library configuration cannot operate without embedding a privileged secret, the ranked login UI stays disabled until a compliant public-client/direct OAuth flow is implemented.

The VK ID OAuth code exchange itself is compatible with public-client PKCE: the current VK ID SDK implementation exchanges `code`, `code_verifier`, `client_id`, `device_id`, `redirect_uri` and `state` and does not send `client_secret` for this exchange.

### Backend runtime — Kotlin/JVM + Ktor

The production Weekly HTTP service uses Kotlin/JVM 17 and Ktor.

Reasons:

- the deterministic replay and ranking service are already Kotlin/JVM;
- the backend can reuse the same `:weekly-core`, `:weekly-protocol` and `:weekly-server-core` code instead of reimplementing replay in another language;
- Ktor is coroutine-native, preserving the cancellation behavior already required by the ranking service;
- no backend framework code enters the Android APK.

The first production baseline targets Ktor `3.5.2`. Dependency upgrades remain routine maintenance and do not require a new ADR unless they change the trust model.

### Persistence — PostgreSQL

Weekly accepted population is stored in PostgreSQL. The first production baseline uses PostgreSQL 18 with pgJDBC `42.7.13` or later security-compatible maintenance releases.

Database credentials stay outside Git and outside the APK.

### Deployment — Yandex Cloud

Initial production deployment target:

- Yandex Serverless Containers for the Ktor service;
- Yandex Managed Service for PostgreSQL for ranking persistence;
- a private cloud network between container and database where practical;
- Yandex Lockbox for database/session-signing credentials and other operational secrets;
- least-privilege service accounts for container runtime, registry pull and secret access.

This deployment is replaceable behind the existing server/domain boundaries. Moving to another provider does not require gameplay or wire-contract changes.

## Accepted-attempt policy

Population is deduplicated by authenticated principal and `challengeId`.

For each principal/challenge pair, the stored competitive result is the **best replay-accepted score** seen for that challenge.

- a lower later score does not replace the stored result;
- a higher later score replaces it;
- an equal score keeps the earlier accepted result;
- rejected or malformed submissions never touch the accepted population.

This policy allows retries without letting repeated submissions inflate participant count.

## Rank and percentile policy

Only the deduplicated, replay-accepted population for the same `challengeId` is considered.

For a participant with accepted score `S` in a population of `N` participants:

- `rank = 1 + count(score > S)`;
- participants with equal score receive the same competition rank;
- `percentile = 100.0 * count(score <= S) / N`;
- `participantCount = N`.

Consequences:

- the highest score always receives percentile `100.0`;
- tied scores receive the same percentile;
- percentile/rank are never fabricated when `N == 0` or persistence is unavailable;
- ordering used only for database pagination/debug presentation must not alter rank/percentile semantics.

## Transport and session requirements

The production service must:

- accept HTTPS only at the public edge;
- preserve the existing 16 KiB Weekly JSON payload bound or use a stricter one;
- use bounded connect/read/request timeouts;
- rate-limit login and submission endpoints by server-observed signals and authenticated principal where available;
- issue short-lived opaque or signed Steamforge sessions only after VK ID validation;
- reject expired, malformed or incorrectly scoped sessions;
- never include VK access/refresh tokens, database passwords or session-signing keys in Android logs/analytics;
- never return internal replay/storage errors as trusted ranking data.

## Minimal persisted data

The ranking database stores only what is required for the competitive feature:

- provider namespace (`vkid`);
- provider subject or a one-way server-derived stable subject key;
- `challengeId`;
- accepted score;
- accepted max tile level;
- accepted protocol version;
- accepted-at timestamp;
- operational timestamps needed for deduplication/auditing.

The raw move sequence does not need to remain indefinitely after successful replay unless an explicit anti-abuse/audit retention policy is later approved.

No phone number, email, real name, gender or birth date is required for Weekly ranking and these VK ID scopes/data must not be requested merely for leaderboard identity.

## Privacy and rollout gate

Before enabling ranked Weekly navigation for production users:

1. the privacy policy and store disclosures must describe VK ID authentication and pseudonymous leaderboard identity processing;
2. retention/deletion rules for leaderboard identity and accepted results must be documented;
3. production VK ID application configuration and redirect URIs must be verified;
4. Yandex Cloud secret/database/network configuration must be deployed with least privilege;
5. end-to-end tests must prove login -> replay validation -> best-attempt upsert -> real population ranking -> client verification;
6. the Weekly feature flag remains off until the service is reachable and healthy.

If identity or ranking is unavailable, the client continues to report `UNAVAILABLE`; it must not fall back to client-generated identity or fake percentile.

## Rejected alternatives

### Firebase Authentication + Play Integrity App Check as the default

Not selected as the RuStore-first default. Firebase documents support for apps distributed outside Google Play, but Play Integrity still introduces Google Play Console/Integrity coupling and is not necessary for the core identity requirement. Firebase remains a possible later secondary infrastructure choice if product/distribution requirements change.

### PlayFab client-side score/stat writes

Rejected for competitive authority because client-provided competitive scores are not the trust boundary defined by ADR 0002.

PlayFab server APIs remain technically usable behind a backend, but add another vendor account layer without removing the need for server validation, identity and persistence policy.

### Client-generated UUID as player identity

Rejected. It is useful as a local installation identifier but is not proof of a player/session and cannot be the ownership boundary for a public competitive population.

## References

- RuStore: VK ID authorization SDK — https://www.rustore.ru/help/sdk/vk-id
- VK ID Android SDK — https://github.com/VKCOM/vkid-android-sdk
- Ktor — https://ktor.io/
- pgJDBC — https://jdbc.postgresql.org/
- Yandex Serverless Containers + Managed PostgreSQL — https://yandex.cloud/en/docs/container-registry/tutorials/container-pg-connect
- Yandex Lockbox secrets for Serverless Containers — https://yandex.cloud/en/docs/lockbox/operations/serverless/containers

## Superseding this decision

A later ADR may replace VK ID, Yandex Cloud, PostgreSQL or Ktor, but it must preserve:

- server-recognized identity;
- no privileged credential in the APK;
- deterministic terminal replay validation;
- deduplicated accepted population;
- server-derived score/rank/percentile;
- real population only, never fabricated ranking data.
