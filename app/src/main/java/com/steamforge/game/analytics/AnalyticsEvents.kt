package com.steamforge.game.analytics

/**
 * Typed product analytics catalog. Domain/UI code supplies only stable primitive values;
 * provider-specific serialization remains behind [Analytics].
 */
data class AnalyticsEvent(
    val name: String,
    val params: Map<String, Any?> = emptyMap(),
)

fun Analytics.log(event: AnalyticsEvent) = logEvent(event.name, event.params)

object AnalyticsEvents {
    const val GAME_STARTED = "game_started"
    const val GAME_FINISHED = "game_finished"
    const val MERGE = "merge"
    const val HIGHEST_TILE_UNLOCKED = "highest_tile_unlocked"
    const val CONTRACT_COMPLETED = "contract_completed"
    const val WORKSHOP_UPGRADE = "workshop_upgrade"
    const val BLUEPRINT_RECEIVED = "blueprint_received"
    const val COLLECTION_COMPLETED = "collection_completed"
    const val REWARDED_OFFER_SHOWN = "rewarded_offer_shown"
    const val REWARDED_COMPLETED = "rewarded_completed"

    fun contractCompleted(
        contractId: String,
        type: String,
        target: Int,
        rewardType: String,
        rewardAmount: Int,
    ) = AnalyticsEvent(
        CONTRACT_COMPLETED,
        mapOf(
            "contract_id" to contractId,
            "type" to type,
            "target" to target.coerceAtLeast(0),
            "reward_type" to rewardType,
            "reward_amount" to rewardAmount.coerceAtLeast(0),
        ),
    )

    fun workshopUpgrade(
        mechanism: String,
        fromStage: Int,
        toStage: Int,
        partsSpent: Int,
    ) = AnalyticsEvent(
        WORKSHOP_UPGRADE,
        mapOf(
            "mechanism" to mechanism,
            "from_stage" to fromStage.coerceAtLeast(0),
            "to_stage" to toStage.coerceAtLeast(0),
            "parts_spent" to partsSpent.coerceAtLeast(0),
        ),
    )

    fun blueprintReceived(
        collectionId: String,
        pieceId: String,
        owned: Int,
        total: Int,
    ) = AnalyticsEvent(
        BLUEPRINT_RECEIVED,
        mapOf(
            "collection_id" to collectionId,
            "piece_id" to pieceId,
            "owned" to owned.coerceAtLeast(0),
            "total" to total.coerceAtLeast(0),
        ),
    )

    fun collectionCompleted(collectionId: String, totalPieces: Int) = AnalyticsEvent(
        COLLECTION_COMPLETED,
        mapOf(
            "collection_id" to collectionId,
            "total_pieces" to totalPieces.coerceAtLeast(0),
        ),
    )
}
