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
    fun `successful persisted upgrade emits exactly one workshop event`() = runTest(dispatcher) {
        val cfg = ProgressionConfig()
        val cost = WorkshopProgression.mechanismUpgradeCost(0, cfg)!!
        val repo = FakeDataRepo(initialProgress = PlayerProgress(workshopParts = cost + 100))
        val analytics = RecordingAnalytics()
        val vm = WorkshopViewModel(repo = repo, cfg = cfg, analytics = analytics)

        vm.upgradeMechanism(WorkshopMechanism.CORE)
        advanceUntilIdle()

        assertEquals(1, repo.currentProgress.workshopCoreStage)
        val events = analytics.events.filter { it.first == AnalyticsEvents.WORKSHOP_UPGRADE }
        assertEquals(1, events.size)
        val params = events.single().second
        assertEquals("CORE", params["mechanism"])
        assertEquals(0, params["from_stage"])
        assertEquals(1, params["to_stage"])
        assertEquals(cost, params["parts_spent"])
    }

    @Test
    fun `rejected upgrade does not emit workshop event`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialProgress = PlayerProgress(workshopParts = 0))
        val analytics = RecordingAnalytics()
        val vm = WorkshopViewModel(repo = repo, analytics = analytics)

        vm.upgradeMechanism(WorkshopMechanism.CORE)
        advanceUntilIdle()

        assertEquals(0, repo.currentProgress.workshopCoreStage)
        assertTrue(analytics.events.none { it.first == AnalyticsEvents.WORKSHOP_UPGRADE })
    }
}
