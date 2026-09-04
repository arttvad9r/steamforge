package com.steamforge.game.ui.workshop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.LevelInfo
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.ProgressionConfig
import com.steamforge.game.progression.WorkshopProgression
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkshopUiState(
    val loaded: Boolean = false,
    val level: Int = 1,
    val levelInfo: LevelInfo = LevelInfo(1, 0, 1),
    val gems: Int = 0,
    val workshopParts: Int = 0,
    val coreStage: Int = 0,
    val coreStageLabel: String = "СЛОМАНО",
    val nextCoreCost: Int? = null,
    val canUpgradeCore: Boolean = false,
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
) {
    val coreMaxed: Boolean get() = nextCoreCost == null
}

class WorkshopViewModel(
    private val repo: DataRepo,
    private val cfg: ProgressionConfig = ProgressionConfig(),
    private val today: () -> Long = { LocalDay.todayEpochDay() },
) : ViewModel() {

    val ui: StateFlow<WorkshopUiState> = repo.progress.map { p ->
        val todayDay = today()
        val canClaim = p.dailyRewardDay != todayDay
        val continuingStreak = if (p.dailyRewardDay == todayDay - 1) p.dailyRewardStreak else 0
        val nextDay = (continuingStreak % cfg.dailyRewardCycle) + 1
        val li = p.levelInfo(cfg)
        val coreStage = WorkshopProgression.normalizedCoreStage(p.workshopCoreStage, cfg)
        val nextCoreCost = WorkshopProgression.coreUpgradeCost(coreStage, cfg)
        WorkshopUiState(
            loaded = true,
            level = li.level,
            levelInfo = li,
            gems = p.gems,
            workshopParts = p.workshopParts,
            coreStage = coreStage,
            coreStageLabel = WorkshopProgression.coreStageLabel(coreStage, cfg),
            nextCoreCost = nextCoreCost,
            canUpgradeCore = nextCoreCost != null && p.workshopParts >= nextCoreCost,
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

    fun upgradeCore() {
        viewModelScope.launch {
            repo.updateProgress { p -> WorkshopProgression.upgradeCore(p, cfg) }
        }
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            repo.updateProgress { p ->
                val todayDay = today()
                if (p.dailyRewardDay == todayDay) return@updateProgress p
                val continuingStreak = if (p.dailyRewardDay == todayDay - 1) p.dailyRewardStreak else 0
                val day = (continuingStreak % cfg.dailyRewardCycle) + 1
                val reward = cfg.dailyRewardGems(day)
                val cosmetics = if (day == cfg.dailyRewardCycle) p.unlockedCosmetics + "gold_gauge" else p.unlockedCosmetics
                p.copy(
                    gems = p.gems + reward,
                    stats = p.stats.copy(gemsEarned = p.stats.gemsEarned + reward),
                    dailyRewardDay = todayDay,
                    dailyRewardStreak = day,
                    unlockedCosmetics = cosmetics,
                )
            }
        }
    }
}
