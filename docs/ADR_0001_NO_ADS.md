# ADR 0001 — In-game advertising is disabled and frozen

- **Status:** Accepted
- **Date:** 2026-09-05
- **Scope:** Steamforge product, Android client, analytics, rewards and monetization roadmap

## Context

Steamforge previously contained Yandex rewarded/interstitial advertising infrastructure and roadmap items for rewarded placements, ad-driven rewards and Remove Ads.

The current product decision is to develop Steamforge without in-game advertising. This decision is intentionally stronger than merely keeping ads disabled in the current build: ad development itself is frozen so future work does not accidentally restart it from stale code or roadmap entries.

## Decision

Steamforge must not add, restore or expand in-game advertising unless a later ADR explicitly supersedes this decision.

The following are out of scope:

- rewarded ads;
- interstitial ads;
- banner/native ads;
- ad-driven reward multipliers or bonus rewards;
- ad offer/start/complete product flows;
- ad-specific analytics added for future monetization;
- a Remove Ads purchase;
- new ad-network SDKs or migrations to another ad provider.

Existing dormant/disabled advertising code may remain temporarily while unrelated work is in progress, but it is legacy code, not an active product surface and must not be treated as a roadmap item. It can be removed in a dedicated cleanup change when safe.

## Allowed monetization work

This decision does not prohibit future non-ad monetization. If separately justified by product data, Steamforge may later consider direct-purchase items such as cosmetics, Workshop themes or small cosmetic/starter bundles. Such work must not depend on advertising.

## Implementation rule

Before starting any task involving ads, rewarded offers, interstitials, ad SDKs, ad analytics or Remove Ads, check this ADR. The task must be rejected or reframed unless a newer accepted ADR explicitly supersedes ADR 0001.

## Superseding this decision

Changing this decision requires a new ADR that:

1. explicitly states that ADR 0001 is superseded;
2. explains the product reason for bringing advertising back;
3. defines acceptable placements and player-experience constraints;
4. defines privacy/store/compliance implications;
5. updates the canonical `docs/PRODUCT_PLAN.md` in the same change.

Until then, **Steamforge is a no-ads product and advertising development is frozen**.
