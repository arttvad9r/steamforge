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
    fun `streak continues from yesterday and resets after gap`() = runTest(dispatcher) {
        val day = 1000L
        val continuingRepo = FakeDataRepo(
            initialProgress = PlayerProgress(dailyRewardDay = day - 1, dailyRewardStreak = 3),
        )
        val continuing = WorkshopViewModel(continuingRepo, today = { day })
        backgroundScope.subscribe(continuing.ui)
        advanceUntilIdle()
        assertEquals(4, continuing.ui.value.dailyRewardDay)

        val gapRepo = FakeDataRepo(
            initialProgress = PlayerProgress(dailyRewardDay = day - 2, dailyRewardStreak = 5),
        )
        val gap = WorkshopViewModel(gapRepo, today = { day })
        backgroundScope.subscribe(gap.ui)
        advanceUntilIdle()
        assertEquals(1, gap.ui.value.dailyRewardDay)
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
