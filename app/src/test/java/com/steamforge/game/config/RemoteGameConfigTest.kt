package com.steamforge.game.config

import com.steamforge.game.progression.ProgressionConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteGameConfigTest {

    @Test
    fun `local provider is immediately usable offline`() = runTest {
        val provider = LocalDefaultRemoteConfigProvider()

        val snapshot = provider.snapshot.value
        assertEquals(RemoteConfigSource.LOCAL_DEFAULT, snapshot.source)
        assertEquals(LocalDefaultConfig.value, snapshot.config)
        assertEquals(RemoteConfigRefreshResult.LOCAL_FALLBACK, provider.refresh())
    }

    @Test
    fun `unknown schema falls back to compiled defaults`() {
        val invalid = RemoteGameConfig(
            schemaVersion = 999,
            workshopUpgradeCosts = listOf(1, 2, 3, 4),
            contractRewardMultiplier = 2.0,
            rewardMultiplier = 2.0,
            featureFlags = RemoteFeatureFlags(weeklyChallengeEnabled = true),
        )

        assertEquals(LocalDefaultConfig.value, invalid.sanitized())
    }

    @Test
    fun `invalid meta values fall back without discarding valid feature flags`() {
        val config = RemoteGameConfig(
            workshopUpgradeCosts = listOf(20, 15, -1, 80),
            contractRewardMultiplier = Double.NaN,
            rewardMultiplier = 20.0,
            featureFlags = RemoteFeatureFlags(
                weeklyChallengeEnabled = true,
                liveOpsEnabled = false,
                returnLoopEnabled = true,
            ),
        )

        val safe = config.sanitized()
        assertEquals(LocalDefaultConfig.value.workshopUpgradeCosts, safe.workshopUpgradeCosts)
        assertEquals(1.0, safe.contractRewardMultiplier, 0.0)
        assertEquals(1.0, safe.rewardMultiplier, 0.0)
        assertTrue(safe.featureFlags.weeklyChallengeEnabled)
        assertFalse(safe.featureFlags.liveOpsEnabled)
        assertTrue(safe.featureFlags.returnLoopEnabled)
    }

    @Test
    fun `valid remote meta config only overrides workshop costs in progression bridge`() {
        val base = ProgressionConfig(
            pressureMax = 130,
            undoGemsCost = 7,
            wrenchGemsCost = 13,
        )
        val config = RemoteGameConfig(
            workshopUpgradeCosts = listOf(25, 45, 70, 100),
            contractRewardMultiplier = 1.25,
            rewardMultiplier = 1.1,
        )

        val progression = config.toProgressionConfig(base)

        assertEquals(listOf(25, 45, 70, 100), progression.workshopCoreUpgradeCosts)
        assertEquals(130, progression.pressureMax)
        assertEquals(7, progression.undoGemsCost)
        assertEquals(13, progression.wrenchGemsCost)
    }
}
