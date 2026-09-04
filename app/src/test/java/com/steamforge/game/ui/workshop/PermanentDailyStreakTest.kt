package com.steamforge.game.ui.workshop

import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.PlayerProgress
import com.steamforge.game.progression.PlayerStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PermanentDailyStreakTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun CoroutineScope.subscribe(ui: StateFlow<*>) = launch { ui.collect {} }

    @Test
    fun `streak continues beyond seven while reward cycle wraps`() = runTest(dispatcher) {
        val day = 2_000L
        val repo = FakeDataRepo(
            initialProgress = PlayerProgress(
                dailyRewardDay = day - 1,
                dailyRewardStreak = 7,
                stats = PlayerStats(highestDailyStreak = 7),
            ),
        )
        val vm = WorkshopViewModel(repo, today = { day })
        backgroundScope.subscribe(vm.ui)
        advanceUntilIdle()

        assertEquals(1, vm.ui.value.dailyRewardDay)

        vm.claimDailyReward()
        advanceUntilIdle()

        assertEquals(8, repo.currentProgress.dailyRewardStreak)
        assertEquals(8, repo.currentProgress.stats.highestDailyStreak)
    }

    @Test
    fun `gap resets current streak but preserves legacy historical lower bound`() = runTest(dispatcher) {
        val day = 3_000L
        val repo = FakeDataRepo(
            initialProgress = PlayerProgress(
                dailyRewardDay = day - 2,
                dailyRewardStreak = 7,
                stats = PlayerStats(highestDailyStreak = 0),
            ),
        )
        val vm = WorkshopViewModel(repo, today = { day })
        backgroundScope.subscribe(vm.ui)
        advanceUntilIdle()

        assertEquals(1, vm.ui.value.dailyRewardDay)

        vm.claimDailyReward()
        advanceUntilIdle()

        assertEquals(1, repo.currentProgress.dailyRewardStreak)
        assertEquals(7, repo.currentProgress.stats.highestDailyStreak)
    }
}
