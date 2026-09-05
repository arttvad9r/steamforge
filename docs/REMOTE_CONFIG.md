# Steamforge Remote Config

Steamforge Remote Config is an optional meta-configuration channel. The game must remain fully launchable and playable without it.

## Enabling the remote provider

Set the Gradle property:

```properties
steamforge.remoteConfigUrl=https://example.com/steamforge/config.json
```

If the property is absent or blank, `LocalDefaultRemoteConfigProvider` is used and no Remote Config network request is made.

Only HTTPS endpoints are accepted. Redirects are disabled. The client uses bounded connection/read timeouts and rejects payloads larger than 64 KiB.

## Payload schema v1

```json
{
  "schemaVersion": 1,
  "workshopUpgradeCosts": [20, 35, 55, 80],
  "contractRewardMultiplier": 1.0,
  "rewardMultiplier": 1.0,
  "featureFlags": {
    "weeklyChallengeEnabled": false,
    "liveOpsEnabled": false,
    "returnLoopEnabled": false
  }
}
```

Unknown JSON fields are ignored so additive server-side metadata does not break older clients. Unknown schema versions are rejected entirely.

Validation rules:

- Workshop costs must contain the expected number of positive, strictly increasing values; otherwise compiled defaults are used for that field.
- Multipliers must be finite and within `0.25..4.0`; otherwise compiled defaults are used for that field.
- A malformed or unsupported remote payload never overwrites the last known-good cache.

## Offline/cache behavior

Startup always begins from compiled local defaults.

When the HTTPS provider is enabled, refresh proceeds in this order:

1. load and validate the last known-good cached payload;
2. expose it as a `CACHE` snapshot when valid;
3. request the HTTPS payload;
4. validate/sanitize it;
5. persist the sanitized payload and expose it as a `REMOTE` snapshot.

If cache or network access fails, the current valid snapshot remains active. Connectivity is never required to enter or play the game.

## Deliberately excluded values

Remote Config must not contain or change:

- board dimensions;
- spawn probability;
- RNG behavior/seeds;
- merge/scoring rules;
- active-run save semantics.

Those values participate in deterministic gameplay/save behavior and require explicit per-run versioning before they could ever become configurable.

Advertising is also not a Remote Config feature. ADR 0001 keeps all advertising and advertising development frozen; feature flags must not be used to bypass that decision.

## Current consumers

The first runtime consumer is Workshop upgrade costs. Other fields exist as validated schema surface for subsequent target-architecture work and must only be connected when the corresponding product system can apply the value consistently in UI, persistence, economy, and analytics.
