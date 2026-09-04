package com.steamforge.game.ui.workshop

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.analytics.AnalyticsEvents
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.PlayerProgress
import com.steamforge.game.progression.ProgressionConfig
import com.steamforge.game.progression.WorkshopMechanism
import com.steamforge.game.progression.WorkshopProgression
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkshopAnalyticsTest {
    private val dispatcher = StandardTestDispatcher()

    private class RecordingAnalytics : Analytics {
        val events = mutableListOf<Pair<String, Map<String, Any?>>>()
        override fun logEvent(name: String, params: Map<String, Any?>) {
            events += name to params
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `successful persisted upgrade emits workshop and economy events`() = runTest(dispatcher) {
        val cfg = ProgressionConfig()
        val cost = WorkshopProgression.mechanismUpgradeCost(0, cfg)!!
        val remaining = 100
        val repo = FakeDataRepo(initialProgress = PlayerProgress(workshopParts = cost + remaining))
        val analytics = RecordingAnalytics()
        val vm = WorkshopViewModel(repo = repo, cfg = cfg, analytics = analytics)

        vm.upgradeMechanism(WorkshopMechanism.CORE)
        advanceUntilIdle()

        assertEquals(1, repo.currentProgress.workshopCoreStage)
        val upgradeEvents = analytics.events.filter { it.first == AnalyticsEvents.WORKSHOP_UPGRADE }
        assertEquals(1, upgradeEvents.size)
        val upgradeParams = upgradeEvents.single().second
        assertEquals("CORE", upgradeParams["mechanism"])
        assertEquals(0, upgradeParams["from_stage"])
        assertEquals(1, upgradeParams["to_stage"])
        assertEquals(cost, upgradeParams["parts_spent"])

        val economyEvents = analytics.events.filter { it.first == AnalyticsEvents.RESOURCE_SPENT }
        assertEquals(1, economyEvents.size)
        val economyParams = economyEvents.single().second
        assertEquals("workshop_parts", economyParams["resource_type"])
        assertEquals("workshop_upgrade", economyParams["source"])
        assertEquals(cost, economyParams["amount"])
        assertEquals(remaining, economyParams["balance_after"])
    }

    @Test
    fun `rejected upgrade does not emit workshop or economy event`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialProgress = PlayerProgress(workshopParts = 0))
        val analytics = RecordingAnalytics()
        val vm = WorkshopViewModel(repo = repo, analytics = analytics)

        vm.upgradeMechanism(WorkshopMechanism.CORE)
        advanceUntilIdle()

        assertEquals(0, repo.currentProgress.workshopCoreStage)
        assertTrue(analytics.events.none { it.first == AnalyticsEvents.WORKSHOP_UPGRADE })
        assertTrue(analytics.events.none { it.first == AnalyticsEvents.RESOURCE_SPENT })
    }
}
