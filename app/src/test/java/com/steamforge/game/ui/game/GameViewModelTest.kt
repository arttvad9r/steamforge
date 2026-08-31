package com.steamforge.game.ui.game

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.core.GameState
import com.steamforge.game.core.Move
import com.steamforge.game.core.Tile
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.data.SavedGame
import com.steamforge.game.progression.DailyChallenge
import com.steamforge.game.progression.DailyChallenges
import com.steamforge.game.progression.DailyGoalType
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.PlayerProgress
import com.steamforge.game.progression.ProgressionConfig
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
class GameViewModelTest {

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
        val events = mutableListOf<Pair<String, Map<String, Any?>>>()
        override fun logEvent(name: String, params: Map<String, Any?>) {
            events += name to params
        }
    }

    private fun vm(
        repo: FakeDataRepo = FakeDataRepo(),
        analytics: RecordingAnalytics = RecordingAnalytics(),
        daily: Boolean = false,
        seed: Long = 42L,
        dailyChallenge: DailyChallenge? = null,
    ): GameViewModel = GameViewModel(
        repo = repo,
        analytics = analytics,
        cfg = ProgressionConfig(),
        dailyMode = daily,
        dailyProvider = { dailyChallenge ?: DailyChallenges.forEpochDay(LocalDay.todayEpochDay()) },
        seedProvider = { seed },
        savedGameProvider = { repo.currentGame },
        systemAnimationsEnabled = true,
    )

    private suspend fun GameViewModel.playMoves(maxMoves: Int = 60) {
        val moves = listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)
        repeat(maxMoves) { i -> onMove(moves[i % moves.size]) }
    }

    /** Full board with exactly one legal merge. LEFT merges the leading 2+2, spawn fills the gap,
     * and the resulting board has no adjacent equal tiles, so Game Over is deterministic. */
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

    @Test
    fun `new game restores or starts fresh`() = runTest(dispatcher) {
        val model = vm()
        advanceUntilIdle()
        assertEquals(2, model.ui.value.state.tiles.size)
    }

    @Test
    fun `move updates board and free undos allow one step back`() = runTest(dispatcher) {
        val model = vm(seed = 7L)
        advanceUntilIdle()
        var moved = false
        for (m in listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)) {
            val before = model.ui.value.state
            model.onMove(m)
            advanceUntilIdle()
            if (model.ui.value.state != before) {
                moved = true
                break
            }
        }
        assertTrue(moved)
        assertTrue(model.ui.value.canUndo)
        val beforeUndo = model.ui.value.state
        model.undo()
        advanceUntilIdle()
        assertTrue(model.ui.value.state.score <= beforeUndo.score)
        assertEquals(1, model.ui.value.freeUndosLeft)
        assertFalse(model.ui.value.canUndo)
    }

    @Test
    fun `pressure accumulates and overdrive activates`() = runTest(dispatcher) {
        val model = vm(seed = 7L)
        advanceUntilIdle()
        var seenOverdrive = false
        for (i in 0 until 100) {
            model.onMove(listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)[i % 4])
            advanceUntilIdle()
            if (model.ui.value.overdriveRemaining > 0) {
                seenOverdrive = true
                assertEquals(0, model.ui.value.pressure)
                break
            }
        }
        assertTrue(seenOverdrive)
        assertTrue(model.ui.value.overdrivesSession >= 1)
    }

    @Test
    fun `exit saves normal game but does not grant progression`() = runTest(dispatcher) {
        val repo = FakeDataRepo()
        val model = vm(repo, seed = 7L)
        advanceUntilIdle()
        model.playMoves(12)
        advanceUntilIdle()
        val before = repo.currentProgress
        model.exit()
        advanceUntilIdle()
        assertEquals(before, repo.currentProgress)
        assertNotNull(repo.currentGame)
        assertFalse(model.ui.value.finished)
    }

    @Test
    fun `daily exit cannot farm xp`() = runTest(dispatcher) {
        val repo = FakeDataRepo()
        repeat(3) {
            val model = vm(repo, daily = true, seed = 7L)
            advanceUntilIdle()
            model.exit()
            advanceUntilIdle()
        }
        assertEquals(0, repo.currentProgress.totalXp)
        assertEquals(0, repo.currentProgress.stats.gamesPlayed)
    }

    @Test
    fun `daily challenge reward is idempotent across different viewmodels`() = runTest(dispatcher) {
        val today = LocalDay.todayEpochDay()
        val challenge = DailyChallenge(
            epochDay = today,
            type = DailyGoalType.REACH_SCORE,
            target = 0,
            mergeLevel = 6,
            seed = 12345L,
            rewardGems = 15,
            bonusXp = 60,
        )
        val repo = FakeDataRepo()
        val first = vm(repo, daily = true, seed = challenge.seed, dailyChallenge = challenge)
        advanceUntilIdle()
        for (move in listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)) {
            first.onMove(move)
            advanceUntilIdle()
            if (first.ui.value.dailySatisfied) break
        }
        assertTrue(first.ui.value.dailySatisfied)
        val afterFirst = repo.currentProgress
        assertTrue(afterFirst.dailyChallengeDone)
        assertEquals(today, afterFirst.dailyChallengeDay)
        assertEquals(1, afterFirst.stats.dailyCompleted)

        val second = vm(repo, daily = true, seed = challenge.seed, dailyChallenge = challenge)
        advanceUntilIdle()
        assertTrue(second.ui.value.dailySatisfied)
        second.playMoves(20)
        advanceUntilIdle()
        assertEquals(afterFirst.gems, repo.currentProgress.gems)
        assertEquals(afterFirst.totalXp, repo.currentProgress.totalXp)
        assertEquals(1, repo.currentProgress.stats.dailyCompleted)
    }

    @Test
    fun `daily mode does not persist active save`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialGame = null)
        val model = vm(repo, daily = true)
        advanceUntilIdle()
        model.onMove(Move.LEFT)
        advanceUntilIdle()
        assertNull(repo.currentGame)
    }

    @Test
    fun `wrench removes low tile and spends gems`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialProgress = PlayerProgress(gems = 50))
        val model = vm(repo, seed = 7L)
        advanceUntilIdle()
        model.playMoves(10)
        advanceUntilIdle()
        val target = model.ui.value.state.tiles.first { it.level <= 4 }
        val before = model.ui.value.state.tiles.size
        model.toggleRemovingMode()
        assertTrue(model.ui.value.removingMode)
        model.removeTile(target)
        advanceUntilIdle()
        assertEquals(before - 1, model.ui.value.state.tiles.size)
        assertEquals(40, repo.currentProgress.gems)
        assertFalse(model.ui.value.removingMode)
    }

    @Test
    fun `undo beyond free undos refuses when broke`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialProgress = PlayerProgress(gems = 3))
        val model = vm(repo, seed = 7L)
        advanceUntilIdle()
        for (m in listOf(Move.LEFT, Move.UP)) model.onMove(m)
        advanceUntilIdle()
        model.undo(); advanceUntilIdle()
        for (m in listOf(Move.LEFT, Move.UP)) model.onMove(m)
        advanceUntilIdle()
        model.undo(); advanceUntilIdle()
        assertEquals(0, model.ui.value.freeUndosLeft)
        if (model.ui.value.canUndo) {
            model.undo(); advanceUntilIdle()
            assertEquals(3, repo.currentProgress.gems)
        }
    }

    @Test
    fun `analytics events fired for real finish`() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val repo = FakeDataRepo(initialGame = finishingSavedGame(seed = 17L))
        val model = vm(repo = repo, analytics = analytics, seed = 17L)
        advanceUntilIdle()

        model.onMove(Move.LEFT)
        advanceUntilIdle()

        assertTrue(model.ui.value.finished)
        assertTrue(analytics.events.any { it.first == "game_finished" })
    }

    @Test
    fun `rng sequence continues exactly after process recreation`() = runTest(dispatcher) {
        val repo = FakeDataRepo()
        val first = vm(repo, seed = 99L)
        advanceUntilIdle()
        first.playMoves(20)
        advanceUntilIdle()
        val saved = repo.currentGame
        assertNotNull(saved)

        val restoredRepo = FakeDataRepo(initialGame = saved)
        val restored = vm(restoredRepo, seed = 123456L)
        advanceUntilIdle()
        assertEquals(first.ui.value.state, restored.ui.value.state)

        val sequence = listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN, Move.LEFT, Move.DOWN)
        for (move in sequence) {
            first.onMove(move)
            restored.onMove(move)
            advanceUntilIdle()
            assertEquals(first.ui.value.state, restored.ui.value.state)
        }
    }

    @Test
    fun `pressure overdrive and undo count survive process recreation`() = runTest(dispatcher) {
        val repo = FakeDataRepo()
        val first = vm(repo, seed = 7L)
        advanceUntilIdle()
        var seenOverdrive = false
        for (i in 0 until 100) {
            first.onMove(listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)[i % 4])
            advanceUntilIdle()
            if (first.ui.value.overdriveRemaining > 0) {
                seenOverdrive = true
                break
            }
        }
        assertTrue(seenOverdrive)
        val saved = repo.currentGame!!
        val second = vm(FakeDataRepo(initialGame = saved), seed = 999L)
        advanceUntilIdle()
        assertEquals(first.ui.value.state, second.ui.value.state)
        assertEquals(first.ui.value.overdriveRemaining, second.ui.value.overdriveRemaining)
        assertEquals(first.ui.value.freeUndosLeft, second.ui.value.freeUndosLeft)
    }

    @Test
    fun `restart resets finish guard and allows next game moves`() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val repo = FakeDataRepo(initialGame = finishingSavedGame(seed = 123L))
        val model = vm(repo, analytics = analytics, seed = 123L)
        advanceUntilIdle()

        model.onMove(Move.LEFT)
        advanceUntilIdle()
        assertTrue(model.ui.value.finished)
        assertEquals(1, repo.currentProgress.stats.gamesPlayed)

        model.restart()
        advanceUntilIdle()
        assertFalse(model.ui.value.finished)
        // Restoring an existing saved session does not emit game_started; restart starts one new session.
        assertEquals(1, analytics.events.count { it.first == "game_started" })

        var acceptedMove = false
        for (move in listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)) {
            val before = model.ui.value.state
            model.onMove(move)
            advanceUntilIdle()
            if (model.ui.value.state != before) {
                acceptedMove = true
                break
            }
        }
        assertTrue("restart left finish guard active", acceptedMove)
    }

    @Test
    fun `seeded games are deterministic`() = runTest(dispatcher) {
        val a = vm(seed = 99L)
        val b = vm(seed = 99L)
        advanceUntilIdle()
        assertEquals(a.ui.value.state, b.ui.value.state)
        repeat(20) { i ->
            val move = listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)[i % 4]
            a.onMove(move)
            b.onMove(move)
            advanceUntilIdle()
        }
        assertEquals(a.ui.value.state, b.ui.value.state)
    }
}
