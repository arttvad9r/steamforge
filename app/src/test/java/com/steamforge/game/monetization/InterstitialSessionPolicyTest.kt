package com.steamforge.game.monetization

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterstitialSessionPolicyTest {

    @Test
    fun `default ads config keeps advertising globally disabled`() {
        assertFalse(AdsConfig().enabled)
    }

    @Test
    fun `cadence schedules first interstitial at minimum then configured interval`() {
        val policy = InterstitialSessionPolicy(
            AdsConfig(interstitialMinGames = 3, interstitialEveryGames = 5),
        )

        repeat(2) {
            policy.onGameFinished()
            assertFalse(policy.shouldAttemptInterstitial())
        }

        policy.onGameFinished()
        assertTrue(policy.shouldAttemptInterstitial())
        policy.onInterstitialAttemptStarted()

        repeat(4) {
            policy.onGameFinished()
            assertFalse(policy.shouldAttemptInterstitial())
        }

        policy.onGameFinished()
        assertTrue(policy.shouldAttemptInterstitial())
    }

    @Test
    fun `missing loaded ad does not consume pending natural pause`() {
        val policy = InterstitialSessionPolicy(
            AdsConfig(interstitialMinGames = 1, interstitialEveryGames = 5),
        )

        policy.onGameFinished()

        assertTrue(policy.shouldAttemptInterstitial())
        assertTrue(policy.shouldAttemptInterstitial())
    }

    @Test
    fun `rewarded suppresses exactly one interstitial opportunity without losing pending`() {
        val policy = InterstitialSessionPolicy(
            AdsConfig(interstitialMinGames = 1, interstitialEveryGames = 5),
        )

        policy.onGameFinished()
        policy.onRewardedShown()

        assertFalse(policy.shouldAttemptInterstitial())
        assertTrue(policy.shouldAttemptInterstitial())
    }

    @Test
    fun `failed interstitial show restores pending moment`() {
        val policy = InterstitialSessionPolicy(
            AdsConfig(interstitialMinGames = 1, interstitialEveryGames = 5),
        )

        policy.onGameFinished()
        assertTrue(policy.shouldAttemptInterstitial())
        policy.onInterstitialAttemptStarted()
        assertFalse(policy.shouldAttemptInterstitial())

        policy.onInterstitialAttemptFailed()
        assertTrue(policy.shouldAttemptInterstitial())
    }

    @Test
    fun `disabled interstitial never becomes eligible to show`() {
        val policy = InterstitialSessionPolicy(
            AdsConfig(
                interstitialMinGames = 1,
                interstitialEveryGames = 1,
                interstitialEnabled = false,
            ),
        )

        repeat(3) { policy.onGameFinished() }

        assertFalse(policy.shouldAttemptInterstitial())
    }
}
