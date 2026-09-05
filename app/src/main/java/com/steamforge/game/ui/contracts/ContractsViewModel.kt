package com.steamforge.game.ui.contracts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.analytics.Analytics
import com.steamforge.game.analytics.AnalyticsEvent
import com.steamforge.game.analytics.AnalyticsEvents
import com.steamforge.game.analytics.NoopAnalytics
import com.steamforge.game.analytics.log
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.BlueprintCollections
import com.steamforge.game.progression.ContractDef
import com.steamforge.game.progression.ContractReward
import com.steamforge.game.progression.ContractType
import com.steamforge.game.progression.DailyContracts
import com.steamforge.game.progression.LocalDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContractItemUi(
    val def: ContractDef,
    val progress: Int,
    val claimed: Boolean,
    val recommended: Boolean = false,
) {
    val complete: Boolean get() = progress >= def.target
    val fraction: Float get() = (progress.toFloat() / def.target.coerceAtLeast(1)).coerceIn(0f, 1f)
}

data class ContractsUiState(
    val day: Long = -1L,
    val workshopParts: Int = 0,
    val items: List<ContractItemUi> = emptyList(),
    val firstContractOnboarding: Boolean = false,
) {
    val completed: Int get() = items.count { it.complete }
    val claimed: Int get() = items.count { it.claimed }
}

internal fun shouldFocusFirstContract(
    gamesPlayed: Int,
    ledgerDay: Long,
    today: Long,
    claimedCount: Int,
): Boolean = gamesPlayed == 1 && ledgerDay == today && claimedCount == 0

internal fun prioritizeFirstContract(
    items: List<ContractItemUi>,
    enabled: Boolean,
): List<ContractItemUi> {
    if (!enabled || items.isEmpty()) return items
    val candidates = items.filterNot { it.claimed }
    if (candidates.isEmpty()) return items

    val completed = candidates.filter { it.complete }
    val recommended = if (completed.isNotEmpty()) {
        completed.minByOrNull { firstContractTypePriority(it.def.type) }
    } else {
        candidates.minWithOrNull(
            compareBy<ContractItemUi> { firstContractConceptTier(it.def.type) }
                .thenBy { 1f - it.fraction }
                .thenBy { firstContractTypePriority(it.def.type) },
        )
    } ?: return items

    return buildList(items.size) {
        add(recommended.copy(recommended = true))
        items.asSequence()
            .filterNot { it.def.id == recommended.def.id }
            .forEach { add(it.copy(recommended = false)) }
    }
}

private fun firstContractConceptTier(type: ContractType): Int = when (type) {
    ContractType.MERGE_COUNT,
    ContractType.SURVIVE_MOVES,
    ContractType.PLAY_RUNS -> 0
    ContractType.SCORE -> 1
    ContractType.REACH_TILE,
    ContractType.TOTAL_SCORE -> 2
    ContractType.MAKE_TILE,
    ContractType.COMBO_COUNT -> 3
}

private fun firstContractTypePriority(type: ContractType): Int = when (type) {
    ContractType.MERGE_COUNT -> 0
    ContractType.SURVIVE_MOVES -> 1
    ContractType.PLAY_RUNS -> 2
    ContractType.SCORE -> 3
    ContractType.REACH_TILE -> 4
    ContractType.TOTAL_SCORE -> 5
    ContractType.MAKE_TILE -> 6
    ContractType.COMBO_COUNT -> 7
}

