package com.steamforge.game.ui.event

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.config.LocalDefaultConfig
import com.steamforge.game.config.MutableGameConfigProvider
import com.steamforge.game.config.RemoteGameConfig
import com.steamforge.game.data.DataRepo
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.EventDefinition
import com.steamforge.game.progression.EventMetric
import com.steamforge.game.progression.EventMilestone
import com.steamforge.game.progression.EventReward
import com.steamforge.game.progression.EventScoringRule
import com.steamforge.game.progression.EventTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class RecordingAnalytics : Analytics {
        val events = mutableListOf<Pair<String, Map<String, Any?>>>()

        override fun logEvent(name: String, params: Map<String, Any?>) {
            events += name to params
        }
    }

    private class ClaimingRepo(
        private val delegate: FakeDataRepo = FakeDataRepo(),
        var claimResult: Boolean = true,
    ) : DataRepo by delegate {
        override suspend fun claimEventMilestone(event: EventDefinition, milestoneId: String): Boolean = claimResult
    }

    private fun maintenanceEvent(day: Long) = LocalDefaultConfig.foundryTemplate.instantiateForEpochDay(day).copy(
        id = "maintenance-week",
        startEpochDay = day,
        endEpochDayExclusive = day + 4,
        scoringRule = EventScoringRule(EventMetric.SCORE, pointsPerUnit = 1, unitsPerStep = 100),
        milestones = listOf(
            EventMilestone("calibration-10", 10, EventReward(gems = 3)),
            EventMilestone("calibration-25", 25, EventReward(gems = 7)),
        ),
        theme = EventTheme(
            id = "maintenance",
            title = "MAINTENANCE WEEK",
            subtitle = "Калибруйте механизмы цеха",
            accent = "patina-teal",
        ),
    )

    @Test
    fun `opening reward track uses scheduled event and logs its context`() = runTest(dispatcher) {
        val day = 25_000L
        val analytics = RecordingAnalytics()
        val event = maintenanceEvent(day)
        val config = MutableGameConfigProvider(
            RemoteGameConfig(scheduledEvents = listOf(event)),
        )
        val model = EventViewModel(
            repo = ClaimingRepo(),
            configProvider = config,
            today = { day },
            analytics = analytics,
        )
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.ui.collect { } }
        advanceUntilIdle()

        assertEquals(event.id, model.ui.value.event.id)
        assertEquals(event.theme.id, model.ui.value.event.theme.id)
        assertEquals(event.scoringRule, model.ui.value.event.scoringRule)
        assertEquals(event.milestones.map { it.id }, model.ui.value.event.milestones.map { it.id })

        val entered = analytics.events.single { it.first == "event_entered" }.second
        assertEquals(event.id, entered["event_id"])
        assertEquals(event.theme.id, entered["theme_id"])
        assertEquals("reward_track", entered["surface"])
        assertEquals(event.milestones.size, entered["track_levels"])
        collector.cancel()
    }

    @Test
    fun `milestone telemetry is emitted only after successful atomic claim`() = runTest(dispatcher) {
        val day = 25_000L
        val analytics = RecordingAnalytics()
        val repo = ClaimingRepo(claimResult = true)
        val model = EventViewModel(repo = repo, today = { day }, analytics = analytics)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.ui.collect { } }
        advanceUntilIdle()
        val milestone = model.ui.value.event.milestones.first()

        model.claim(milestone.id)
        advanceUntilIdle()

        val claimed = analytics.events.single { it.first == "event_milestone" }.second
        assertEquals(milestone.id, claimed["milestone_id"])
        assertEquals(milestone.targetPoints, claimed["target_points"])
        assertEquals(milestone.reward.gems, claimed["reward_gems"])

        analytics.events.clear()
        repo.claimResult = false
        model.claim(milestone.id)
        advanceUntilIdle()

        assertFalse(analytics.events.any { it.first == "event_milestone" })
        assertTrue(model.ui.value.event.id.isNotBlank())
        collector.cancel()
    }
}
