package com.steamforge.game.progression

import kotlin.math.max

enum class EventMetric {
    HIGH_MERGES,
    TOTAL_MERGES,
    SCORE,
    MOVES,
    OVERDRIVES,
}

data class EventScoringRule(
    val metric: EventMetric,
    val pointsPerUnit: Int = 1,
    val unitsPerStep: Int = 1,
) {
    init {
        require(pointsPerUnit > 0)
        require(unitsPerStep > 0)
    }
}

data class EventReward(
    val gems: Int = 0,
    val blueprintPieces: Int = 0,
    val cosmeticId: String? = null,
) {
    init {
        require(gems >= 0)
        require(blueprintPieces >= 0)
    }
}

data class EventMilestone(
    val id: String,
    val targetPoints: Int,
    val reward: EventReward,
) {
    init {
        require(id.isNotBlank())
        require(targetPoints > 0)
    }
}

data class EventTheme(
    val id: String,
    val title: String,
    val subtitle: String,
    val accent: String,
)

data class EventDefinition(
    val id: String,
    val startEpochDay: Long,
    val endEpochDayExclusive: Long,
    val scoringRule: EventScoringRule,
    val milestones: List<EventMilestone>,
    val theme: EventTheme,
    val collection: String? = null,
    val featureFlags: Set<String> = emptySet(),
) {
    init {
        require(id.isNotBlank())
        require(endEpochDayExclusive > startEpochDay)
        require(milestones.isNotEmpty())
        require(milestones.map { it.id }.distinct().size == milestones.size)
        require(milestones.zipWithNext().all { (a, b) -> a.targetPoints < b.targetPoints })
    }

    fun isActive(epochDay: Long): Boolean = epochDay in startEpochDay until endEpochDayExclusive
}

data class EventRunCounters(
    val score: Int = 0,
    val merges: Int = 0,
    val moves: Int = 0,
    val highMerges: Int = 0,
    val overdrives: Int = 0,
)

data class LiveOpsLedger(
    val eventId: String = "",
    val totalPoints: Int = 0,
    val claimedMilestones: Set<String> = emptySet(),
    val activeRunSeed: Long? = null,
    val activeRunPoints: Int = 0,
)

object LiveOpsCatalog {
    private const val DAYS_PER_WEEK = 7L

    /**
     * Offline fallback catalog. A production Remote Config/backend can replace only this catalog source;
     * progression, persistence and UI keep consuming EventDefinition unchanged.
     */
    fun activeForEpochDay(epochDay: Long): EventDefinition {
        val start = epochDay - Math.floorMod(epochDay + 3L, DAYS_PER_WEEK)
        return foundryWeek(start)
    }

    fun foundryWeek(startEpochDay: Long): EventDefinition = EventDefinition(
        id = "foundry-week-$startEpochDay",
        startEpochDay = startEpochDay,
        endEpochDayExclusive = startEpochDay + DAYS_PER_WEEK,
        scoringRule = EventScoringRule(
            metric = EventMetric.HIGH_MERGES,
            pointsPerUnit = 25,
        ),
        milestones = listOf(
            EventMilestone("pressure-100", 100, EventReward(gems = 5)),
            EventMilestone("pressure-250", 250, EventReward(gems = 8)),
            EventMilestone("pressure-500", 500, EventReward(gems = 12)),
            EventMilestone("pressure-900", 900, EventReward(gems = 18)),
            EventMilestone("pressure-1500", 1500, EventReward(gems = 30, blueprintPieces = 1)),
        ),
        theme = EventTheme(
            id = "foundry",
            title = "FOUNDRY WEEK",
            subtitle = "Поддерживайте давление литейной",
            accent = "forge-orange",
        ),
        collection = "steam_engine",
        featureFlags = setOf("event_milestones", "high_merge_scoring"),
    )
}

object LiveOpsProgression {
    fun normalized(ledger: LiveOpsLedger, event: EventDefinition): LiveOpsLedger =
        if (ledger.eventId == event.id) ledger else LiveOpsLedger(eventId = event.id)

    fun pointsFor(rule: EventScoringRule, counters: EventRunCounters): Int {
        val units = when (rule.metric) {
            EventMetric.HIGH_MERGES -> counters.highMerges
            EventMetric.TOTAL_MERGES -> counters.merges
            EventMetric.SCORE -> counters.score
            EventMetric.MOVES -> counters.moves
            EventMetric.OVERDRIVES -> counters.overdrives
        }.coerceAtLeast(0)
        return (units / rule.unitsPerStep) * rule.pointsPerUnit
    }

    fun recordLiveSnapshot(
        ledger: LiveOpsLedger,
        event: EventDefinition,
        runSeed: Long,
        counters: EventRunCounters,
    ): LiveOpsLedger {
        val current = normalized(ledger, event)
        val runPoints = pointsFor(event.scoringRule, counters)
        val sameRun = current.activeRunSeed == runSeed
        val baseline = if (sameRun) current.activeRunPoints else 0
        val delta = (runPoints - baseline).coerceAtLeast(0)
        return current.copy(
            totalPoints = current.totalPoints + delta,
            activeRunSeed = runSeed,
            activeRunPoints = if (sameRun) max(current.activeRunPoints, runPoints) else runPoints,
        )
    }

    fun recordFinishedRun(
        ledger: LiveOpsLedger,
        event: EventDefinition,
        runSeed: Long,
        counters: EventRunCounters,
    ): LiveOpsLedger {
        val current = normalized(ledger, event)
        val preservedSeed = current.activeRunSeed?.takeIf { it != runSeed }
        val preservedPoints = if (preservedSeed != null) current.activeRunPoints else 0
        val updated = recordLiveSnapshot(current, event, runSeed, counters)
        return if (preservedSeed != null) {
            updated.copy(activeRunSeed = preservedSeed, activeRunPoints = preservedPoints)
        } else {
            updated.copy(activeRunSeed = null, activeRunPoints = 0)
        }
    }

    fun canClaim(ledger: LiveOpsLedger, event: EventDefinition, milestone: EventMilestone): Boolean {
        val current = normalized(ledger, event)
        return milestone.id !in current.claimedMilestones && current.totalPoints >= milestone.targetPoints
    }

    fun markClaimed(
        ledger: LiveOpsLedger,
        event: EventDefinition,
        milestone: EventMilestone,
    ): LiveOpsLedger? {
        val current = normalized(ledger, event)
        if (!canClaim(current, event, milestone)) return null
        return current.copy(claimedMilestones = current.claimedMilestones + milestone.id)
    }

    fun nextMilestone(ledger: LiveOpsLedger, event: EventDefinition): EventMilestone? {
        val current = normalized(ledger, event)
        return event.milestones.firstOrNull { it.targetPoints > current.totalPoints }
    }
}
