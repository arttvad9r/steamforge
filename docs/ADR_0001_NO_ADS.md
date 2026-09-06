# ADR 0001 — In-game advertising is prohibited

- **Status:** Accepted
- **Date:** 2026-09-05
- **Implementation status:** advertising SDK/runtime removal applied 2026-09-06
- **Scope:** Steamforge product, Android client, rewards and monetization roadmap

## Context

Steamforge previously contained Yandex rewarded/interstitial infrastructure, ad-driven reward flows and historical Remove Ads plans.

The product decision is to develop Steamforge without in-game advertising. This is stronger than keeping ads disabled by a feature switch: advertising must not exist as a production runtime/dependency/UI surface.

## Decision

Steamforge must not add, restore or expand in-game advertising unless a later accepted ADR explicitly supersedes this decision.

Prohibited product/runtime surface:

- rewarded ads;
- interstitial ads;
- banner/native ads;
- ad-driven reward multipliers or bonus rewards;
- ad offer/start/complete flows;
- ad-specific telemetry;
- Remove Ads purchases;
- ad-network SDKs;
- ad unit IDs or advertising credentials;
- advertising consent UI;
- advertising settings UI.

## Implementation invariant

The production Android dependency graph and manifest must not contain an advertising SDK or ad-network initialization metadata.

Historical gameplay call sites may temporarily use a strictly inert compatibility shell only while they are mechanically removed. Such a shell must:

- have no advertising dependency;
- perform no network request;
- never report rewarded readiness;
- never show an ad;
- never grant an ad-driven reward;
- never expose an advertising surface to the player.

It is not a roadmap feature and should disappear as orchestration code is decomposed.

## Allowed monetization work

This decision does not prohibit future non-ad monetization. If separately justified, Steamforge may later consider direct-purchase cosmetic content such as tile sets or Workshop themes. Such work must not depend on advertising and must have a separate product/release decision.

## Implementation rule

Any task involving rewarded/interstitial/banner/native ads, ad SDKs, ad telemetry, ad-driven rewards or Remove Ads must be rejected unless a newer accepted ADR explicitly supersedes ADR 0001.

## Superseding this decision

Changing this decision requires a new ADR that:

1. explicitly supersedes ADR 0001;
2. states the product reason;
3. defines placements/player-experience constraints;
4. defines privacy/store/compliance implications;
5. updates `docs/PRODUCT_PLAN.md` in the same change.

Until then, **Steamforge is an advertising-free product and the shipped application must contain no advertising SDK or player-facing advertising surface**.
