package com.steamforge.game.monetization

/**
 * Pure session policy for automatic interstitial timing.
 *
 * SDK availability is intentionally outside this class: a pending ad moment must survive a missing
 * loaded ad so AdsManager can retry it at the next natural pause. Rewarded suppresses exactly one
 * interstitial opportunity on the same result pause.
 */
internal class InterstitialSessionPolicy(
    private val cfg: AdsConfig,
) {
    private var gamesFinished = 0
    private var interstitialPending = false
    private var rewardedShownSincePause = false

    fun onGameFinished() {
        if (!cfg.enabled) return
        gamesFinished++
        if (
            gamesFinished >= cfg.interstitialMinGames &&
            (gamesFinished - cfg.interstitialMinGames) % cfg.interstitialEveryGames == 0
        ) {
            interstitialPending = true
        }
    }

    fun onRewardedShown() {
        if (!cfg.enabled) return
        rewardedShownSincePause = true
    }

    /**
     * Returns whether AdsManager may attempt the automatic interstitial now.
     * Merely asking does not consume a pending moment unless it is the one-shot rewarded suppression.
     */
    fun shouldAttemptInterstitial(): Boolean {
        if (!cfg.enabled) return false
        if (rewardedShownSincePause) {
            rewardedShownSincePause = false
            return false
        }
        return interstitialPending && cfg.interstitialEnabled
    }

    /** Call only after a concrete loaded interstitial is about to be shown. */
    fun onInterstitialAttemptStarted() {
        if (!cfg.enabled) return
        interstitialPending = false
    }

    /** A failed show restores the same natural ad moment instead of losing it. */
    fun onInterstitialAttemptFailed() {
        if (!cfg.enabled) return
        interstitialPending = true
    }
}
