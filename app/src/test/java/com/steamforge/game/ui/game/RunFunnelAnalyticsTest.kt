package com.steamforge.game.ui.game

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.analytics.AnalyticsEvents
import com.steamforge.game.core.GameState
import com.steamforge.game.core.Move
import com.steamforge.game.core.Tile
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.data.SavedGame
import com.steamforge.game.progression.DailyChallenge
import com.steamforge.game.progression.DailyGoalType
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RunFunnelAnalyticsTest {
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
    fun `fresh normal run emits one canonical game start and persists its run id`() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val repo = FakeDataRepo(initialGame = null)
        GameViewModel(
            repo = repo,
            analytics = analytics,
            seedProvider = { 41L },
            savedGameProvider = { null },
        )
        advanceUntilIdle()

        val starts = analytics.events.filter { it.first == AnalyticsEvents.GAME_STARTED }
        assertEquals(1, starts.size)
        val runId = starts.single().second["run_id"] as String
        assertTrue(runId.startsWith("normal-"))
        assertEquals(false, starts.single().second["daily"])
        assertFalse(starts.single().second.containsKey("daily_type"))
        assertEquals(0, analytics.events.count { it.first == "daily_started" })
        assertEquals(runId, repo.currentGame?.analyticsRunId)
    }

    @Test
    fun `same replay seed still produces distinct normal run ids`() = runTest(dispatcher) {
        suspend fun startId(): String {
            val analytics = RecordingAnalytics()
            GameViewModel(
                repo = FakeDataRepo(initialGame = null),
                analytics = analytics,
                seedProvider = { 41L },
                savedGameProvider = { null },
            )
            advanceUntilIdle()
            return analytics.events
                .single { it.first == AnalyticsEvents.GAME_STARTED }
                .second["run_id"] as String
        }

        assertNotEquals(startId(), startId())
    }

    @Test
    fun `daily run joins canonical funnel and preserves legacy start`() = runTest(dispatcher) {
        val challenge = DailyChallenge(
            epochDay = 12_345L,
            type = DailyGoalType.REACH_SCORE,
            target = 1_000,
            mergeLevel = 6,
            seed = 73L,
            rewardGems = 15,
            bonusXp = 60,
        )
        val analytics = RecordingAnalytics()
        GameViewModel(
            repo = FakeDataRepo(initialGame = null),
            analytics = analytics,
            dailyMode = true,
            dailyProvider = { challenge },
            seedProvider = { 999L },
            savedGameProvider = { null },
        )
        advanceUntilIdle()

        val starts = analytics.events.filter { it.first == AnalyticsEvents.GAME_STARTED }
        assertEquals(1, starts.size)
        assertTrue((starts.single().second["run_id"] as String).startsWith("daily-"))
        assertEquals(true, starts.single().second["daily"])
        assertEquals(challenge.type.name, starts.single().second["daily_type"])

        val legacy = analytics.events.filter { it.first == "daily_started" }
        assertEquals(1, legacy.size)
        assertEquals(challenge.type.name, legacy.single().second["daily_type"])
    }

    @Test
    fun `persisted normal run id correlates start and finish across process restore`() = runTest(dispatcher) {
        val seed = 17L
        val startAnalytics = RecordingAnalytics()
        val startRepo = FakeDataRepo(initialGame = null)
        GameViewModel(
            repo = startRepo,
            analytics = startAnalytics,
            seedProvider = { seed },
            savedGameProvider = { null },
        )
        advanceUntilIdle()
        val startId = startAnalytics.events
            .single { it.first == AnalyticsEvents.GAME_STARTED }
            .second["run_id"] as String
        val persistedId = requireNotNull(startRepo.currentGame?.analyticsRunId)
        assertEquals(startId, persistedId)

        val saved = finishingSavedGame(seed, analyticsRunId = persistedId)
        val finishAnalytics = RecordingAnalytics()
        val restored = GameViewModel(
            repo = FakeDataRepo(initialGame = saved),
            analytics = finishAnalytics,
            seedProvider = { 999L },
            savedGameProvider = { saved },
        )
        advanceUntilIdle()
        assertEquals(0, finishAnalytics.events.count { it.first == AnalyticsEvents.GAME_STARTED })

        restored.onMove(Move.LEFT)
        advanceUntilIdle()

        assertTrue(restored.ui.value.finished)
        val finishId = finishAnalytics.events
            .single { it.first == AnalyticsEvents.GAME_FINISHED }
            .second["run_id"]
        assertEquals(startId, finishId)
    }

    private fun finishingSavedGame(seed: Long, analyticsRunId: String? = null): SavedGame {
        val levels = listOf(
            1, 1, 3, 4,
            5, 6, 7, 8,
            9, 10, 1, 2,
            3, 4, 5, 6,
        )
        val tiles = levels.mapIndexed { index, level ->
            Tile(
                id = (index + 1).toLong(),
                level = level,
                row = index / 4,
                col = index % 4,
            )
        }
        return SavedGame(
            state = GameState(
                size = 4,
                tiles = tiles,
                score = 128,
                nextTileId = 17L,
                moves = 20,
            ),
            seed = seed,
            pressure = 0,
            overdriveRemaining = 0,
            freeUndosLeft = 2,
            rngDraws = 0L,
            analyticsRunId = analyticsRunId,
        )
    }
}
