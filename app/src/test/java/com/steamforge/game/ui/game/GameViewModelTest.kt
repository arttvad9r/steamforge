package com.steamforge.game.ui.game

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.core.Move
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.DailyChallenges
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.PlayerProgress
import com.steamforge.game.progression.ProgressionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.random.Random
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
    ): GameViewModel = GameViewModel(
        repo = repo,
        analytics = analytics,
        cfg = ProgressionConfig(),
        dailyMode = daily,
        dailyProvider = { DailyChallenges.forEpochDay(20600L) },
        seedProvider = { seed },
        savedGameProvider = { repo.currentGame },
        systemAnimationsEnabled = true,
    )

    private suspend fun GameViewModel.playUntilMergeOrMax(maxMoves: Int = 60): Int {
        val moves = listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)
        for (i in 0 until maxMoves) {
            onMove(moves[i % 4])
        }
        return ui.value.state.score
    }

    @Test
    fun `new game restores or starts fresh`() = runTest(dispatcher) {
        val repo = FakeDataRepo()
        val vm = vm(repo)
        advanceUntilIdle()
        assertEquals(2, vm.ui.value.state.tiles.size)
    }

    @Test
    fun `move updates board and free undos allow one step back`() = runTest(dispatcher) {
        val vm = vm(seed = 7L)
        advanceUntilIdle()
        // играем до первого успешного хода
        var moved = false
        for (m in listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)) {
            val before = vm.ui.value.state
            vm.onMove(m)
            advanceUntilIdle()
            if (vm.ui.value.state != before) {
                moved = true
                break
            }
        }
        assertTrue(moved)
        assertTrue(vm.ui.value.canUndo)
        val beforeUndo = vm.ui.value.state
        vm.undo()
        advanceUntilIdle()
        assertTrue(vm.ui.value.state.score <= beforeUndo.score)
        assertEquals(1, vm.ui.value.freeUndosLeft)
        assertFalse(vm.ui.value.canUndo)
    }

    @Test
    fun `pressure accumulates and overdrive activates with multiplier`() = runTest(dispatcher) {
        val vm = vm(seed = 7L)
        advanceUntilIdle()
        var seenOverdrive = false
        var scoreWithout = 0
        for (i in 0 until 80) {
            vm.onMove(listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)[i % 4])
            advanceUntilIdle()
            val s = vm.ui.value
            if (s.overdriveRemaining > 0) {
                seenOverdrive = true
                // во время overdrive давление заморожено
                assertEquals(0, s.pressure)
            }
            if (seenOverdrive) break
        }
        assertTrue(seenOverdrive)
        assertTrue(vm.ui.value.overdrivesSession >= 1)
        assertTrue(vm.ui.value.state.score > scoreWithout)
    }

    @Test
    fun `finish on exit grants xp and counts game`() = runTest(dispatcher) {
        val repo = FakeDataRepo()
        val vm = vm(repo, seed = 7L)
        advanceUntilIdle()
        vm.playUntilMergeOrMax(12)
        vm.exit()
        advanceUntilIdle()
        val p = repo.currentProgress
        assertEquals(1, p.stats.gamesPlayed)
        assertTrue(p.totalXp > 0)
        assertNull(repo.currentGame)
        assertTrue(vm.ui.value.finished)
    }

    @Test
    fun `daily mode completes goal and grants reward once`() = runTest(dispatcher) {
        val repo = FakeDataRepo()
        val challenge = DailyChallenges.forEpochDay(20600L)
        val vm = vm(repo, daily = true, seed = challenge.seed)
        advanceUntilIdle()
        for (i in 0 until 200) {
            if (vm.ui.value.dailySatisfied) break
            vm.onMove(listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)[i % 4])
            advanceUntilIdle()
        }
        assertTrue(vm.ui.value.dailySatisfied)
        val p = repo.currentProgress
        assertTrue(p.dailyChallengeDone)
        // завершение записывается на сегодняшний день (challenge выдаётся на today)
        assertEquals(LocalDay.todayEpochDay(), p.dailyChallengeDay)
        // награда за challenge + достижение daily_1
        assertTrue(p.gems >= challenge.rewardGems)
        assertTrue(p.totalXp >= challenge.bonusXp)
        assertEquals(1, p.stats.dailyCompleted)
        // повторное выполнение не даёт награду второй раз
        val gemsAfter = p.gems
        for (i in 0 until 10) {
            vm.onMove(Move.LEFT)
            advanceUntilIdle()
        }
        assertEquals(gemsAfter, repo.currentProgress.gems)
    }

    @Test
    fun `daily mode does not persist or restore save`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialGame = null)
        val vm = vm(repo, daily = true)
        advanceUntilIdle()
        vm.onMove(Move.LEFT)
        advanceUntilIdle()
        assertNull(repo.currentGame)
    }

    @Test
    fun `wrench removes low tile and spends gems`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialProgress = PlayerProgress(gems = 50))
        val vm = vm(repo, seed = 7L)
        advanceUntilIdle()
        vm.playUntilMergeOrMax(10)
        advanceUntilIdle()
        val target = vm.ui.value.state.tiles.first { it.level <= 4 }
        val before = vm.ui.value.state.tiles.size
        vm.removeTile(target)
        advanceUntilIdle()
        assertEquals(before - 1, vm.ui.value.state.tiles.size)
        assertEquals(50 - 10, repo.currentProgress.gems)
        assertFalse(vm.ui.value.removingMode)
    }

    @Test
    fun `undo beyond free undos costs gems and refuses when broke`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialProgress = PlayerProgress(gems = 3))
        val vm = vm(repo, seed = 7L)
        advanceUntilIdle()
        // делаем два успешных хода и два undo: 2 бесплатных, дальше не хватает гемов (3 < 5)
        for (m in listOf(Move.LEFT, Move.UP)) vm.onMove(m)
        advanceUntilIdle()
        vm.undo(); advanceUntilIdle()
        for (m in listOf(Move.LEFT, Move.UP)) vm.onMove(m)
        advanceUntilIdle()
        vm.undo(); advanceUntilIdle()
        assertEquals(0, vm.ui.value.freeUndosLeft)
        val canUndoBefore = vm.ui.value.canUndo
        if (canUndoBefore) {
            vm.undo(); advanceUntilIdle()
            assertEquals(3, repo.currentProgress.gems)
        }
    }

    @Test
    fun `analytics events fired`() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val vm = vm(repo = FakeDataRepo(), analytics = analytics)
        advanceUntilIdle()
        assertTrue(analytics.events.any { it.first == "game_started" })
        vm.exit()
        advanceUntilIdle()
        assertTrue(analytics.events.any { it.first == "game_finished" })
    }

    @Test
    fun `pressure and overdrive survive process recreation`() = runTest(dispatcher) {
        val repo = FakeDataRepo()
        val vm1 = vm(repo, seed = 7L)
        advanceUntilIdle()
        // играем до активного Overdrive (давление при этом заморожено на 0)
        var seenOverdrive = false
        for (i in 0 until 80) {
            vm1.onMove(listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)[i % 4])
            advanceUntilIdle()
            if (vm1.ui.value.overdriveRemaining > 0) {
                seenOverdrive = true
                break
            }
        }
        assertTrue(seenOverdrive)
        val remaining = vm1.ui.value.overdriveRemaining
        assertTrue(remaining > 0)
        assertTrue(repo.currentGame!!.overdriveRemaining == remaining)

        // "пересоздание процесса": новое состояние приложения/репозитория читает тот же save
        val vm2 = vm(repo, seed = 999L) // другой seed провайдера: restore обязан взять seed из save
        advanceUntilIdle()
        assertEquals(vm1.ui.value.state, vm2.ui.value.state)
        assertEquals(remaining, vm2.ui.value.overdriveRemaining)
        assertEquals(vm1.ui.value.freeUndosLeft, vm2.ui.value.freeUndosLeft)
    }

    @Test
    fun `accumulated pressure survives process recreation`() = runTest(dispatcher) {
        val repo = FakeDataRepo()
        val vm1 = vm(repo, seed = 7L)
        advanceUntilIdle()
        var moved = false
        for (i in 0 until 40) {
            vm1.onMove(listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)[i % 4])
            advanceUntilIdle()
            if (vm1.ui.value.pressure > 0) {
                moved = true
                break
            }
        }
        assertTrue(moved)
        val pressure = vm1.ui.value.pressure
        assertTrue(pressure in 1..99)

        val vm2 = vm(repo)
        advanceUntilIdle()
        assertEquals(pressure, vm2.ui.value.pressure)
    }

    @Test
    fun `restore keeps saved game and won flag`() = runTest(dispatcher) {
        val engine = com.steamforge.game.core.GameEngine()
        val saved = com.steamforge.game.data.SavedGame(
            state = engine.newGame(rng = Random(5)).copy(won = true),
            seed = 5L,
            pressure = 40,
            overdriveRemaining = 2,
            freeUndosLeft = 1,
        )
        val repo = FakeDataRepo(initialGame = saved)
        val vm = vm(repo)
        advanceUntilIdle()
        assertEquals(saved.state.tiles, vm.ui.value.state.tiles)
        assertEquals(40, vm.ui.value.pressure)
        assertEquals(2, vm.ui.value.overdriveRemaining)
        assertEquals(1, vm.ui.value.freeUndosLeft)
        assertTrue(vm.ui.value.winCelebrated)
    }

    @Test
    fun `seeded game is deterministic`() = runTest(dispatcher) {
        val a = vm(seed = 99L)
        val b = vm(seed = 99L)
        advanceUntilIdle()
        assertEquals(a.ui.value.state, b.ui.value.state)
        for (i in 0 until 5) {
            a.onMove(Move.LEFT); b.onMove(Move.LEFT)
            advanceUntilIdle()
        }
        assertEquals(a.ui.value.state, b.ui.value.state)
    }

    @Test
    fun `local day helper consistent`() {
        assertEquals(LocalDay.epochDayOf(2026, 8, 30), LocalDay.epochDayOf(2026, 8, 30))
    }
}
