package com.steamforge.game.ui.workshop

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.config.LocalDefaultConfig
import com.steamforge.game.config.MutableGameConfigProvider
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
    fun `daily reward impression and successful claim emit funnel events once`() = runTest(dispatcher) {
        val day = 1000L
        val repo = FakeDataRepo()
        val analytics = CaptureAnalytics()
        val vm = WorkshopViewModel(repo, today = { day }, analytics = analytics)
        backgroundScope.subscribe(vm.ui)
        advanceUntilIdle()

        vm.recordDailyRewardShown()
        vm.recordDailyRewardShown()
        assertEquals(1, analytics.events.count { it.name == "daily_reward_shown" })
        val shown = analytics.events.single { it.name == "daily_reward_shown" }
        assertEquals(vm.ui.value.dailyRewardDay, shown.params["reward_day"])
        assertEquals(vm.ui.value.dailyRewardGems, shown.params["reward_gems"])

        val expectedReward = vm.ui.value.dailyRewardGems
        vm.claimDailyReward()
        advanceUntilIdle()

        val claimed = analytics.events.filter { it.name == "daily_reward_claimed" }
        assertEquals(1, claimed.size)
        assertEquals(expectedReward, claimed.single().params["reward_gems"])
        assertEquals(repo.currentProgress.gems, claimed.single().params["gem_balance"])

        vm.claimDailyReward()
        advanceUntilIdle()
        assertEquals(1, analytics.events.count { it.name == "daily_reward_claimed" })
    }

    @Test
    fun `streak continues normally and forgives one missed day`() = runTest(dispatcher) {
        val day = 1000L
        val continuingRepo = FakeDataRepo(
            initialProgress = PlayerProgress(dailyRewardDay = day - 1, dailyRewardStreak = 3),
        )
        val continuing = WorkshopViewModel(continuingRepo, today = { day })
        backgroundScope.subscribe(continuing.ui)
        advanceUntilIdle()
        assertEquals(4, continuing.ui.value.dailyRewardDay)

        val graceRepo = FakeDataRepo(
            initialProgress = PlayerProgress(dailyRewardDay = day - 2, dailyRewardStreak = 5),
        )
        val grace = WorkshopViewModel(graceRepo, today = { day })
        backgroundScope.subscribe(grace.ui)
        advanceUntilIdle()
        assertEquals(6, grace.ui.value.dailyRewardDay)
        assertTrue(grace.ui.value.dailyRewardUsesGrace)

        val exhaustedRepo = FakeDataRepo(
            initialProgress = PlayerProgress(
                dailyRewardDay = day - 2,
                dailyRewardStreak = 5,
                dailyRewardGraceUsed = true,
            ),
        )
        val exhausted = WorkshopViewModel(exhaustedRepo, today = { day })
        backgroundScope.subscribe(exhausted.ui)
        advanceUntilIdle()
        assertEquals(1, exhausted.ui.value.dailyRewardDay)
        assertFalse(exhausted.ui.value.dailyRewardUsesGrace)
    }

    @Test
    fun `remote reward multiplier matches displayed and granted daily reward`() = runTest(dispatcher) {
        val day = 1000L
        val repo = FakeDataRepo()
        val config = MutableGameConfigProvider(
            LocalDefaultConfig.value.copy(rewardMultiplierPercent = 200),
        )
        val vm = WorkshopViewModel(repo, configProvider = config, today = { day })
        backgroundScope.subscribe(vm.ui)
        advanceUntilIdle()

        assertEquals(10, vm.ui.value.dailyRewardGems)
        vm.claimDailyReward()
        advanceUntilIdle()
        assertEquals(10, repo.currentProgress.gems)
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

    private data class Event(val name: String, val params: Map<String, Any?>)

    private class CaptureAnalytics : Analytics {
        val events = mutableListOf<Event>()
        override fun logEvent(name: String, params: Map<String, Any?>) {
            events += Event(name, params)
        }
    }
}
