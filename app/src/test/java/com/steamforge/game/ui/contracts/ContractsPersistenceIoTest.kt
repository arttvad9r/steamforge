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

    private class FailingProgressRepo(
        val delegate: FakeDataRepo,
    ) : DataRepo by delegate {
        var updateAttempts: Int = 0
            private set

        override suspend fun updateProgress(block: (PlayerProgress) -> PlayerProgress) {
            updateAttempts += 1
            throw IOException("ENOSPC contracts")
        }
    }

    @Test
    fun `claim keeps durable progress visible when persistence write fails`() = runTest(dispatcher) {
        val day = 20_000L
        val initial = PlayerProgress(workshopParts = 41)
        val repo = FailingProgressRepo(FakeDataRepo(initial))
        val vm = ContractsViewModel(repo = repo, today = { day })
        backgroundScope.subscribe(vm.ui)
        advanceUntilIdle()
        val contractId = DailyContracts.forEpochDay(day, blueprintAvailable = true).first().id

        vm.claim(contractId)
        advanceUntilIdle()

        assertEquals(1, repo.updateAttempts)
        assertEquals(initial, repo.delegate.currentProgress)
        assertEquals(initial.workshopParts, vm.ui.value.workshopParts)
    }
}
