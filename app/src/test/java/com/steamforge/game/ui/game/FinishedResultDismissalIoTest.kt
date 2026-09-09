package com.steamforge.game.ui.game

import com.steamforge.game.core.GameState
import com.steamforge.game.core.Tile
import com.steamforge.game.data.DataRepo
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.data.FinishedGameRecord
import com.steamforge.game.data.GameSaveCodec
import com.steamforge.game.data.SavedGame
import com.steamforge.game.progression.LocalDay
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
class FinishedResultDismissalIoTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FailingClearRepo(
        private val delegate: FakeDataRepo,
        var remainingClearFailures: Int,
    ) : DataRepo by delegate {
        var clearAttempts = 0

        val currentFinished: FinishedGameRecord?
            get() = delegate.currentFinished

        val currentGame: SavedGame?
            get() = delegate.currentGame

        override suspend fun clearFinishedGame() {
            clearAttempts++
            if (remainingClearFailures > 0) {
                remainingClearFailures--
                throw IOException("ENOSPC clear finished")
            }
            delegate.clearFinishedGame()
        }
    }

    private class AmbiguousClearRepo(
        private val delegate: FakeDataRepo,
    ) : DataRepo by delegate {
        var clearAttempts = 0

        val currentFinished: FinishedGameRecord?
            get() = delegate.currentFinished

        override suspend fun clearFinishedGame() {
            clearAttempts++
            delegate.clearFinishedGame()
            if (clearAttempts == 1) {
                throw IOException("commit completed before acknowledgement")
            }
        }
    }

    @Test
    fun `restart waits for durable dismissal and failed clear remains retryable`() = runTest(dispatcher) {
        val delegate = FakeDataRepo(initialFinished = finishedRecord())
        val repo = FailingClearRepo(delegate, remainingClearFailures = 2)
        val model = GameViewModel(repo = repo, seedProvider = { 99L })
        advanceUntilIdle()

        assertTrue(model.ui.value.finished)
        assertNotNull(repo.currentFinished)

        model.restart()

        // Restart must synchronously keep the persisted result on screen while its clear is pending.
        assertTrue(model.ui.value.finished)
        assertTrue(model.ui.value.finishPersistenceInProgress)

        advanceUntilIdle()

        assertEquals(2, repo.clearAttempts)
        assertTrue(model.ui.value.finished)
        assertFalse(model.ui.value.finishPersistenceInProgress)
        assertTrue(model.ui.value.finishPersistenceFailed)
        assertNotNull(repo.currentFinished)
        assertNull(repo.currentGame)

        repo.remainingClearFailures = 0
        model.retryFinishPersistence()
        advanceUntilIdle()

        assertEquals(3, repo.clearAttempts)
        assertFalse(model.ui.value.finished)
        assertFalse(model.ui.value.finishPersistenceFailed)
        assertNull(repo.currentFinished)
        assertNotNull(repo.currentGame)
    }

    @Test
    fun `exit handoff is emitted only after durable finished-result dismissal`() = runTest(dispatcher) {
        val delegate = FakeDataRepo(initialFinished = finishedRecord())
        val repo = FailingClearRepo(delegate, remainingClearFailures = 1)
        val model = GameViewModel(repo = repo, seedProvider = { 99L })
        advanceUntilIdle()

        model.exit()

        assertTrue(model.ui.value.finished)
        assertTrue(model.ui.value.finishPersistenceInProgress)
        assertFalse(model.ui.value.exitAfterPersistenceReady)
        assertNotNull(repo.currentFinished)

        advanceUntilIdle()

        assertEquals(2, repo.clearAttempts)
        assertNull(repo.currentFinished)
        assertFalse(model.ui.value.finishPersistenceInProgress)
        assertFalse(model.ui.value.finishPersistenceFailed)
        assertTrue(model.ui.value.exitAfterPersistenceReady)

        model.consumeExitAfterPersistenceReady()
        assertFalse(model.ui.value.exitAfterPersistenceReady)
    }

    @Test
    fun `ambiguous committed clear retries idempotently and still emits exit handoff`() = runTest(dispatcher) {
        val delegate = FakeDataRepo(initialFinished = finishedRecord())
        val repo = AmbiguousClearRepo(delegate)
        val model = GameViewModel(repo = repo, seedProvider = { 99L })
        advanceUntilIdle()

        model.exit()
        advanceUntilIdle()

        assertEquals(2, repo.clearAttempts)
        assertNull(repo.currentFinished)
        assertTrue(model.ui.value.finished)
        assertFalse(model.ui.value.finishPersistenceInProgress)
        assertFalse(model.ui.value.finishPersistenceFailed)
        assertTrue(model.ui.value.exitAfterPersistenceReady)
    }

    private fun finishedRecord(): FinishedGameRecord {
        val saved = SavedGame(
            state = GameState(
                tiles = listOf(
                    Tile(1, 1, 0, 0),
                    Tile(2, 2, 1, 1),
                ),
                score = 512,
                nextTileId = 3,
                moves = 12,
            ),
            seed = 42L,
            pressure = 0,
            overdriveRemaining = 0,
            freeUndosLeft = 2,
        )
        return FinishedGameRecord(
            id = "finished-dismissal-proof",
            day = LocalDay.todayEpochDay(),
            daily = false,
            score = saved.state.score,
            maxTileLevel = saved.state.maxLevel,
            state = GameSaveCodec.encode(saved),
        )
    }
}
