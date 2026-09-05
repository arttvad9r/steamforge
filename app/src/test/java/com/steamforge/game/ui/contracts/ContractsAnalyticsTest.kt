package com.steamforge.game.ui.contracts

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.analytics.AnalyticsEvents
import com.steamforge.game.config.RemoteConfigProvider
import com.steamforge.game.config.RemoteConfigRefreshResult
import com.steamforge.game.config.RemoteConfigSnapshot
import com.steamforge.game.config.RemoteConfigSource
import com.steamforge.game.config.RemoteGameConfig
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.BlueprintCollections
import com.steamforge.game.progression.ContractCounters
import com.steamforge.game.progression.ContractDef
import com.steamforge.game.progression.ContractLedger
import com.steamforge.game.progression.ContractReward
import com.steamforge.game.progression.ContractType
import com.steamforge.game.progression.DailyContracts
import com.steamforge.game.progression.PlayerProgress
import com.steamforge.game.progression.scaledWorkshopParts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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

    private class FixedRemoteConfigProvider(config: RemoteGameConfig) : RemoteConfigProvider {
        override val snapshot: StateFlow<RemoteConfigSnapshot> = MutableStateFlow(
            RemoteConfigSnapshot(
                config = config.sanitized(),
                source = RemoteConfigSource.REMOTE,
                revision = "test",
            ),
        )

        override suspend fun refresh(): RemoteConfigRefreshResult = RemoteConfigRefreshResult.UPDATED
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
    fun `workshop parts contract emits actual earned amount and balance once`() = runTest(dispatcher) {
        val (day, contract) = findWorkshopPartsContract()
        val reward = contract.reward as ContractReward.WorkshopParts
        val initialBalance = 17
        val repo = FakeDataRepo(
            initialProgress = PlayerProgress(
                workshopParts = initialBalance,
                contracts = ContractLedger(day = day, totals = completedCounters(contract)),
            ),
        )
        val analytics = RecordingAnalytics()
        val vm = ContractsViewModel(repo = repo, today = { day }, analytics = analytics)

        vm.claim(contract.id)
        advanceUntilIdle()
        vm.claim(contract.id)
        advanceUntilIdle()

        val events = analytics.events.filter { it.first == AnalyticsEvents.RESOURCE_EARNED }
        assertEquals(1, events.size)
        val params = events.single().second
        assertEquals("workshop_parts", params["resource_type"])
        assertEquals("daily_contract", params["source"])
        assertEquals(reward.amount, params["amount"])
        assertEquals(initialBalance + reward.amount, params["balance_after"])
        assertEquals(initialBalance + reward.amount, repo.currentProgress.workshopParts)
    }

    @Test
    fun `remote multiplier matches displayed claimed and analytics reward`() = runTest(dispatcher) {
        val (day, contract) = findWorkshopPartsContract()
        val multiplier = 1.5
        val effectiveReward = contract.reward.scaledWorkshopParts(multiplier) as ContractReward.WorkshopParts
        val initialBalance = 13
        val repo = FakeDataRepo(
            initialProgress = PlayerProgress(
                workshopParts = initialBalance,
                contracts = ContractLedger(day = day, totals = completedCounters(contract)),
            ),
        )
        val analytics = RecordingAnalytics()
        val provider = FixedRemoteConfigProvider(
            RemoteGameConfig(contractRewardMultiplier = multiplier),
        )
        val vm = ContractsViewModel(
            repo = repo,
            remoteConfigProvider = provider,
            today = { day },
            analytics = analytics,
        )
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()

        val displayed = vm.ui.value.items.single { it.def.id == contract.id }.def.reward
            as ContractReward.WorkshopParts
        assertEquals(effectiveReward.amount, displayed.amount)

        vm.claim(contract.id)
        advanceUntilIdle()

        assertEquals(initialBalance + effectiveReward.amount, repo.currentProgress.workshopParts)

        val completedParams = analytics.events
            .single { it.first == AnalyticsEvents.CONTRACT_COMPLETED }
            .second
        assertEquals("workshop_parts", completedParams["reward_type"])
        assertEquals(effectiveReward.amount, completedParams["reward_amount"])

        val economyParams = analytics.events
            .single { it.first == AnalyticsEvents.RESOURCE_EARNED }
            .second
        assertEquals(effectiveReward.amount, economyParams["amount"])
        assertEquals(initialBalance + effectiveReward.amount, economyParams["balance_after"])
    }

    @Test
    fun `blueprint reward identity stays unchanged while fallback parts scale`() {
        val reward = ContractReward.BlueprintPiece(collectionId = "steam_engine", fallbackParts = 10)

        val scaled = reward.scaledWorkshopParts(1.5) as ContractReward.BlueprintPiece

        assertEquals(reward.collectionId, scaled.collectionId)
        assertEquals(15, scaled.fallbackParts)
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

    private fun findWorkshopPartsContract(): Pair<Long, ContractDef> {
        for (day in 0L..500L) {
            val contract = DailyContracts.forEpochDay(day, blueprintAvailable = true)
                .firstOrNull { it.reward is ContractReward.WorkshopParts }
            if (contract != null) return day to contract
        }
        error("No deterministic WorkshopParts contract found in search window")
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
