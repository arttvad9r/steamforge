package com.steamforge.game.ui.workshop

import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.PlayerProgress
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DailyRewardTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun CoroutineScope.subscribe(ui: StateFlow<*>) = launch { ui.collect {} }

    @Test
    fun `daily reward can be claimed only once per day`() = runTest(dispatcher) {
        val day = 1000L
        val repo = FakeDataRepo()
        val vm = WorkshopViewModel(repo, today = { day })
        backgroundScope.subscribe(vm.ui)
        advanceUntilIdle()
        assertTrue(vm.ui.value.dailyRewardAvailable)
        val reward = vm.ui.value.dailyRewardGems

        vm.claimDailyReward()
        advanceUntilIdle()
        assertEquals(reward, repo.currentProgress.gems)
        assertFalse(vm.ui.value.dailyRewardAvailable)

        vm.claimDailyReward()
        advanceUntilIdle()
        assertEquals(reward, repo.currentProgress.gems)
    }

    @Test
    fun `streak survives one missed day and resets after two missed days`() = runTest(dispatcher) {
        val day = 1000L
        val yesterdayRepo = FakeDataRepo(
            initialProgress = PlayerProgress(dailyRewardDay = day - 1, dailyRewardStreak = 3),
        )
        val yesterday = WorkshopViewModel(yesterdayRepo, today = { day })
        backgroundScope.subscribe(yesterday.ui)
        advanceUntilIdle()
        assertEquals(3, yesterday.ui.value.dailyRewardStreak)
        assertEquals(4, yesterday.ui.value.dailyRewardDay)

        val oneMissedDayRepo = FakeDataRepo(
            initialProgress = PlayerProgress(dailyRewardDay = day - 2, dailyRewardStreak = 5),
        )
        val oneMissedDay = WorkshopViewModel(oneMissedDayRepo, today = { day })
        backgroundScope.subscribe(oneMissedDay.ui)
        advanceUntilIdle()
        assertEquals(5, oneMissedDay.ui.value.dailyRewardStreak)
        assertEquals(6, oneMissedDay.ui.value.dailyRewardDay)

        val twoMissedDaysRepo = FakeDataRepo(
            initialProgress = PlayerProgress(dailyRewardDay = day - 3, dailyRewardStreak = 5),
        )
        val twoMissedDays = WorkshopViewModel(twoMissedDaysRepo, today = { day })
        backgroundScope.subscribe(twoMissedDays.ui)
        advanceUntilIdle()
        assertEquals(0, twoMissedDays.ui.value.dailyRewardStreak)
        assertEquals(1, twoMissedDays.ui.value.dailyRewardDay)
    }

    @Test
    fun `claim after one missed day continues persisted streak`() = runTest(dispatcher) {
        val day = 1000L
        val repo = FakeDataRepo(
            initialProgress = PlayerProgress(dailyRewardDay = day - 2, dailyRewardStreak = 5),
        )
        val vm = WorkshopViewModel(repo, today = { day })
        backgroundScope.subscribe(vm.ui)
        advanceUntilIdle()
        val expectedReward = vm.ui.value.dailyRewardGems

        vm.claimDailyReward()
        advanceUntilIdle()

        assertEquals(day, repo.currentProgress.dailyRewardDay)
        assertEquals(6, repo.currentProgress.dailyRewardStreak)
        assertEquals(6, repo.currentProgress.stats.highestDailyStreak)
        assertEquals(expectedReward, repo.currentProgress.gems)
        assertFalse(vm.ui.value.dailyRewardAvailable)
        assertEquals(6, vm.ui.value.dailyRewardStreak)
    }

    @Test
    fun `recreated viewmodel cannot claim same daily reward twice`() = runTest(dispatcher) {
        val day = 1000L
        val repo = FakeDataRepo()
        val first = WorkshopViewModel(repo, today = { day })
        backgroundScope.subscribe(first.ui)
        advanceUntilIdle()
        first.claimDailyReward()
        advanceUntilIdle()
        val gemsAfterFirst = repo.currentProgress.gems

        val second = WorkshopViewModel(repo, today = { day })
        backgroundScope.subscribe(second.ui)
        advanceUntilIdle()
        assertFalse(second.ui.value.dailyRewardAvailable)
        second.claimDailyReward()
        advanceUntilIdle()
        assertEquals(gemsAfterFirst, repo.currentProgress.gems)
    }

    @Test
    fun `reset game progress preserves settings and consent`() = runTest(dispatcher) {
        val repo = FakeDataRepo(
            initialProgress = PlayerProgress(
                gems = 99,
                totalXp = 555,
                soundEnabled = false,
                hapticsEnabled = false,
                animationsEnabled = false,
                analyticsConsent = true,
            ),
        )
        repo.resetGameProgress()
        val p = repo.currentProgress
        assertEquals(0, p.gems)
        assertEquals(0, p.totalXp)
        assertFalse(p.soundEnabled)
        assertFalse(p.hapticsEnabled)
        assertFalse(p.animationsEnabled)
        assertEquals(true, p.analyticsConsent)
    }
}
