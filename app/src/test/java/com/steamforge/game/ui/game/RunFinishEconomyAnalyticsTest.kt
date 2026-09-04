package com.steamforge.game.ui.game

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.analytics.AnalyticsEvents
import com.steamforge.game.core.GameState
import com.steamforge.game.core.GameStatus
import com.steamforge.game.core.Move
import com.steamforge.game.core.Tile
import com.steamforge.game.data.DataRepo
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.data.FinishedGameRecord
import com.steamforge.game.data.SavedGame
import com.steamforge.game.progression.FinishEffects
import com.steamforge.game.progression.PlayerProgress
import java.io.IOException
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
class RunFinishEconomyAnalyticsTest {
    private val dispatcher = StandardTestDispatcher()

    private class RecordingAnalytics : Analytics {
        val events = mutableListOf<Pair<String, Map<String, Any?>>>()

        override fun logEvent(name: String, params: Map<String, Any?>) {
            events += name to params
        }
    }

    private class FlakyFinishRepo(
        private val delegate: FakeDataRepo,
    ) : DataRepo by delegate {
        var remainingFinishFailures = 1
        var commitBeforeFailure = false
        var finishAttempts = 0

        val currentFinished: FinishedGameRecord?
            get() = delegate.currentFinished

        val currentProgress: PlayerProgress
            get() = delegate.currentProgress

        override suspend fun applyGameFinish(
            record: FinishedGameRecord,
            finisher: (PlayerProgress) -> Pair<PlayerProgress, FinishEffects>,
        ) {
            finishAttempts++
            if (remainingFinishFailures > 0) {
                remainingFinishFailures--
                if (commitBeforeFailure) delegate.applyGameFinish(record, finisher)
                throw IOException("ENOSPC")
            }
            delegate.applyGameFinish(record, finisher)
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `successful finish emits persisted workshop parts economy event`() = runTest(dispatcher) {
        val initial = finishingSavedGame()
        val repo = FakeDataRepo(
            initialProgress = PlayerProgress(workshopParts = 11),
            initialGame = initial,
        )
        val analytics = RecordingAnalytics()
        val model = GameViewModel(
            repo = repo,
            analytics = analytics,
            seedProvider = { 17L },
            savedGameProvider = { initial },
        )
        advanceUntilIdle()

        model.onMove(Move.LEFT)
        advanceUntilIdle()

        assertTrue(model.ui.value.finished)
        val effects = requireNotNull(model.ui.value.effects)
        assertTrue(effects.workshopPartsGained > 0)
        val events = analytics.events.filter { it.first == AnalyticsEvents.RESOURCE_EARNED }
        assertEquals(1, events.size)
        val params = events.single().second
        assertEquals("workshop_parts", params["resource_type"])
        assertEquals("game_finish", params["source"])
        assertEquals(effects.workshopPartsGained, params["amount"])
        assertEquals(repo.currentProgress.workshopParts, params["balance_after"])
    }

    @Test
    fun `ambiguous durable finish logs workshop parts once after retry`() = runTest(dispatcher) {
        val initial = finishingSavedGame()
        val repo = FlakyFinishRepo(
            FakeDataRepo(
                initialProgress = PlayerProgress(workshopParts = 13),
                initialGame = initial,
            ),
        ).apply { commitBeforeFailure = true }
        val analytics = RecordingAnalytics()
        val model = GameViewModel(
            repo = repo,
            analytics = analytics,
            seedProvider = { 17L },
            savedGameProvider = { initial },
        )
        advanceUntilIdle()

        model.onMove(Move.LEFT)
        advanceUntilIdle()

        assertTrue(model.ui.value.finishPersistenceFailed)
        val durable = requireNotNull(repo.currentFinished)
        assertTrue(durable.workshopPartsGained > 0)
        assertEquals(0, analytics.events.count { it.first == AnalyticsEvents.RESOURCE_EARNED })
        assertEquals(1, repo.finishAttempts)

        model.retryFinishPersistence()
        advanceUntilIdle()

        assertTrue(model.ui.value.finished)
        assertEquals(2, repo.finishAttempts)
        val events = analytics.events.filter { it.first == AnalyticsEvents.RESOURCE_EARNED }
        assertEquals(1, events.size)
        val params = events.single().second
        assertEquals("workshop_parts", params["resource_type"])
        assertEquals("game_finish", params["source"])
        assertEquals(durable.workshopPartsGained, params["amount"])
        assertEquals(repo.currentProgress.workshopParts, params["balance_after"])

        model.retryFinishPersistence()
        advanceUntilIdle()
        assertEquals(1, analytics.events.count { it.first == AnalyticsEvents.RESOURCE_EARNED })
        assertEquals(2, repo.finishAttempts)
    }

    private fun finishingSavedGame(seed: Long = 17L): SavedGame {
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
                status = GameStatus.PLAYING,
            ),
            seed = seed,
            pressure = 0,
            overdriveRemaining = 0,
            freeUndosLeft = 2,
            rngDraws = 0L,
        )
    }
}
