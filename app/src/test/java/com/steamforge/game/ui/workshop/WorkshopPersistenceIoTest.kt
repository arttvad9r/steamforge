package com.steamforge.game.ui.workshop

import com.steamforge.game.data.DataRepo
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.PlayerProgress
import java.io.IOException
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
class WorkshopPersistenceIoTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun CoroutineScope.subscribe(ui: StateFlow<*>) = launch { ui.collect {} }

    private class FailingProgressRepo(
        val delegate: FakeDataRepo,
    ) : DataRepo by delegate {
        var updateAttempts: Int = 0
            private set

        override suspend fun updateProgress(block: (PlayerProgress) -> PlayerProgress) {
            updateAttempts += 1
            throw IOException("ENOSPC workshop")
        }
    }

    @Test
    fun `upgrade keeps durable workshop state when persistence write fails`() = runTest(dispatcher) {
        val initial = PlayerProgress(workshopParts = 900)
        val repo = FailingProgressRepo(FakeDataRepo(initial))
        val vm = WorkshopViewModel(repo = repo, today = { 20_000L })
        backgroundScope.subscribe(vm.ui)
        advanceUntilIdle()

        vm.upgradeCore()
        advanceUntilIdle()

        assertEquals(1, repo.updateAttempts)
        assertEquals(initial, repo.delegate.currentProgress)
        assertEquals(initial.workshopParts, vm.ui.value.workshopParts)
    }

    @Test
    fun `daily reward keeps durable workshop state when persistence write fails`() = runTest(dispatcher) {
        val day = 20_000L
        val initial = PlayerProgress(
            gems = 17,
            workshopParts = 23,
            dailyRewardDay = day - 1,
            dailyRewardStreak = 2,
        )
        val repo = FailingProgressRepo(FakeDataRepo(initial))
        val vm = WorkshopViewModel(repo = repo, today = { day })
        backgroundScope.subscribe(vm.ui)
        advanceUntilIdle()

        vm.claimDailyReward()
        advanceUntilIdle()

        assertEquals(1, repo.updateAttempts)
        assertEquals(initial, repo.delegate.currentProgress)
        assertEquals(initial.gems, vm.ui.value.gems)
        assertEquals(initial.workshopParts, vm.ui.value.workshopParts)
    }
}
