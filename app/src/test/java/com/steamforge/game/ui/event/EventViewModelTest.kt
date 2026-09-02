package com.steamforge.game.ui.event

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.data.DataRepo
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.EventDefinition
import com.steamforge.game.progression.LiveOpsCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

    @Test
    fun `opening reward track logs event entered context`() = runTest(dispatcher) {
        val day = 25_000L
        val analytics = RecordingAnalytics()
        val event = LiveOpsCatalog.activeForEpochDay(day)

        EventViewModel(
            repo = ClaimingRepo(),
            today = { day },
            analytics = analytics,
        )

        val entered = analytics.events.single { it.first == "event_entered" }.second
        assertEquals(event.id, entered["event_id"])
        assertEquals(event.theme.id, entered["theme_id"])
        assertEquals("reward_track", entered["surface"])
        assertEquals(event.milestones.size, entered["track_levels"])
    }

    @Test
    fun `milestone telemetry is emitted only after successful atomic claim`() = runTest(dispatcher) {
        val day = 25_000L
        val analytics = RecordingAnalytics()
        val repo = ClaimingRepo(claimResult = true)
        val model = EventViewModel(repo = repo, today = { day }, analytics = analytics)
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
    }
}
