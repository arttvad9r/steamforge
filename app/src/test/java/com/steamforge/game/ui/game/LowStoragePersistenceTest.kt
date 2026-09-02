package com.steamforge.game.ui.game

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.core.GameState
import com.steamforge.game.core.Move
import com.steamforge.game.data.DataRepo
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.data.SavedGame
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
import org.junit.Assert.assertNotEquals
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

        val currentGame: SavedGame?
            get() = delegate.currentGame

        override suspend fun saveGame(state: SavedGame) {
            if (remainingIoFailures > 0) {
                remainingIoFailures--
                throw IOException("ENOSPC")
            }
            delegate.saveGame(state)
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

    private fun performOneValidMove(model: GameViewModel): GameState {
        for (move in listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)) {
            val before = model.ui.value.state
            model.onMove(move)
            if (model.ui.value.state != before) return model.ui.value.state
        }
        error("fixture did not provide a valid move")
    }
}
