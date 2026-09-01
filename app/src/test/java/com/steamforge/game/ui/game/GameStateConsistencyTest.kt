package com.steamforge.game.ui.game

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.core.GameState
import com.steamforge.game.core.Move
import com.steamforge.game.core.Tile
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.data.SavedGame
import com.steamforge.game.progression.PlayerProgress
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameStateConsistencyTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private object NoopAnalytics : Analytics {
        override fun logEvent(name: String, params: Map<String, Any?>) = Unit
    }

    private fun vm(repo: FakeDataRepo, seed: Long = 42L) = GameViewModel(
        repo = repo,
        analytics = NoopAnalytics,
        seedProvider = { seed },
        savedGameProvider = { repo.currentGame },
    )

    @Test
    fun `undo rewinds rng so replaying the move produces the same spawn`() = runTest(dispatcher) {
        val repo = FakeDataRepo()
        val model = vm(repo, seed = 99L)
        advanceUntilIdle()

        val before = model.ui.value.state
        var acceptedMove: Move? = null
        var firstResult: GameState? = null
        for (move in listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)) {
            model.onMove(move)
            advanceUntilIdle()
            if (model.ui.value.state != before) {
                acceptedMove = move
                firstResult = model.ui.value.state
                break
            }
        }

        assertNotNull(acceptedMove)
        assertNotNull(firstResult)
        model.undo()
        advanceUntilIdle()
        assertEquals(before, model.ui.value.state)

        model.onMove(acceptedMove!!)
        advanceUntilIdle()
        assertEquals(firstResult, model.ui.value.state)
    }

    @Test
    fun `undo restores merge counters but records the undo itself`() = runTest(dispatcher) {
        val saved = SavedGame(
            state = GameState(
                tiles = listOf(
                    Tile(1, 6, 0, 0),
                    Tile(2, 6, 0, 1),
                    Tile(3, 2, 2, 2),
                ),
                score = 500,
                nextTileId = 4,
                moves = 10,
            ),
            seed = 123L,
            pressure = 20,
            overdriveRemaining = 0,
            freeUndosLeft = 2,
            rngDraws = 7L,
            mergesTotal = 4,
            maxMergesInOneMove = 2,
            overdrivesSession = 1,
            undosSession = 3,
            highMergesSession = 2,
        )
        val repo = FakeDataRepo(initialGame = saved)
        val model = vm(repo, seed = 999L)
        advanceUntilIdle()

        model.onMove(Move.LEFT)
        advanceUntilIdle()
        assertTrue(model.ui.value.mergesTotal > saved.mergesTotal)
        assertTrue(model.ui.value.highMergesSession > saved.highMergesSession)

        model.undo()
        advanceUntilIdle()
        val ui = model.ui.value
        assertEquals(saved.state, ui.state)
        assertEquals(saved.pressure, ui.pressure)
        assertEquals(saved.overdriveRemaining, ui.overdriveRemaining)
        assertEquals(saved.mergesTotal, ui.mergesTotal)
        assertEquals(saved.maxMergesInOneMove, ui.maxMergesInOneMove)
        assertEquals(saved.overdrivesSession, ui.overdrivesSession)
        assertEquals(saved.highMergesSession, ui.highMergesSession)
        assertEquals(saved.undosSession + 1, ui.undosSession)
    }

    @Test
    fun `process recreation restores all session counters`() = runTest(dispatcher) {
        val saved = SavedGame(
            state = GameState(
                tiles = listOf(Tile(1, 1, 0, 0), Tile(2, 2, 1, 1)),
                score = 900,
                nextTileId = 3,
                moves = 25,
            ),
            seed = 777L,
            pressure = 66,
            overdriveRemaining = 2,
            freeUndosLeft = 1,
            rngDraws = 31L,
            mergesTotal = 18,
            maxMergesInOneMove = 3,
            overdrivesSession = 2,
            undosSession = 4,
            highMergesSession = 5,
        )
        val model = vm(FakeDataRepo(initialGame = saved), seed = 1L)
        advanceUntilIdle()

        val ui = model.ui.value
        assertEquals(saved.state, ui.state)
        assertEquals(saved.pressure, ui.pressure)
        assertEquals(saved.overdriveRemaining, ui.overdriveRemaining)
        assertEquals(saved.freeUndosLeft, ui.freeUndosLeft)
        assertEquals(saved.mergesTotal, ui.mergesTotal)
        assertEquals(saved.maxMergesInOneMove, ui.maxMergesInOneMove)
        assertEquals(saved.overdrivesSession, ui.overdrivesSession)
        assertEquals(saved.undosSession, ui.undosSession)
        assertEquals(saved.highMergesSession, ui.highMergesSession)
    }

    @Test
    fun `wrench mode requires enough gems`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialProgress = PlayerProgress(gems = 9))
        val model = vm(repo)
        advanceUntilIdle()

        model.toggleRemovingMode()

        assertFalse(model.ui.value.removingMode)
        assertEquals(9, model.ui.value.gems)
    }

    @Test
    fun `wrench ignores stale tile clicks and charges exactly once`() = runTest(dispatcher) {
        val saved = SavedGame(
            state = GameState(
                tiles = listOf(
                    Tile(1, 1, 0, 0),
                    Tile(2, 2, 1, 1),
                ),
                nextTileId = 3,
            ),
            seed = 123L,
            pressure = 0,
            overdriveRemaining = 0,
            freeUndosLeft = 0,
        )
        val repo = FakeDataRepo(
            initialProgress = PlayerProgress(gems = 20),
            initialGame = saved,
        )
        val model = vm(repo)
        advanceUntilIdle()
        val first = saved.state.tiles[0]
        val second = saved.state.tiles[1]

        model.removeTile(first)
        advanceUntilIdle()
        assertEquals(saved.state, model.ui.value.state)
        assertEquals(20, repo.currentProgress.gems)

        model.toggleRemovingMode()
        assertTrue(model.ui.value.removingMode)
        model.removeTile(first)
        model.removeTile(second)
        assertFalse(model.ui.value.removingMode)
        assertEquals(1, model.ui.value.state.tiles.size)
        assertEquals(10, model.ui.value.gems)

        advanceUntilIdle()
        assertEquals(1, model.ui.value.state.tiles.size)
        assertEquals(10, repo.currentProgress.gems)
    }
}