class ContractsViewModel(
    private val repo: DataRepo,
    private val today: () -> Long = { LocalDay.todayEpochDay() },
    private val analytics: Analytics = NoopAnalytics(),
) : ViewModel() {

    val ui: StateFlow<ContractsUiState> = repo.progress.map { progress ->
        val day = today()
        val ledger = DailyContracts.normalized(progress.contracts, day)
        val completedCollection = BlueprintCollections.isSteamEngineComplete(progress.blueprintPieces)
        val scheduledWithBlueprint = DailyContracts.forEpochDay(day, blueprintAvailable = true)
        val claimedBlueprintToday = scheduledWithBlueprint.any { def ->
            def.reward is ContractReward.BlueprintPiece && def.id in ledger.claimedIds
        }
        val blueprintAvailable = !completedCollection || claimedBlueprintToday
        val firstContractOnboarding = shouldFocusFirstContract(
            gamesPlayed = progress.stats.gamesPlayed,
            ledgerDay = progress.contracts.day,
            today = day,
            claimedCount = ledger.claimedIds.size,
        )
        val items = prioritizeFirstContract(
            items = DailyContracts.forEpochDay(day, blueprintAvailable).map { def ->
                ContractItemUi(
                    def = def,
                    progress = DailyContracts.progress(def, ledger),
                    claimed = def.id in ledger.claimedIds,
                )
            },
            enabled = firstContractOnboarding,
        )
        ContractsUiState(
            day = day,
            workshopParts = progress.workshopParts,
            items = items,
            firstContractOnboarding = firstContractOnboarding,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContractsUiState())

    fun claim(contractId: String) {
        viewModelScope.launch {
            val day = today()
            var completedEvent: AnalyticsEvent? = null
            var economyEvent: AnalyticsEvent? = null
            var blueprintEvent: AnalyticsEvent? = null
            var collectionEvent: AnalyticsEvent? = null

            repo.updateProgress { progress ->
                val ledger = DailyContracts.normalized(progress.contracts, day)
                val blueprintAvailable = !BlueprintCollections.isSteamEngineComplete(progress.blueprintPieces)
                val contract = DailyContracts.forEpochDay(day, blueprintAvailable)
                    .firstOrNull { it.id == contractId }
                    ?: return@updateProgress progress
                if (contract.id in ledger.claimedIds || !DailyContracts.isComplete(contract, ledger)) {
                    return@updateProgress progress
                }

                val beforePieces = progress.blueprintPieces
                val updated = DailyContracts.claim(progress, day, contractId)
                if (updated == progress) return@updateProgress progress

                val (rewardType, rewardAmount) = when (val reward = contract.reward) {
                    is ContractReward.WorkshopParts -> "workshop_parts" to reward.amount
                    is ContractReward.BlueprintPiece -> "blueprint_piece" to 1
                }
                completedEvent = AnalyticsEvents.contractCompleted(
                    contractId = contract.id,
                    type = contract.type.name,
                    target = contract.target,
                    rewardType = rewardType,
                    rewardAmount = rewardAmount,
                )

                if (contract.reward is ContractReward.WorkshopParts) {
                    val earnedParts = (
                        updated.workshopParts - progress.workshopParts.coerceAtLeast(0)
                    ).coerceAtLeast(0)
                    if (earnedParts > 0) {
                        economyEvent = AnalyticsEvents.resourceEarned(
                            resourceType = "workshop_parts",
                            source = "daily_contract",
                            amount = earnedParts,
                            balanceAfter = updated.workshopParts,
                        )
                    }
                }

                val addedPieceId = (updated.blueprintPieces - beforePieces).singleOrNull()
                if (addedPieceId != null) {
                    val collection = BlueprintCollections.all.firstOrNull { addedPieceId in it.pieceIds }
                    if (collection != null) {
                        blueprintEvent = AnalyticsEvents.blueprintReceived(
                            collectionId = collection.id,
                            pieceId = addedPieceId,
                            owned = BlueprintCollections.ownedCount(collection, updated.blueprintPieces),
                            total = collection.pieces.size,
                        )
                        val wasComplete = BlueprintCollections.isComplete(collection, beforePieces)
                        val isComplete = BlueprintCollections.isComplete(collection, updated.blueprintPieces)
                        if (!wasComplete && isComplete) {
                            collectionEvent = AnalyticsEvents.collectionCompleted(
                                collectionId = collection.id,
                                totalPieces = collection.pieces.size,
                            )
                        }
                    }
                }
                updated
            }

            completedEvent?.let { analytics.log(it) }
            economyEvent?.let { analytics.log(it) }
            blueprintEvent?.let { analytics.log(it) }
            collectionEvent?.let { analytics.log(it) }
        }
    }
}
