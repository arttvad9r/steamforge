package com.steamforge.game.config

import com.steamforge.game.progression.ContractType
import com.steamforge.game.progression.EventMilestone
import com.steamforge.game.progression.EventMetric
import com.steamforge.game.progression.EventReward
import com.steamforge.game.progression.EventScoringRule
import com.steamforge.game.progression.EventTheme
import com.steamforge.game.progression.LiveOpsCatalog
import com.steamforge.game.progression.ProgressionConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteGameConfigTest {

    @Test
    fun `provider starts from local defaults offline`() {
        val provider = FallbackGameConfigProvider()
        assertEquals(LocalDefaultConfig.value, provider.config.value)
        assertFalse(runBlocking { provider.refresh() })
        assertEquals(LocalDefaultConfig.value, provider.config.value)
    }

    @Test
    fun `failed remote fetch never clears defaults`() {
        val provider = FallbackGameConfigProvider(RemoteConfigSource { error("offline") })
        assertFalse(runBlocking { provider.refresh() })
        assertEquals(LocalDefaultConfig.value, provider.config.value)
    }

    @Test
    fun `unsupported schema never replaces known snapshot`() {
        val incompatible = LocalDefaultConfig.value.copy(schemaVersion = 999, rewardMultiplierPercent = 500)
        val provider = FallbackGameConfigProvider(RemoteConfigSource { incompatible })

        assertFalse(runBlocking { provider.refresh() })
        assertEquals(LocalDefaultConfig.value, provider.config.value)
    }

    @Test
    fun `successful remote fetch atomically replaces snapshot`() {
        val remote = LocalDefaultConfig.value.copy(
            rewardMultiplierPercent = 125,
            features = FeatureFlags(weeklyChallenge = false),
        )
        val provider = FallbackGameConfigProvider(RemoteConfigSource { remote })

        assertTrue(runBlocking { provider.refresh() })
        assertEquals(remote, provider.config.value)
        assertFalse(provider.config.value.features.weeklyChallenge)
    }

    @Test
    fun `economy tuning cannot change fundamental core rules`() {
        val base = ProgressionConfig(
            pressureMax = 137,
            pressureBaseGain = 9,
            pressureGainPerLevel = 11,
            overdriveMerges = 6,
            overdriveMultiplier = 3,
            wrenchMaxTileLevel = 5,
        )
        val tuned = EconomyTuning(
            xpScoreDivisor = 7,
            freeUndosPerGame = 4,
            undoGemsCost = 17,
            wrenchGemsCost = 23,
        ).applyTo(base)

        assertEquals(137, tuned.pressureMax)
        assertEquals(9, tuned.pressureBaseGain)
        assertEquals(11, tuned.pressureGainPerLevel)
        assertEquals(6, tuned.overdriveMerges)
        assertEquals(3, tuned.overdriveMultiplier)
        assertEquals(5, tuned.wrenchMaxTileLevel)
        assertEquals(7, tuned.xpScoreDivisor)
        assertEquals(4, tuned.freeUndosPerGame)
        assertEquals(17, tuned.undoGemsCost)
        assertEquals(23, tuned.wrenchGemsCost)
    }

    @Test
    fun `unsafe economy values are clamped`() {
        val tuned = EconomyTuning(
            xpScoreDivisor = 0,
            freeUndosPerGame = 999,
            undoGemsCost = -9,
            dailyRewardCycle = 0,
            dailyRewardGemsBase = -100,
        ).applyTo()

        assertEquals(1, tuned.xpScoreDivisor)
        assertEquals(10, tuned.freeUndosPerGame)
        assertEquals(0, tuned.undoGemsCost)
        assertEquals(1, tuned.dailyRewardCycle)
        assertEquals(0, tuned.dailyRewardGemsBase)
    }

    @Test
    fun `reward multiplier is clamped and overflow safe`() {
        assertEquals(0, RemoteGameConfig(rewardMultiplierPercent = -10).scaleReward(50))
        assertEquals(250, RemoteGameConfig(rewardMultiplierPercent = 500).scaleReward(50))
        assertEquals(250, RemoteGameConfig(rewardMultiplierPercent = 900).scaleReward(50))
        assertEquals(0, RemoteGameConfig(rewardMultiplierPercent = 100).scaleReward(-50))
        assertEquals(Int.MAX_VALUE, RemoteGameConfig(rewardMultiplierPercent = 500).scaleReward(Int.MAX_VALUE))
    }

    @Test
    fun `feature flags are independent`() {
        val flags = FeatureFlags(
            dailyChallenge = false,
            dailyContracts = true,
            weeklyChallenge = false,
            liveOps = true,
            rewardedAds = false,
            offers = true,
        )
        assertFalse(flags.dailyChallenge)
        assertTrue(flags.dailyContracts)
        assertFalse(flags.weeklyChallenge)
        assertTrue(flags.liveOps)
        assertFalse(flags.rewardedAds)
        assertTrue(flags.offers)
    }

    @Test
    fun `contract tuning recovers safe bounds and enabled set`() {
        val tuning = ContractTuning(
            contractsPerDay = 999,
            targetScalePercent = 2,
            rewardScalePercent = 999,
            enabledTypes = emptySet(),
        )
        assertEquals(ContractType.entries.size, tuning.safeContractsPerDay)
        assertEquals(50, tuning.safeTargetScalePercent)
        assertEquals(500, tuning.safeRewardScalePercent)
        assertEquals(ContractType.entries.toSet(), tuning.safeEnabledTypes)
    }

    @Test
    fun `event template sanitizes duplicate ids and targets`() {
        val template = LocalDefaultConfig.foundryTemplate.copy(
            milestones = listOf(
                EventMilestone("a", 200, EventReward(gems = 1)),
                EventMilestone("a", 100, EventReward(gems = 2)),
                EventMilestone("b", 200, EventReward(gems = 3)),
                EventMilestone("c", 300, EventReward(gems = 4)),
            ),
        )
        val event = template.instantiateForEpochDay(25_000L)

        assertEquals(listOf(200, 300), event.milestones.map { it.targetPoints })
        assertEquals(listOf("a", "c"), event.milestones.map { it.id })
    }

    @Test
    fun `scheduled event overrides fallback while active`() {
        val day = 25_000L
        val scheduled = LocalDefaultConfig.foundryTemplate.instantiateForEpochDay(day).copy(
            id = "remote-maintenance-week",
            startEpochDay = day,
            endEpochDayExclusive = day + 3,
            scoringRule = EventScoringRule(EventMetric.SCORE, pointsPerUnit = 1, unitsPerStep = 100),
            milestones = listOf(EventMilestone("score-10", 10, EventReward(gems = 1))),
            theme = EventTheme("maintenance", "MAINTENANCE WEEK", "Собирайте очки", "teal"),
        )
        val config = RemoteGameConfig(scheduledEvents = listOf(scheduled))

        assertEquals("remote-maintenance-week", config.activeEvent(day)?.id)
        assertNotEquals("remote-maintenance-week", config.activeEvent(day + 4)?.id)
    }

    @Test
    fun `liveops feature flag disables active event`() {
        val config = RemoteGameConfig(features = FeatureFlags(liveOps = false))
        assertNull(config.activeEvent(25_000L))
    }

    @Test
    fun `local remote fallback stays aligned with current foundry catalog`() {
        val day = 25_000L
        val configured = LocalDefaultConfig.value.activeEvent(day)!!
        val catalog = LiveOpsCatalog.activeForEpochDay(day)

        assertEquals(catalog.id, configured.id)
        assertEquals(catalog.scoringRule, configured.scoringRule)
        assertEquals(catalog.milestones.map { it.targetPoints }, configured.milestones.map { it.targetPoints })
        assertEquals(catalog.milestones.map { it.reward }, configured.milestones.map { it.reward })
    }
}
