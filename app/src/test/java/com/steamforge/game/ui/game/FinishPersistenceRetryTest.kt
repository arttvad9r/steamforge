package com.steamforge.game.ui.game

import com.steamforge.game.analytics.Analytics
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FinishPersistenceRetryTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class RecordingAnalytics : Analytics {
        val names = mutableListOf<String>()
        override fun logEvent(name: String, params: Map<String, Any?>) {
            names += name
        }
    }

    private class FlakyFinishRepo(
        private val delegate: FakeDataRepo,
    ) : DataRepo by delegate {
        var remainingFinishFailures = 1
        var commitBeforeFailure = false
        var finishAttempts = 0
        val attemptedIds = mutableListOf<String>()

        val currentFinished: FinishedGameRecord?
            get() = delegate.currentFinished

        val currentProgress: PlayerProgress
            get() = delegate.currentProgress

        override suspend fun applyGameFinish(
            record: FinishedGameRecord,
            finisher: (PlayerProgress) -> Pair<PlayerProgress, FinishEffects>,
        ) {
            finishAttempts++
            attemptedIds += record.id
            if (remainingFinishFailures > 0) {
                remainingFinishFailures--
                if (commitBeforeFailure) delegate.applyGameFinish(record, finisher)
                throw IOException("ENOSPC")
            }
            delegate.applyGameFinish(record, finisher)
        }
    }

    @Test
    fun `game over io failure keeps final board stable and retry commits exactly once`() = runTest(dispatcher) {
        val initial = finishingSavedGame()
        val repo = FlakyFinishRepo(FakeDataRepo(initialGame = initial))
        val analytics = RecordingAnalytics()
        val model = GameViewModel(
            repo = repo,
            analytics = analytics,
            seedProvider = { 17L },
            savedGameProvider = { initial },
        )
        advanceUntilIdle()

        model.onMove(Move.LEFT)
        assertTrue(model.ui.value.finishPersistenceInProgress)
        model.exit()
        advanceUntilIdle()

        assertEquals(GameStatus.GAME_OVER, model.ui.value.state.status)
        assertFalse(model.ui.value.finished)
        assertTrue(model.ui.value.finishPersistenceFailed)
        assertFalse(model.ui.value.finishPersistenceInProgress)
        assertNull(repo.currentFinished)
        assertEquals(0, repo.currentProgress.stats.gamesPlayed)
        assertEquals(1, repo.finishAttempts)
        assertTrue("terminal I/O failure was not surfaced", "game_finish_save_failed" in analytics.names)

        val finalState = model.ui.value.state
        model.retryFinishPersistence()
        advanceUntilIdle()

        assertEquals(finalState, model.ui.value.state)
        assertTrue(model.ui.value.finished)
        assertFalse(model.ui.value.finishPersistenceFailed)
        assertFalse(model.ui.value.finishPersistenceInProgress)
        assertEquals(2, repo.finishAttempts)
        assertEquals(1, repo.attemptedIds.toSet().size)
        assertNotNull(repo.currentFinished)
        assertEquals(repo.attemptedIds.first(), repo.currentFinished?.id)
        assertEquals(1, repo.currentProgress.stats.gamesPlayed)
        assertEquals(1, analytics.names.count { it == "game_finished" })
        assertEquals(1, analytics.names.count { it == "game_finish_save_failed" })
        assertEquals(1, analytics.names.count { it == "game_finish_save_retry" })
        assertEquals(1, analytics.names.count { it == "game_finish_save_recovered" })

        model.retryFinishPersistence()
        advanceUntilIdle()
        assertEquals(2, repo.finishAttempts)
        assertEquals(1, repo.currentProgress.stats.gamesPlayed)
    }

    @Test
    fun `ambiguous io after durable commit retries idempotently`() = runTest(dispatcher) {
        val initial = finishingSavedGame()
        val repo = FlakyFinishRepo(FakeDataRepo(initialGame = initial)).apply { commitBeforeFailure = true }
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
        assertFalse(model.ui.value.finished)
        assertEquals(1, repo.currentProgress.stats.gamesPlayed)
        val durable = requireNotNull(repo.currentFinished)
        assertEquals(1, repo.finishAttempts)

        model.retryFinishPersistence()
        advanceUntilIdle()

        assertTrue(model.ui.value.finished)
        assertFalse(model.ui.value.finishPersistenceFailed)
        assertEquals(2, repo.finishAttempts)
        assertEquals(1, repo.attemptedIds.toSet().size)
        assertEquals(durable.id, repo.currentFinished?.id)
        assertEquals(1, repo.currentProgress.stats.gamesPlayed)
        assertEquals(durable.xpGained, model.ui.value.effects?.xpGained)
        assertEquals(durable.gemsGained, model.ui.value.effects?.gemsGained)
        assertEquals(1, analytics.names.count { it == "game_finished" })
        assertEquals(1, analytics.names.count { it == "game_finish_save_failed" })
        assertEquals(1, analytics.names.count { it == "game_finish_save_retry" })
        assertEquals(1, analytics.names.count { it == "game_finish_save_recovered" })
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
            ),
            seed = seed,
            pressure = 0,
            overdriveRemaining = 0,
            freeUndosLeft = 2,
            rngDraws = 0L,
        )
    }
}
