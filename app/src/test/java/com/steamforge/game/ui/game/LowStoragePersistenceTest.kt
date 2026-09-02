package com.steamforge.game.ui.game

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.core.GameState
import com.steamforge.game.core.Move
import com.steamforge.game.core.Tile
import com.steamforge.game.data.DataRepo
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.data.FinishedGameRecord
import com.steamforge.game.data.SavedGame
import com.steamforge.game.progression.FinishEffects
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LowStoragePersistenceTest {

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

    private class FlakySaveRepo(
        private val delegate: FakeDataRepo = FakeDataRepo(),
    ) : DataRepo by delegate {
        var remainingIoFailures: Int = 0
        var remainingFinishIoFailures: Int = 0
        val finishAttemptIds = mutableListOf<String>()

        val currentGame: SavedGame?
            get() = delegate.currentGame
        val currentFinished: FinishedGameRecord?
            get() = delegate.currentFinished
        val currentProgress
            get() = delegate.currentProgress

        override suspend fun saveGame(state: SavedGame) {
            if (remainingIoFailures > 0) {
                remainingIoFailures--
                throw IOException("ENOSPC")
            }
            delegate.saveGame(state)
        }

        override suspend fun applyGameFinish(
            record: FinishedGameRecord,
            finisher: (com.steamforge.game.progression.PlayerProgress) -> Pair<com.steamforge.game.progression.PlayerProgress, FinishEffects>,
        ) {
            finishAttemptIds += record.id
            if (remainingFinishIoFailures > 0) {
                remainingFinishIoFailures--
                throw IOException("ENOSPC")
            }
            delegate.applyGameFinish(record, finisher)
        }
    }

    @Test
    fun `transient save io failure keeps run alive and next autosave recovers`() = runTest(dispatcher) {
        val repo = FlakySaveRepo()
        val analytics = RecordingAnalytics()
        val model = GameViewModel(
            repo = repo,
            analytics = analytics,
            seedProvider = { 73L },
            savedGameProvider = { repo.currentGame },
        )
        advanceUntilIdle()

        val durableBeforeFailure = requireNotNull(repo.currentGame).state
        repo.remainingIoFailures = 1

        val failedSaveState = performOneValidMove(model)
        advanceUntilIdle()

        assertEquals(failedSaveState, model.ui.value.state)
        assertEquals(durableBeforeFailure, requireNotNull(repo.currentGame).state)
        assertTrue("I/O save failure was not surfaced to analytics", "run_save_failed" in analytics.names)

        val recoveredState = performOneValidMove(model)
        advanceUntilIdle()

        assertNotEquals(failedSaveState, recoveredState)
        assertEquals(recoveredState, requireNotNull(repo.currentGame).state)
        assertTrue("save recovery was not surfaced to analytics", "run_save_recovered" in analytics.names)
    }

    @Test
    fun `terminal io failure retries same result and applies finish exactly once`() = runTest(dispatcher) {
        val initialGame = finishingSavedGame(seed = 17L)
        val delegate = FakeDataRepo(initialGame = initialGame)
        val repo = FlakySaveRepo(delegate)
        val analytics = RecordingAnalytics()
        val model = GameViewModel(
            repo = repo,
            analytics = analytics,
            seedProvider = { 17L },
            savedGameProvider = { repo.currentGame },
        )
        advanceUntilIdle()
        val progressBeforeFinish = repo.currentProgress

        repo.remainingFinishIoFailures = 1
        model.onMove(Move.LEFT)
        advanceUntilIdle()

        assertFalse(model.ui.value.finished)
        assertTrue(model.ui.value.finishPersistenceFailed)
        assertFalse(model.ui.value.finishPersistenceRetrying)
        assertEquals(progressBeforeFinish, repo.currentProgress)
        assertNull(repo.currentFinished)
        assertEquals(1, repo.finishAttemptIds.size)
        val resultId = repo.finishAttemptIds.single()
        assertEquals(1, analytics.names.count { it == "game_finish_save_failed" })
        assertEquals(0, analytics.names.count { it == "game_finished" })

        model.retryFinishPersistence()
        assertTrue(model.ui.value.finishPersistenceRetrying)
        advanceUntilIdle()

        assertTrue(model.ui.value.finished)
        assertFalse(model.ui.value.finishPersistenceFailed)
        assertFalse(model.ui.value.finishPersistenceRetrying)
        assertEquals(listOf(resultId, resultId), repo.finishAttemptIds)
        assertNotNull(repo.currentFinished)
        assertEquals(resultId, repo.currentFinished?.id)
        assertEquals(progressBeforeFinish.stats.gamesPlayed + 1, repo.currentProgress.stats.gamesPlayed)
        assertEquals(1, analytics.names.count { it == "game_finish_save_failed" })
        assertEquals(1, analytics.names.count { it == "game_finish_save_retry" })
        assertEquals(1, analytics.names.count { it == "game_finish_save_recovered" })
        assertEquals(1, analytics.names.count { it == "game_finished" })
    }

    private fun finishingSavedGame(seed: Long): SavedGame {
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

    private fun performOneValidMove(model: GameViewModel): GameState {
        for (move in listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)) {
            val before = model.ui.value.state
            model.onMove(move)
            if (model.ui.value.state != before) return model.ui.value.state
        }
        error("fixture did not provide a valid move")
    }
}
