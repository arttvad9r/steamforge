package com.steamforge.game.ui.home

import com.steamforge.game.config.LocalDefaultConfig
import com.steamforge.game.config.MutableGameConfigProvider
import com.steamforge.game.config.RemoteGameConfig
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.EventMetric
import com.steamforge.game.progression.EventMilestone
import com.steamforge.game.progression.EventReward
import com.steamforge.game.progression.EventScoringRule
import com.steamforge.game.progression.EventTheme
import com.steamforge.game.progression.LiveOpsLedger
import com.steamforge.game.progression.PlayerProgress
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `home event card follows scheduled remote event`() = runTest(dispatcher) {
        val day = 25_000L
        val event = LocalDefaultConfig.foundryTemplate.instantiateForEpochDay(day).copy(
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
        val config = MutableGameConfigProvider(RemoteGameConfig(scheduledEvents = listOf(event)))
        val repo = FakeDataRepo(
            initialProgress = PlayerProgress(
                liveOps = LiveOpsLedger(eventId = event.id, totalPoints = 12),
            ),
        )
        val model = HomeViewModel(repo = repo, configProvider = config, today = { day })
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.ui.collect { } }
        advanceUntilIdle()

        assertEquals(event.theme, model.ui.value.eventTheme)
        assertEquals(12, model.ui.value.eventPoints)
        assertEquals(4, model.ui.value.eventDaysRemaining)
        assertTrue(model.ui.value.eventRewardAvailable)
        assertTrue(model.ui.value.eventEnabled)
        collector.cancel()
    }
}
