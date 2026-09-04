package com.steamforge.game.ui.workshop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.analytics.Analytics
import com.steamforge.game.analytics.AnalyticsEvent
import com.steamforge.game.analytics.AnalyticsEvents
import com.steamforge.game.analytics.NoopAnalytics
import com.steamforge.game.analytics.log
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.BlueprintCollections
import com.steamforge.game.progression.LevelInfo
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.ProgressionConfig
import com.steamforge.game.progression.Reward
import com.steamforge.game.progression.RewardSystem
import com.steamforge.game.progression.WorkshopMechanism
import com.steamforge.game.progression.WorkshopProgression
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkshopMechanismUi(
    val mechanism: WorkshopMechanism,
    val stage: Int,
    val stageLabel: String,
    val nextCost: Int?,
    val canUpgrade: Boolean,
)

data class WorkshopUiState(
    val loaded: Boolean = false,
    val level: Int = 1,
    val levelInfo: LevelInfo = LevelInfo(1, 0, 1),
    val gems: Int = 0,
    val workshopParts: Int = 0,
    val mechanisms: List<WorkshopMechanismUi> = emptyList(),
    val coreStage: Int = 0,
    val coreStageLabel: String = "СЛОМАНО",
    val pressureStage: Int = 0,
    val gearPressStage: Int = 0,
    val steamEnginePieces: Int = 0,
    val steamEnginePiecesTotal: Int = BlueprintCollections.steamEngine.pieces.size,
    val steamEngineUnlocked: Boolean = false,
    val bestScore: Int = 0,
    val gamesPlayed: Int = 0,
    val achievementsUnlocked: Int = 0,
    val dailyDone: Boolean = false,
    val dailyRewardAvailable: Boolean = false,
    val dailyRewardStreak: Int = 0,
    val dailyRewardDay: Int = 1,
    val dailyRewardGems: Int = 0,
    val goldGaugeCosmetic: Boolean = false,
    val animationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
)

