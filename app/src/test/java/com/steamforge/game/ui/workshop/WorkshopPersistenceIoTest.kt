package com.steamforge.game.ui.workshop

import com.steamforge.game.data.DataRepo
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.PlayerProgress
import com.steamforge.game.progression.WorkshopMechanism
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

    private class FailingRepo(val delegate: FakeDataRepo) : DataRepo by delegate {
        override suspend fun updateProgress(block: (PlayerProgress) -> PlayerProgress) {
            throw IOException("ENOSPC workshop")
        }
    }

    @Test
    fun `daily reward keeps durable progress when persistence fails`() = runTest(dispatcher) {
        val day = 1_000L
        val delegate = FakeDataRepo()
        val repo = FailingRepo(delegate)
        val vm = WorkshopViewModel(repo, today = { day })
        backgroundScope.subscribe(vm.ui)
        advanceUntilIdle()
        val before = delegate.currentProgress

        vm.claimDailyReward()
        advanceUntilIdle()

        assertEquals(before, delegate.currentProgress)
    }

    @Test
    fun `mechanism upgrade keeps durable progress when persistence fails`() = runTest(dispatcher) {
        val initial = PlayerProgress(workshopParts = 100_000)
        val delegate = FakeDataRepo(initial)
        val repo = FailingRepo(delegate)
        val vm = WorkshopViewModel(repo, today = { 1_000L })
        backgroundScope.subscribe(vm.ui)
        advanceUntilIdle()
        val before = delegate.currentProgress

        vm.upgradeMechanism(WorkshopMechanism.CORE)
        advanceUntilIdle()

        assertEquals(before, delegate.currentProgress)
    }
}
