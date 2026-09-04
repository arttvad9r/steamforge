package com.steamforge.game.ui.contracts

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.analytics.AnalyticsEvents
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.BlueprintCollections
import com.steamforge.game.progression.ContractCounters
import com.steamforge.game.progression.ContractDef
import com.steamforge.game.progression.ContractLedger
import com.steamforge.game.progression.ContractReward
import com.steamforge.game.progression.ContractType
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContractsAnalyticsTest {
    private val dispatcher = StandardTestDispatcher()

    private class RecordingAnalytics : Analytics {
        val events = mutableListOf<Pair<String, Map<String, Any?>>>()
        override fun logEvent(name: String, params: Map<String, Any?>) {
            events += name to params
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `claimed completion emits once even after repeated tap`() = runTest(dispatcher) {
        val day = 12_345L
        val contract = DailyContracts.forEpochDay(day, blueprintAvailable = true).first()
        val repo = FakeDataRepo(
            initialProgress = PlayerProgress(
                contracts = ContractLedger(day = day, totals = completedCounters(contract)),
            ),
        )
        val analytics = RecordingAnalytics()
        val vm = ContractsViewModel(repo = repo, today = { day }, analytics = analytics)

        vm.claim(contract.id)
        advanceUntilIdle()
        vm.claim(contract.id)
        advanceUntilIdle()

        val events = analytics.events.filter { it.first == AnalyticsEvents.CONTRACT_COMPLETED }
        assertEquals(1, events.size)
        assertEquals(contract.id, events.single().second["contract_id"])
        assertEquals(contract.type.name, events.single().second["type"])
        assertTrue(contract.id in repo.currentProgress.contracts.claimedIds)
    }

    @Test
    fun `last blueprint piece emits received and collection completed once`() = runTest(dispatcher) {
        val (day, contract) = findBlueprintContract()
        val collection = BlueprintCollections.steamEngine
        val lastPiece = collection.pieces.last()
        val ownedBefore = collection.pieces.dropLast(1).map { it.id }.toSet()
        val repo = FakeDataRepo(
            initialProgress = PlayerProgress(
                blueprintPieces = ownedBefore,
                contracts = ContractLedger(day = day, totals = completedCounters(contract)),
            ),
        )
        val analytics = RecordingAnalytics()
        val vm = ContractsViewModel(repo = repo, today = { day }, analytics = analytics)

        vm.claim(contract.id)
        advanceUntilIdle()
        vm.claim(contract.id)
        advanceUntilIdle()

        assertTrue(BlueprintCollections.isComplete(collection, repo.currentProgress.blueprintPieces))
        val received = analytics.events.filter { it.first == AnalyticsEvents.BLUEPRINT_RECEIVED }
        val completed = analytics.events.filter { it.first == AnalyticsEvents.COLLECTION_COMPLETED }
        assertEquals(1, received.size)
        assertEquals(1, completed.size)
        assertEquals(lastPiece.id, received.single().second["piece_id"])
        assertEquals(collection.id, received.single().second["collection_id"])
        assertEquals(collection.pieces.size, received.single().second["owned"])
        assertEquals(collection.id, completed.single().second["collection_id"])
    }

    private fun findBlueprintContract(): Pair<Long, ContractDef> {
        for (day in 0L..500L) {
            val contract = DailyContracts.forEpochDay(day, blueprintAvailable = true)
                .firstOrNull { it.reward is ContractReward.BlueprintPiece }
            if (contract != null) return day to contract
        }
        error("No deterministic blueprint contract found in search window")
    }

    private fun completedCounters(def: ContractDef): ContractCounters = when (def.type) {
        ContractType.MAKE_TILE -> ContractCounters(
            madeTilesByLevel = mapOf(requireNotNull(def.tileLevel) to def.target),
        )
        ContractType.REACH_TILE -> ContractCounters(maxTileLevel = requireNotNull(def.tileLevel))
        ContractType.MERGE_COUNT -> ContractCounters(merges = def.target)
        ContractType.SCORE -> ContractCounters(bestRunScore = def.target)
        ContractType.TOTAL_SCORE -> ContractCounters(score = def.target)
        ContractType.COMBO_COUNT -> ContractCounters(maxCombo = def.target)
        ContractType.PLAY_RUNS -> ContractCounters(runs = def.target)
        ContractType.SURVIVE_MOVES -> ContractCounters(moves = def.target)
    }
}
