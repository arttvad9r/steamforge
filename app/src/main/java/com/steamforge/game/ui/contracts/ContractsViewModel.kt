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
) {
    val complete: Boolean get() = progress >= def.target
    val fraction: Float get() = (progress.toFloat() / def.target.coerceAtLeast(1)).coerceIn(0f, 1f)
}

data class ContractsUiState(
    val day: Long = -1L,
    val workshopParts: Int = 0,
    val items: List<ContractItemUi> = emptyList(),
) {
    val completed: Int get() = items.count { it.complete }
    val claimed: Int get() = items.count { it.claimed }
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
        val items = DailyContracts.forEpochDay(day, blueprintAvailable).map { def ->
            ContractItemUi(
                def = def,
                progress = DailyContracts.progress(def, ledger),
                claimed = def.id in ledger.claimedIds,
            )
        }
        ContractsUiState(
            day = day,
            workshopParts = progress.workshopParts,
            items = items,
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
