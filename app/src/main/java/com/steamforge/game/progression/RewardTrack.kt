package com.steamforge.game.progression

/**
 * Store- and event-neutral reward payload for long-form progression tracks.
 * Premium rewards are modelled here, but no premium entitlement is introduced by Reward Track v1.
 */
data class RewardTrackReward(
    val gems: Int = 0,
    val blueprintPieces: Int = 0,
    val cosmeticId: String? = null,
) {
    init {
        require(gems >= 0)
        require(blueprintPieces >= 0)
    }
}

data class RewardTrackLevel(
    val id: String,
    val level: Int,
    val progressRequirement: Int,
    val freeReward: RewardTrackReward,
    val premiumReward: RewardTrackReward? = null,
) {
    init {
        require(id.isNotBlank())
        require(level > 0)
        require(progressRequirement > 0)
    }
}

data class RewardTrackDefinition(
    val id: String,
    val levels: List<RewardTrackLevel>,
) {
    init {
        require(id.isNotBlank())
        require(levels.map { it.id }.distinct().size == levels.size)
        require(levels.map { it.level }.distinct().size == levels.size)
        require(levels.zipWithNext().all { (a, b) ->
            a.level < b.level && a.progressRequirement < b.progressRequirement
        })
    }
}

data class RewardTrackLevelState(
    val definition: RewardTrackLevel,
    val freeClaimed: Boolean,
    val freeClaimable: Boolean,
)

data class RewardTrackSnapshot(
    val definition: RewardTrackDefinition = RewardTrackDefinition(id = "empty", levels = emptyList()),
    val progress: Int = 0,
    val levels: List<RewardTrackLevelState> = emptyList(),
    val nextLevel: RewardTrackLevel? = null,
)

object RewardTrackProgression {
    /**
     * Event milestones are the first Reward Track consumer. Their persisted IDs stay unchanged, so the
     * existing LiveOps ledger remains migration-free and event claims stay atomic in DataRepo.
     */
    fun forEvent(event: EventDefinition): RewardTrackDefinition = RewardTrackDefinition(
        id = "${event.id}-reward-track",
        levels = event.milestones.mapIndexed { index, milestone ->
            RewardTrackLevel(
                id = milestone.id,
                level = index + 1,
                progressRequirement = milestone.targetPoints,
                freeReward = milestone.reward.toTrackReward(),
            )
        },
    )

    fun snapshot(
        definition: RewardTrackDefinition,
        progress: Int,
        claimedFreeIds: Set<String>,
    ): RewardTrackSnapshot {
        val safeProgress = progress.coerceAtLeast(0)
        val levels = definition.levels.map { level ->
            RewardTrackLevelState(
                definition = level,
                freeClaimed = level.id in claimedFreeIds,
                freeClaimable = level.id !in claimedFreeIds && safeProgress >= level.progressRequirement,
            )
        }
        return RewardTrackSnapshot(
            definition = definition,
            progress = safeProgress,
            levels = levels,
            nextLevel = definition.levels.firstOrNull { it.progressRequirement > safeProgress },
        )
    }
}

private fun EventReward.toTrackReward(): RewardTrackReward = RewardTrackReward(
    gems = gems,
    blueprintPieces = blueprintPieces,
    cosmeticId = cosmeticId,
)
