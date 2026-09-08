package com.steamforge.game.ui.contracts

import com.steamforge.game.data.DataRepo
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.DailyContracts
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
class ContractsPersistenceIoTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun CoroutineScope.subscribe(ui: StateFlow<*>) = launch { ui.collect {} }

    private class FailingRepo(val delegate: FakeDataRepo) : DataRepo by delegate {
        override suspend fun updateProgress(block: (PlayerProgress) -> PlayerProgress) {
            throw IOException("ENOSPC contracts")
        }
    }

    @Test
    fun `contract claim keeps durable progress when persistence fails`() = runTest(dispatcher) {
        val day = 1_000L
        val delegate = FakeDataRepo()
        val repo = FailingRepo(delegate)
        val vm = ContractsViewModel(repo, today = { day })
        backgroundScope.subscribe(vm.ui)
        advanceUntilIdle()
        val contractId = DailyContracts.forEpochDay(day, blueprintAvailable = true).first().id
        val before = delegate.currentProgress

        vm.claim(contractId)
        advanceUntilIdle()

        assertEquals(before, delegate.currentProgress)
    }
}
