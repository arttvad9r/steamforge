package com.steamforge.game.ui.contracts

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.ContractCounters
import com.steamforge.game.progression.ContractLedger
import com.steamforge.game.progression.DailyContracts
import com.steamforge.game.progression.PlayerProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContractsAnalyticsTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `opening contracts and successful claim emit funnel events once`() = runTest(dispatcher) {
        val day = 1_234L
        val complete = ContractCounters(
            score = 10_000_000,
            merges = 10_000_000,
            moves = 10_000_000,
            runs = 10_000_000,
            maxTileLevel = 30,
            overdrives = 10_000_000,
        )
        val repo = FakeDataRepo(
            initialProgress = PlayerProgress(contracts = ContractLedger(day = day, totals = complete)),
        )
        val analytics = CaptureAnalytics()
        val vm = ContractsViewModel(repo, today = { day }, analytics = analytics)

        val opened = analytics.events.singleOrNull { it.name == "contracts_opened" }
        assertNotNull(opened)
        assertEquals(3, opened?.params?.get("contracts"))

        val contract = DailyContracts.forEpochDay(day).first()
        vm.claim(contract.id)
        advanceUntilIdle()

        val claimed = analytics.events.filter { it.name == "contract_completed" }
        assertEquals(1, claimed.size)
        assertEquals(contract.id, claimed.single().params["contract_id"])
        assertEquals(contract.type.name, claimed.single().params["type"])
        assertEquals(contract.rewardGems, claimed.single().params["reward_gems"])

        vm.claim(contract.id)
        advanceUntilIdle()
        assertEquals(1, analytics.events.count { it.name == "contract_completed" })
    }

    private data class Event(val name: String, val params: Map<String, Any?>)

    private class CaptureAnalytics : Analytics {
        val events = mutableListOf<Event>()
        override fun logEvent(name: String, params: Map<String, Any?>) {
            events += Event(name, params)
        }
    }
}