class WorkshopViewModel(
    private val repo: DataRepo,
    private val cfg: ProgressionConfig = ProgressionConfig(),
    private val today: () -> Long = { LocalDay.todayEpochDay() },
    private val analytics: Analytics = NoopAnalytics(),
) : ViewModel() {

    val ui: StateFlow<WorkshopUiState> = repo.progress.map { p ->
        val todayDay = today()
        val canClaim = p.dailyRewardDay != todayDay
        val continuingStreak = if (p.dailyRewardDay == todayDay - 1) p.dailyRewardStreak else 0
        val cycle = cfg.dailyRewardCycle.coerceAtLeast(1)
        val nextDay = (continuingStreak % cycle) + 1
        val li = p.levelInfo(cfg)
        val mechanisms = WorkshopMechanism.entries.map { mechanism ->
            val stage = WorkshopProgression.mechanismStage(p, mechanism, cfg)
            val nextCost = WorkshopProgression.mechanismUpgradeCost(stage, cfg)
            WorkshopMechanismUi(
                mechanism = mechanism,
                stage = stage,
                stageLabel = WorkshopProgression.mechanismStageLabel(stage, cfg),
                nextCost = nextCost,
                canUpgrade = nextCost != null && p.workshopParts >= nextCost,
            )
        }
        val core = mechanisms.first { it.mechanism == WorkshopMechanism.CORE }
        val pressure = mechanisms.first { it.mechanism == WorkshopMechanism.PRESSURE_GENERATOR }
        val press = mechanisms.first { it.mechanism == WorkshopMechanism.GEAR_PRESS }
        val steamEnginePieces = BlueprintCollections.ownedCount(BlueprintCollections.steamEngine, p.blueprintPieces)
        val steamEngineUnlocked = BlueprintCollections.isSteamEngineComplete(p.blueprintPieces)
        WorkshopUiState(
            loaded = true,
            level = li.level,
            levelInfo = li,
            gems = p.gems,
            workshopParts = p.workshopParts,
            mechanisms = mechanisms,
            coreStage = core.stage,
            coreStageLabel = core.stageLabel,
            pressureStage = pressure.stage,
            gearPressStage = press.stage,
            steamEnginePieces = steamEnginePieces,
            steamEnginePiecesTotal = BlueprintCollections.steamEngine.pieces.size,
            steamEngineUnlocked = steamEngineUnlocked,
            bestScore = p.bestScore,
            gamesPlayed = p.stats.gamesPlayed,
            achievementsUnlocked = p.unlockedAchievements.size,
            dailyDone = p.dailyChallengeDay == todayDay && p.dailyChallengeDone,
            dailyRewardAvailable = canClaim,
            dailyRewardStreak = continuingStreak,
            dailyRewardDay = nextDay,
            dailyRewardGems = cfg.dailyRewardGems(nextDay),
            goldGaugeCosmetic = "gold_gauge" in p.unlockedCosmetics,
            animationsEnabled = p.animationsEnabled,
            soundEnabled = p.soundEnabled,
            hapticsEnabled = p.hapticsEnabled,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkshopUiState())

    fun upgradeMechanism(mechanism: WorkshopMechanism) {
        viewModelScope.launch {
            var upgradeEvent: AnalyticsEvent? = null
            var economyEvent: AnalyticsEvent? = null
            repo.updateProgress { p ->
                val fromStage = WorkshopProgression.mechanismStage(p, mechanism, cfg)
                val cost = WorkshopProgression.mechanismUpgradeCost(fromStage, cfg)
                val updated = WorkshopProgression.upgradeMechanism(p, mechanism, cfg)
                val toStage = WorkshopProgression.mechanismStage(updated, mechanism, cfg)
                if (toStage > fromStage && cost != null) {
                    upgradeEvent = AnalyticsEvents.workshopUpgrade(
                        mechanism = mechanism.name,
                        fromStage = fromStage,
                        toStage = toStage,
                        partsSpent = cost,
                    )
                    val spentParts = (
                        p.workshopParts.coerceAtLeast(0) - updated.workshopParts.coerceAtLeast(0)
                    ).coerceAtLeast(0)
                    if (spentParts > 0) {
                        economyEvent = AnalyticsEvents.resourceSpent(
                            resourceType = "workshop_parts",
                            source = "workshop_upgrade",
                            amount = spentParts,
                            balanceAfter = updated.workshopParts,
                        )
                    }
                }
                updated
            }
            upgradeEvent?.let { analytics.log(it) }
            economyEvent?.let { analytics.log(it) }
        }
    }

    fun upgradeCore() = upgradeMechanism(WorkshopMechanism.CORE)

    fun claimDailyReward() {
        viewModelScope.launch {
            repo.updateProgress { p ->
                val todayDay = today()
                if (p.dailyRewardDay == todayDay) return@updateProgress p
                val continuingStreak = if (p.dailyRewardDay == todayDay - 1) p.dailyRewardStreak else 0
                val nextStreak = continuingStreak + 1
                val cycle = cfg.dailyRewardCycle.coerceAtLeast(1)
                val rewardDay = ((nextStreak - 1) % cycle) + 1
                val rewards = buildList<Reward> {
                    add(Reward.Gems(cfg.dailyRewardGems(rewardDay)))
                    if (rewardDay == cycle) add(Reward.CosmeticUnlock("gold_gauge"))
                }
                val (rewarded, _) = RewardSystem.apply(p, rewards)
                rewarded.copy(
                    dailyRewardDay = todayDay,
                    dailyRewardStreak = nextStreak,
                    stats = rewarded.stats.copy(
                        highestDailyStreak = maxOf(
                            rewarded.stats.highestDailyStreak,
                            p.dailyRewardStreak,
                            nextStreak,
                        ),
                    ),
                )
            }
        }
    }
}
