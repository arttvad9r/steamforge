package com.steamforge.game.ui.workshop

import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.ProgressionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/**
 * Дневная награда выдаётся ровно один раз за календарный день,
 * в том числе при повторных вызовах claim и при пересоздании ViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyRewardTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm(repo: FakeDataRepo, today: Long = LocalDay.todayEpochDay()): WorkshopViewModel =
        WorkshopViewModel(repo, cfg = ProgressionConfig(), today = { today })

    /** stateIn(WhileSubscribed) оживает только при подписчике. */
    private fun kotlinx.coroutines.CoroutineScope.subscribe(ui: kotlinx.coroutines.flow.StateFlow<*>) =
        launch { ui.collect {} }

    @Test
    fun `daily reward claimable once per day and idempotent on repeat`() = runTest(dispatcher) {
        val repo = FakeDataRepo()
        val model = vm(repo)
        backgroundScope.subscribe(model.ui)
        advanceUntilIdle()

        val today = LocalDay.todayEpochDay()
        assertTrue(model.ui.value.dailyRewardAvailable)
        val expected = ProgressionConfig().dailyRewardGems(1)
        model.claimDailyReward()
        advanceUntilIdle()
        assertEquals(expected, repo.currentProgress.gems)
        assertEquals(today, repo.currentProgress.dailyRewardDay)

        // повторный claim в тот же день (recomposition / повторный вход / пересоздание VM)
        val second = vm(repo)
        backgroundScope.subscribe(second.ui)
        advanceUntilIdle()
        assertFalse(second.ui.value.dailyRewardAvailable)
        second.claimDailyReward()
        advanceUntilIdle()
        assertEquals(expected, repo.currentProgress.gems)
    }

    @Test
    fun `streak continues only from yesterday and reward grows with day`() = runTest(dispatcher) {
        val today = LocalDay.todayEpochDay()
        val cfg = ProgressionConfig()
        // вчера был claim дня 3
        val repo = FakeDataRepo(initialProgress = com.steamforge.game.progression.PlayerProgress(
            dailyRewardDay = today - 1,
            dailyRewardStreak = 3,
            gems = 0,
        ))
        val model = vm(repo, today)
        backgroundScope.subscribe(model.ui)
        advanceUntilIdle()
        assertTrue(model.ui.value.dailyRewardAvailable)
        assertEquals(4, model.ui.value.dailyRewardDay)
        model.claimDailyReward()
        advanceUntilIdle()
        assertEquals(cfg.dailyRewardGems(4), repo.currentProgress.gems)
        assertEquals(today, repo.currentProgress.dailyRewardDay)
        assertEquals(4, repo.currentProgress.dailyRewardStreak)
    }
}
