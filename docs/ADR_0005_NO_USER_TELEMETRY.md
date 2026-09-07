# ADR 0005 — No user analytics or telemetry SDK

- **Status:** Accepted
- **Date:** 2026-09-06
- **Scope:** Android client, analytics, privacy UI, release configuration

## Context

Steamforge previously integrated AppMetrica behind an explicit consent flow and kept a typed internal analytics event catalog for product/run/economy events.

The product now chooses a simpler privacy baseline: the shipped client must not collect or transmit user analytics/telemetry.

## Decision

The production Steamforge client must not contain or initialize a user analytics/telemetry SDK.

The following are prohibited unless a later accepted ADR explicitly supersedes this decision:

- AppMetrica or another product-analytics SDK;
- user/session behavior telemetry sent from the client;
- analytics API keys;
- analytics consent prompts;
- analytics toggles/settings;
- advertising identifiers or analytics-specific device/user identifiers;
- debug telemetry that mirrors user behavior outside normal local development diagnostics.

## Compatibility boundary

Existing typed event definitions/call sites may temporarily remain while gameplay/progression orchestration is decomposed, but the application-level implementation must be a strict in-process no-op:

- no network transmission;
- no persistence;
- no Logcat event stream;
- no SDK delegate;
- no hidden fallback uploader.

These internal event schemas are not a product telemetry feature and may be removed/renamed during later architecture cleanup.

## Network permissions

This ADR does not prohibit network access required for explicit non-telemetry product services such as:

- optional Remote Config;
- future authenticated Weekly ranking/backend functionality.

Those services must have their own bounded protocols and privacy/security decisions and must not be repurposed as general behavioral analytics.

## Release invariant

A production release must not require an analytics credential or analytics/privacy consent configuration to build or run.

## Superseding this decision

Reintroducing user telemetry requires a new ADR that explicitly supersedes ADR 0005 and defines:

1. why telemetry is necessary;
2. exactly what data is collected;
3. retention and identifiers;
4. consent/legal/store requirements;
5. the narrow SDK/backend choice;
6. player-facing controls if required.

Until then, **Steamforge ships without user analytics/telemetry collection**.
