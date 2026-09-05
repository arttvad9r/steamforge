package com.steamforge.game.analytics

import com.steamforge.game.GameRunMode

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

    /** Backward-compatible overload for current NORMAL/DAILY callers. */
    fun gameStarted(
        runId: String,
        daily: Boolean,
        dailyType: String? = null,
    ) = gameStarted(
        runId = runId,
        mode = if (daily) GameRunMode.DAILY else GameRunMode.NORMAL,
        dailyType = dailyType,
    )

    fun gameStarted(
        runId: String,
        mode: GameRunMode,
        dailyType: String? = null,
    ) = AnalyticsEvent(
        GAME_STARTED,
        buildMap {
            put("run_id", runId.trim())
            putRunMode(mode)
            if (mode == GameRunMode.DAILY) {
                dailyType?.trim()?.takeIf { it.isNotEmpty() }?.let { put("daily_type", it) }
            }
        },
    )

    /** Backward-compatible overload for current NORMAL/DAILY callers. */
    fun gameFinished(
        runId: String,
        score: Int,
        maxTile: Int,
        moves: Int,
        daily: Boolean,
    ) = gameFinished(
        runId = runId,
        score = score,
        maxTile = maxTile,
        moves = moves,
        mode = if (daily) GameRunMode.DAILY else GameRunMode.NORMAL,
    )

    fun gameFinished(
        runId: String,
        score: Int,
        maxTile: Int,
        moves: Int,
        mode: GameRunMode,
    ) = AnalyticsEvent(
        GAME_FINISHED,
        buildMap {
            put("run_id", runId.trim())
            put("score", score.coerceAtLeast(0))
            put("max_tile", maxTile.coerceAtLeast(0))
            put("moves", moves.coerceAtLeast(0))
            putRunMode(mode)
        },
    )

    /** Backward-compatible overload for current NORMAL/DAILY callers. */
    fun gameRestarted(
        runId: String,
        score: Int,
        maxTile: Int,
        moves: Int,
        daily: Boolean,
    ) = gameRestarted(
        runId = runId,
        score = score,
        maxTile = maxTile,
        moves = moves,
        mode = if (daily) GameRunMode.DAILY else GameRunMode.NORMAL,
    )

    fun gameRestarted(
        runId: String,
        score: Int,
        maxTile: Int,
        moves: Int,
        mode: GameRunMode,
    ) = AnalyticsEvent(
        GAME_RESTARTED,
        buildMap {
            put("run_id", runId.trim())
            put("score", score.coerceAtLeast(0))
            put("max_tile", maxTile.coerceAtLeast(0))
            put("moves", moves.coerceAtLeast(0))
            putRunMode(mode)
        },
    )

    /** Backward-compatible overload for current NORMAL/DAILY callers. */
    fun merge(
        tileLevel: Int,
        tileValue: Int,
        moveNumber: Int,
        mergesInMove: Int,
        scoreGained: Int,
        daily: Boolean,
    ) = merge(
        tileLevel = tileLevel,
        tileValue = tileValue,
        moveNumber = moveNumber,
        mergesInMove = mergesInMove,
        scoreGained = scoreGained,
        mode = if (daily) GameRunMode.DAILY else GameRunMode.NORMAL,
    )

    fun merge(
        tileLevel: Int,
        tileValue: Int,
        moveNumber: Int,
        mergesInMove: Int,
        scoreGained: Int,
        mode: GameRunMode,
    ) = AnalyticsEvent(
        MERGE,
        buildMap {
            put("tile_level", tileLevel.coerceAtLeast(0))
            put("tile_value", tileValue.coerceAtLeast(0))
            put("move", moveNumber.coerceAtLeast(0))
            put("merges_in_move", mergesInMove.coerceAtLeast(0))
            put("score_gained", scoreGained.coerceAtLeast(0))
            putRunMode(mode)
        },
    )

    /** Backward-compatible overload for current NORMAL/DAILY callers. */
    fun highestTileUnlocked(
        tileLevel: Int,
        tileValue: Int,
        moveNumber: Int,
        daily: Boolean,
    ) = highestTileUnlocked(
        tileLevel = tileLevel,
        tileValue = tileValue,
        moveNumber = moveNumber,
        mode = if (daily) GameRunMode.DAILY else GameRunMode.NORMAL,
    )

    fun highestTileUnlocked(
        tileLevel: Int,
        tileValue: Int,
        moveNumber: Int,
        mode: GameRunMode,
    ) = AnalyticsEvent(
        HIGHEST_TILE_UNLOCKED,
        buildMap {
            put("tile_level", tileLevel.coerceAtLeast(0))
            put("tile_value", tileValue.coerceAtLeast(0))
            put("move", moveNumber.coerceAtLeast(0))
            putRunMode(mode)
        },
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

    private fun MutableMap<String, Any?>.putRunMode(mode: GameRunMode) {
        // Keep legacy boolean while dashboards migrate to the explicit three-state field.
        put("daily", mode.isDaily)
        put("run_mode", mode.wireName)
    }

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
