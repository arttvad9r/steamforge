package com.steamforge.game.config

import com.steamforge.game.progression.ContractType
import com.steamforge.game.progression.EventDefinition
import com.steamforge.game.progression.EventMilestone
import com.steamforge.game.progression.EventMetric
import com.steamforge.game.progression.EventReward
import com.steamforge.game.progression.EventScoringRule
import com.steamforge.game.progression.EventTheme
import com.steamforge.game.progression.ProgressionConfig

/**
 * Только безопасно настраиваемая поверхность. Fundamental 2048 rules, spawn RNG и Pressure/Overdrive
 * намеренно отсутствуют: remote config не должен менять честность core между запусками.
 */
data class EconomyTuning(
    val xpScoreDivisor: Int = 20,
    val xpPerMaxTileLevel: Int = 10,
    val winBonusXp: Int = 60,
    val baseXpToLevel: Int = 120,
    val xpGrowthPerLevel: Int = 40,
    val levelUpGemsBase: Int = 10,
    val levelUpGemsPerLevel: Int = 2,
    val freeUndosPerGame: Int = 2,
    val undoGemsCost: Int = 5,
    val wrenchGemsCost: Int = 10,
    val dailyRewardCycle: Int = 7,
    val dailyRewardGemsBase: Int = 5,
    val dailyRewardGemsStep: Int = 3,
) {
    fun applyTo(base: ProgressionConfig = ProgressionConfig()): ProgressionConfig = base.copy(
        xpScoreDivisor = xpScoreDivisor.coerceIn(1, 10_000),
        xpPerMaxTileLevel = xpPerMaxTileLevel.coerceIn(0, 1_000),
        winBonusXp = winBonusXp.coerceIn(0, 100_000),
        baseXpToLevel = baseXpToLevel.coerceIn(1, 1_000_000),
        xpGrowthPerLevel = xpGrowthPerLevel.coerceIn(0, 1_000_000),
        levelUpGemsBase = levelUpGemsBase.coerceIn(0, 100_000),
        levelUpGemsPerLevel = levelUpGemsPerLevel.coerceIn(0, 100_000),
        freeUndosPerGame = freeUndosPerGame.coerceIn(0, 10),
        undoGemsCost = undoGemsCost.coerceIn(0, 100_000),
        wrenchGemsCost = wrenchGemsCost.coerceIn(0, 100_000),
        dailyRewardCycle = dailyRewardCycle.coerceIn(1, 30),
        dailyRewardGemsBase = dailyRewardGemsBase.coerceIn(0, 100_000),
        dailyRewardGemsStep = dailyRewardGemsStep.coerceIn(0, 100_000),
    )
}

data class ContractTuning(
    val contractsPerDay: Int = 3,
    val targetScalePercent: Int = 100,
    val rewardScalePercent: Int = 100,
    val enabledTypes: Set<ContractType> = ContractType.entries.toSet(),
) {
    val safeContractsPerDay: Int get() = contractsPerDay.coerceIn(1, ContractType.entries.size)
    val safeTargetScalePercent: Int get() = targetScalePercent.coerceIn(50, 300)
    val safeRewardScalePercent: Int get() = rewardScalePercent.coerceIn(0, 500)
    val safeEnabledTypes: Set<ContractType> get() = enabledTypes.ifEmpty { ContractType.entries.toSet() }
}

data class FeatureFlags(
    val dailyChallenge: Boolean = true,
    val dailyContracts: Boolean = true,
    val weeklyChallenge: Boolean = true,
    val liveOps: Boolean = true,
    val rewardedAds: Boolean = true,
    val offers: Boolean = true,
)

data class EventTemplateConfig(
    val idPrefix: String,
    val durationDays: Int,
    val scoringRule: EventScoringRule,
    val milestones: List<EventMilestone>,
    val theme: EventTheme,
    val collection: String? = null,
    val featureFlags: Set<String> = emptySet(),
) {
    fun instantiateForEpochDay(epochDay: Long): EventDefinition {
        val duration = durationDays.coerceIn(1, 60).toLong()
        val start = epochDay - Math.floorMod(epochDay + 3L, duration)
        val safeMilestones = milestones
            .filter { it.targetPoints > 0 }
            .distinctBy { it.id }
            .sortedBy { it.targetPoints }
            .distinctBy { it.targetPoints }
            .ifEmpty { LocalDefaultConfig.foundryMilestones }
        return EventDefinition(
            id = "$idPrefix-$start",
            startEpochDay = start,
            endEpochDayExclusive = start + duration,
            scoringRule = scoringRule,
            milestones = safeMilestones,
            theme = theme,
            collection = collection,
            featureFlags = featureFlags,
        )
    }
}

data class RemoteGameConfig(
    val schemaVersion: Int = 1,
    val economy: EconomyTuning = EconomyTuning(),
    val contracts: ContractTuning = ContractTuning(),
    val rewardMultiplierPercent: Int = 100,
    val scheduledEvents: List<EventDefinition> = emptyList(),
    val fallbackEvent: EventTemplateConfig = LocalDefaultConfig.foundryTemplate,
    val features: FeatureFlags = FeatureFlags(),
) {
    val safeRewardMultiplierPercent: Int get() = rewardMultiplierPercent.coerceIn(0, 500)

    fun progressionConfig(): ProgressionConfig = economy.applyTo()

    fun scaleReward(base: Int): Int =
        ((base.coerceAtLeast(0).toLong() * safeRewardMultiplierPercent) / 100L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

    fun activeEvent(epochDay: Long): EventDefinition? {
        if (!features.liveOps) return null
        return scheduledEvents.firstOrNull { it.isActive(epochDay) } ?: fallbackEvent.instantiateForEpochDay(epochDay)
    }
}

object LocalDefaultConfig {
    val foundryMilestones = listOf(
        EventMilestone("pressure-100", 100, EventReward(gems = 5)),
        EventMilestone("pressure-250", 250, EventReward(gems = 8)),
        EventMilestone("pressure-500", 500, EventReward(gems = 12)),
        EventMilestone("pressure-900", 900, EventReward(gems = 18)),
        EventMilestone("pressure-1500", 1500, EventReward(gems = 30, blueprintPieces = 1)),
    )

    val foundryTemplate = EventTemplateConfig(
        idPrefix = "foundry-week",
        durationDays = 7,
        scoringRule = EventScoringRule(EventMetric.HIGH_MERGES, pointsPerUnit = 25),
        milestones = foundryMilestones,
        theme = EventTheme(
            id = "foundry",
            title = "FOUNDRY WEEK",
            subtitle = "Поддерживайте давление литейной",
            accent = "forge-orange",
            scoreLabel = "STEAM PRESSURE",
            compactUnit = "pressure",
            milestoneUnit = "PRESSURE",
            milestonesTitle = "РУБЕЖИ ЛИТЕЙНОЙ",
            rulesText = "Объединения плиток 64+ дают по 25 Steam Pressure. Давление копится в обычных партиях до конца события. Достигнутые рубежи можно забрать один раз до ротации события.",
        ),
        collection = "steam_engine",
        featureFlags = setOf("event_milestones", "high_merge_scoring"),
    )

    val value = RemoteGameConfig(fallbackEvent = foundryTemplate)
}
