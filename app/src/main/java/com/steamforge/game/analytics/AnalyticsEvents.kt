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
    const val GAME_RESTARTED = "game_restarted"
    const val MERGE = "merge"
    const val HIGHEST_TILE_UNLOCKED = "highest_tile_unlocked"
    const val CONTRACT_COMPLETED = "contract_completed"
    const val WORKSHOP_UPGRADE = "workshop_upgrade"
    const val BLUEPRINT_RECEIVED = "blueprint_received"
    const val COLLECTION_COMPLETED = "collection_completed"
    const val RESOURCE_EARNED = "resource_earned"
    const val RESOURCE_SPENT = "resource_spent"
    const val REWARDED_OFFER_SHOWN = "rewarded_offer_shown"
    const val REWARDED_COMPLETED = "rewarded_completed"

    fun gameStarted(
        runId: String,
        daily: Boolean,
        dailyType: String? = null,
    ) = AnalyticsEvent(
        GAME_STARTED,
        buildMap {
            put("run_id", runId.trim())
            put("daily", daily)
            dailyType?.trim()?.takeIf { it.isNotEmpty() }?.let { put("daily_type", it) }
        },
    )

    fun gameFinished(
        runId: String,
        score: Int,
        maxTile: Int,
        moves: Int,
        daily: Boolean,
    ) = AnalyticsEvent(
        GAME_FINISHED,
        mapOf(
            "run_id" to runId.trim(),
            "score" to score.coerceAtLeast(0),
            "max_tile" to maxTile.coerceAtLeast(0),
            "moves" to moves.coerceAtLeast(0),
            "daily" to daily,
        ),
    )

    fun gameRestarted(
        runId: String,
        score: Int,
        maxTile: Int,
        moves: Int,
        daily: Boolean,
    ) = AnalyticsEvent(
        GAME_RESTARTED,
        mapOf(
            "run_id" to runId.trim(),
            "score" to score.coerceAtLeast(0),
            "max_tile" to maxTile.coerceAtLeast(0),
            "moves" to moves.coerceAtLeast(0),
            "daily" to daily,
        ),
    )

    fun merge(
        tileLevel: Int,
        tileValue: Int,
        moveNumber: Int,
        mergesInMove: Int,
        scoreGained: Int,
        daily: Boolean,
    ) = AnalyticsEvent(
        MERGE,
        mapOf(
            "tile_level" to tileLevel.coerceAtLeast(0),
            "tile_value" to tileValue.coerceAtLeast(0),
            "move" to moveNumber.coerceAtLeast(0),
            "merges_in_move" to mergesInMove.coerceAtLeast(0),
            "score_gained" to scoreGained.coerceAtLeast(0),
            "daily" to daily,
        ),
    )

    fun highestTileUnlocked(
        tileLevel: Int,
        tileValue: Int,
        moveNumber: Int,
        daily: Boolean,
    ) = AnalyticsEvent(
        HIGHEST_TILE_UNLOCKED,
        mapOf(
            "tile_level" to tileLevel.coerceAtLeast(0),
            "tile_value" to tileValue.coerceAtLeast(0),
            "move" to moveNumber.coerceAtLeast(0),
            "daily" to daily,
        ),
    )

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

    fun resourceEarned(
        resourceType: String,
        source: String,
        amount: Int,
        balanceAfter: Int,
    ) = economyEvent(
        name = RESOURCE_EARNED,
        resourceType = resourceType,
        source = source,
        amount = amount,
        balanceAfter = balanceAfter,
    )

    fun resourceSpent(
        resourceType: String,
        source: String,
        amount: Int,
        balanceAfter: Int,
    ) = economyEvent(
        name = RESOURCE_SPENT,
        resourceType = resourceType,
        source = source,
        amount = amount,
        balanceAfter = balanceAfter,
    )

    fun rewardedOfferShown(
        placement: String,
        rewardType: String,
        rewardAmount: Int,
        daily: Boolean,
    ) = AnalyticsEvent(
        REWARDED_OFFER_SHOWN,
        mapOf(
            "placement" to placement,
            "reward_type" to rewardType,
            "reward_amount" to rewardAmount.coerceAtLeast(0),
            "daily" to daily,
        ),
    )

    fun rewardedCompleted(
        placement: String,
        rewardType: String,
        rewardAmount: Int,
        daily: Boolean,
    ) = AnalyticsEvent(
        REWARDED_COMPLETED,
        mapOf(
            "placement" to placement,
            "reward_type" to rewardType,
            "reward_amount" to rewardAmount.coerceAtLeast(0),
            "daily" to daily,
        ),
    )

    private fun economyEvent(
        name: String,
        resourceType: String,
        source: String,
        amount: Int,
        balanceAfter: Int,
    ) = AnalyticsEvent(
        name,
        mapOf(
            "resource_type" to resourceType,
            "source" to source,
            "amount" to amount.coerceAtLeast(0),
            "balance_after" to balanceAfter.coerceAtLeast(0),
        ),
    )
}
