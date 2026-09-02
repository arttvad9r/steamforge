package com.steamforge.game.ui.workshop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.analytics.Analytics
import com.steamforge.game.analytics.NoopAnalytics
import com.steamforge.game.config.FallbackGameConfigProvider
import com.steamforge.game.config.GameConfigProvider
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.Blueprints
import com.steamforge.game.progression.LevelInfo
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.ProgressionConfig
import com.steamforge.game.progression.ReturnLoop
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkshopUiState(
    val loaded: Boolean = false,
    val level: Int = 1,
    val levelInfo: LevelInfo = LevelInfo(1, 0, 1),
    val gems: Int = 0,
    val bestScore: Int = 0,
    val gamesPlayed: Int = 0,
    val achievementsUnlocked: Int = 0,
    val dailyDone: Boolean = false,
    val dailyRewardAvailable: Boolean = false,
    val dailyRewardStreak: Int = 0,
    val dailyRewardDay: Int = 1,
    val dailyRewardGems: Int = 0,
    val dailyRewardUsesGrace: Boolean = false,
    val dailyRewardGraceAvailable: Boolean = true,
    val goldGaugeCosmetic: Boolean = false,
    val steamEngineUnlocked: Boolean = false,
    val animationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
)

class WorkshopViewModel(
    private val repo: DataRepo,
    private val configProvider: GameConfigProvider = FallbackGameConfigProvider(),
    private val baseCfg: ProgressionConfig = ProgressionConfig(),
    private val today: () -> Long = { LocalDay.todayEpochDay() },
    private val analytics: Analytics = NoopAnalytics(),
) : ViewModel() {

    private var lastDailyRewardImpressionDay: Long? = null

    val ui: StateFlow<WorkshopUiState> = combine(repo.progress, configProvider.config) { p, remote ->
        val cfg = remote.economy.applyTo(baseCfg)
        val todayDay = today()
        val plan = ReturnLoop.dailyRewardPlan(
            lastClaimDay = p.dailyRewardDay,
            streakDay = p.dailyRewardStreak,
            graceUsed = p.dailyRewardGraceUsed,
            today = todayDay,
            cycleDays = cfg.dailyRewardCycle,
        )
        val li = p.levelInfo(cfg)
        WorkshopUiState(
            loaded = true,
            level = li.level,
            levelInfo = li,
            gems = p.gems,
            bestScore = p.bestScore,
            gamesPlayed = p.stats.gamesPlayed,
            achievementsUnlocked = p.unlockedAchievements.size,
            dailyDone = p.dailyChallengeDay == todayDay && p.dailyChallengeDone,
            dailyRewardAvailable = plan.canClaim,
            dailyRewardStreak = plan.visibleStreak,
            dailyRewardDay = plan.rewardDay,
            dailyRewardGems = remote.scaleReward(cfg.dailyRewardGems(plan.rewardDay)),
            dailyRewardUsesGrace = plan.usesGrace,
            dailyRewardGraceAvailable = !p.dailyRewardGraceUsed,
            goldGaugeCosmetic = "gold_gauge" in p.unlockedCosmetics,
            steamEngineUnlocked = Blueprints.STEAM_ENGINE_UNLOCK in p.unlockedCosmetics,
            animationsEnabled = p.animationsEnabled,
            soundEnabled = p.soundEnabled,
            hapticsEnabled = p.hapticsEnabled,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkshopUiState())

    /** Called only by the real Workshop reward surface, not by onboarding presentation. */
    fun recordDailyRewardShown() {
        val state = ui.value
        if (!state.loaded || !state.dailyRewardAvailable) return
        val day = today()
        if (lastDailyRewardImpressionDay == day) return
        lastDailyRewardImpressionDay = day
        analytics.logEvent(
            "daily_reward_shown",
            mapOf(
                "reward_day" to state.dailyRewardDay,
                "reward_gems" to state.dailyRewardGems,
                "uses_grace" to state.dailyRewardUsesGrace,
            ),
        )
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            val remote = configProvider.config.value
            val cfg = remote.economy.applyTo(baseCfg)
            var claimAnalytics: Map<String, Any?>? = null
            repo.updateProgress { p ->
                val todayDay = today()
                val plan = ReturnLoop.dailyRewardPlan(
                    lastClaimDay = p.dailyRewardDay,
                    streakDay = p.dailyRewardStreak,
                    graceUsed = p.dailyRewardGraceUsed,
                    today = todayDay,
                    cycleDays = cfg.dailyRewardCycle,
                )
                if (!plan.canClaim) return@updateProgress p

                val reward = remote.scaleReward(cfg.dailyRewardGems(plan.rewardDay))
                val cosmetics = if (plan.rewardDay == cfg.dailyRewardCycle) {
                    p.unlockedCosmetics + "gold_gauge"
                } else {
                    p.unlockedCosmetics
                }
                val updated = p.copy(
                    gems = p.gems + reward,
                    stats = p.stats.copy(gemsEarned = p.stats.gemsEarned + reward),
                    dailyRewardDay = todayDay,
                    dailyRewardStreak = plan.rewardDay,
                    dailyRewardGraceUsed = plan.graceUsedAfterClaim,
                    unlockedCosmetics = cosmetics,
                )
                claimAnalytics = mapOf(
                    "reward_day" to plan.rewardDay,
                    "reward_gems" to reward,
                    "uses_grace" to plan.usesGrace,
                    "cycle_complete" to (plan.rewardDay == cfg.dailyRewardCycle),
                    "gem_balance" to updated.gems,
                )
                updated
            }
            claimAnalytics?.let { analytics.logEvent("daily_reward_claimed", it) }
        }
    }
}
