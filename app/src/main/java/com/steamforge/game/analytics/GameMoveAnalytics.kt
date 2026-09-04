package com.steamforge.game.analytics

import com.steamforge.game.core.MoveResult

/**
 * Converts an already-calculated core move result into product analytics events.
 * The game engine stays free of analytics dependencies; this adapter only observes immutable results.
 */
object GameMoveAnalytics {
    fun eventsFor(
        result: MoveResult,
        previousMaxLevel: Int,
        daily: Boolean,
    ): List<AnalyticsEvent> {
        if (!result.moved || result.merges.isEmpty()) return emptyList()

        val mergeCount = result.merges.size
        val moveNumber = result.state.moves
        val mergeEvents = result.merges.map { merge ->
            AnalyticsEvents.merge(
                tileLevel = merge.tile.level,
                tileValue = merge.tile.value,
                moveNumber = moveNumber,
                mergesInMove = mergeCount,
                scoreGained = result.scoreGained,
                daily = daily,
            )
        }
        val unlockedEvents = result.merges
            .asSequence()
            .map { it.tile.level }
            .filter { it > previousMaxLevel }
            .distinct()
            .sorted()
            .map { level ->
                AnalyticsEvents.highestTileUnlocked(
                    tileLevel = level,
                    tileValue = tileValue(level),
                    moveNumber = moveNumber,
                    daily = daily,
                )
            }
            .toList()

        return mergeEvents + unlockedEvents
    }

    private fun tileValue(level: Int): Int = when {
        level <= 0 -> 0
        level >= 30 -> 1 shl 30
        else -> 1 shl level
    }
}
