package com.steamforge.game.monetization

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdDisplayPolicyTest {
    @Test
    fun `remove ads disables automatic interstitial only`() {
        val cfg = AdsConfig(rewardedEnabled = true, interstitialEnabled = true)

        assertFalse(AdDisplayPolicy.allowsInterstitial(removeAdsOwned = true, cfg = cfg))
        assertTrue(AdDisplayPolicy.allowsRewarded(cfg))
    }

    @Test
    fun `free player keeps configured interstitial policy`() {
        assertTrue(AdDisplayPolicy.allowsInterstitial(removeAdsOwned = false, cfg = AdsConfig()))
        assertFalse(
            AdDisplayPolicy.allowsInterstitial(
                removeAdsOwned = false,
                cfg = AdsConfig(interstitialEnabled = false),
            ),
        )
    }
}
