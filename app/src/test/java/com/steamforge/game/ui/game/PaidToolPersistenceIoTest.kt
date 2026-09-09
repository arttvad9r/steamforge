package com.steamforge.game.ui.game

import com.steamforge.game.core.GameState
import com.steamforge.game.core.Move
import com.steamforge.game.data.DataRepo
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.data.SavedGame
import com.steamforge.game.progression.PlayerProgress
import com.steamforge.game.progression.ProgressionConfig
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
class PaidToolPersistenceIoTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FailingPaidToolRepo(
        val delegate: FakeDataRepo,
    ) : DataRepo by delegate {
        var spendAttempts: Int = 0
            private set

        val currentGame: SavedGame?
            get() = delegate.currentGame

        override suspend fun applyPaidTool(
            operationId: String,
            expectedGems: Int,
            gemCost: Int,
            activeGame: SavedGame?,
        ): Boolean {
            spendAttempts += 1
            throw IOException("ENOSPC paid tool")
        }
    }

    private class AmbiguousPaidToolRepo(
        val delegate: FakeDataRepo,
    ) : DataRepo by delegate {
        var spendAttempts: Int = 0
            private set

        val currentGame: SavedGame?
            get() = delegate.currentGame

        override suspend fun applyPaidTool(
            operationId: String,
            expectedGems: Int,
            gemCost: Int,
            activeGame: SavedGame?,
        ): Boolean {
            spendAttempts += 1
            val applied = delegate.applyPaidTool(operationId, expectedGems, gemCost, activeGame)
            if (spendAttempts == 1) throw IOException("ENOSPC after paid tool commit")
            return applied
        }
    }

    @Test
    fun `wrench does not become a free durable action when gem debit fails`() = runTest(dispatcher) {
        val repo = FailingPaidToolRepo(FakeDataRepo(PlayerProgress(gems = 50)))
        val model = GameViewModel(
            repo = repo,
            seedProvider = { 7L },
            savedGameProvider = { repo.currentGame },
        )
        advanceUntilIdle()
        repeat(10) { index ->
            model.onMove(listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)[index % 4])
            advanceUntilIdle()
        }
        val target = model.ui.value.state.tiles.first { it.level <= 4 }
        val beforeUi = model.ui.value.state
        val durableBefore = requireNotNull(repo.currentGame).state

        model.toggleRemovingMode()
        model.removeTile(target)
        advanceUntilIdle()

        assertEquals(2, repo.spendAttempts)
        assertEquals(50, repo.delegate.currentProgress.gems)
        assertEquals(beforeUi, model.ui.value.state)
        assertEquals(durableBefore, requireNotNull(repo.currentGame).state)
    }

    @Test
    fun `ambiguous wrench commit retries without double spending`() = runTest(dispatcher) {
        val repo = AmbiguousPaidToolRepo(FakeDataRepo(PlayerProgress(gems = 50)))
        val model = GameViewModel(
            repo = repo,
            seedProvider = { 7L },
            savedGameProvider = { repo.currentGame },
        )
        advanceUntilIdle()
        repeat(10) { index ->
            model.onMove(listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)[index % 4])
            advanceUntilIdle()
        }
        val target = model.ui.value.state.tiles.first { it.level <= 4 }
        val beforeCount = model.ui.value.state.tiles.size

        model.toggleRemovingMode()
        model.removeTile(target)
        advanceUntilIdle()

        assertEquals(2, repo.spendAttempts)
        assertEquals(40, repo.delegate.currentProgress.gems)
        assertEquals(beforeCount - 1, model.ui.value.state.tiles.size)
        assertEquals(model.ui.value.state, requireNotNull(repo.currentGame).state)
    }

    @Test
    fun `paid undo does not rewind when atomic spend fails`() = runTest(dispatcher) {
        val repo = FailingPaidToolRepo(FakeDataRepo(PlayerProgress(gems = 50)))
        val model = GameViewModel(
            repo = repo,
            cfg = ProgressionConfig(freeUndosPerGame = 0),
            seedProvider = { 73L },
            savedGameProvider = { repo.currentGame },
        )
        advanceUntilIdle()
        performOneValidMove(model)
        advanceUntilIdle()
        assertTrue(model.ui.value.canUndo)
        val beforeUi = model.ui.value.state
        val durableBefore = requireNotNull(repo.currentGame).state

        model.undo()
        advanceUntilIdle()

        assertEquals(2, repo.spendAttempts)
        assertEquals(50, repo.delegate.currentProgress.gems)
        assertEquals(beforeUi, model.ui.value.state)
        assertEquals(durableBefore, requireNotNull(repo.currentGame).state)
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
