package com.steamforge.game.ui.game

import com.steamforge.game.core.GameState
import com.steamforge.game.core.Tile
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.data.SavedGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestartWinCelebrationTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `restart clears win celebration inherited from restored run`() = runTest(dispatcher) {
        val wonRun = SavedGame(
            state = GameState(
                tiles = listOf(
                    Tile(id = 1L, level = 11, row = 0, col = 0),
                    Tile(id = 2L, level = 1, row = 1, col = 1),
                ),
                nextTileId = 3L,
                won = true,
                moves = 120,
            ),
            seed = 17L,
            pressure = 42,
            overdriveRemaining = 0,
            freeUndosLeft = 1,
        )
        val repo = FakeDataRepo(initialGame = wonRun)
        val model = GameViewModel(
            repo = repo,
            seedProvider = { 99L },
            savedGameProvider = { repo.currentGame },
            systemAnimationsEnabled = true,
        )
        advanceUntilIdle()

        assertTrue(model.ui.value.winCelebrated)
        model.markWinBannerShown()
        assertTrue(model.ui.value.winBannerShown)

        model.restart()
        advanceUntilIdle()

        assertFalse(model.ui.value.state.won)
        assertFalse(model.ui.value.winCelebrated)
        assertFalse(model.ui.value.winBannerShown)
    }
}
