package com.steamforge.game.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.config.FallbackGameConfigProvider
import com.steamforge.game.config.GameConfigProvider
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.Blueprints
import com.steamforge.game.progression.EventTheme
import com.steamforge.game.progression.LiveOpsCatalog
import com.steamforge.game.progression.LiveOpsProgression
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.ReturnLoop
import com.steamforge.game.progression.WeeklyChallenges
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val loaded: Boolean = false,
    val gems: Int = 0,
    val bestScore: Int = 0,
    val workshopLevel: Int = 1,
    val blueprintsCollected: Int = 0,
    val blueprintsTotal: Int = Blueprints.steamEngine.pieces.size,
    val dailyDone: Boolean = false,
    val dailyRewardStreak: Int = 0,
    val weeklyBestScore: Int = 0,
    val weeklyRewardAvailable: Boolean = false,
    val weeklyDaysRemaining: Int = 0,
    val eventPoints: Int = 0,
    val eventRewardAvailable: Boolean = false,
    val eventDaysRemaining: Int = 0,
    val eventTheme: EventTheme? = null,
    val dailyEnabled: Boolean = true,
    val contractsEnabled: Boolean = true,
    val weeklyEnabled: Boolean = true,
    val eventEnabled: Boolean = true,
    val hasSavedRun: Boolean = false,
)

class HomeViewModel(
    repo: DataRepo,
    private val configProvider: GameConfigProvider = FallbackGameConfigProvider(),
    private val today: () -> Long = { LocalDay.todayEpochDay() },
) : ViewModel() {

    val ui: StateFlow<HomeUiState> = combine(repo.progress, repo.savedGame, configProvider.config) { progress, savedGame, remote ->
        val cfg = remote.progressionConfig()
        val todayDay = today()
        val weekly = WeeklyChallenges.forEpochDay(todayDay)
        val weeklyRecord = progress.weekly.takeIf { it.challengeId == weekly.id }
        val event = LiveOpsCatalog.activeForEpochDay(todayDay)
        val eventLedger = LiveOpsProgression.normalized(progress.liveOps, event)
        HomeUiState(
            loaded = true,
            gems = progress.gems,
            bestScore = progress.bestScore,
            workshopLevel = progress.levelInfo(cfg).level,
            blueprintsCollected = Blueprints.collectedCount(Blueprints.steamEngine, progress.blueprintPieces),
            blueprintsTotal = Blueprints.steamEngine.pieces.size,
            dailyDone = progress.dailyChallengeDay == todayDay && progress.dailyChallengeDone,
            dailyRewardStreak = ReturnLoop.visibleStreak(
                lastClaimDay = progress.dailyRewardDay,
                streakDay = progress.dailyRewardStreak,
                graceUsed = progress.dailyRewardGraceUsed,
                today = todayDay,
                cycleDays = cfg.dailyRewardCycle,
            ),
            weeklyBestScore = weeklyRecord?.bestScore ?: 0,
            weeklyRewardAvailable = weeklyRecord?.let { it.bestScore > 0 && !it.rewardClaimed } ?: false,
            weeklyDaysRemaining = WeeklyChallenges.daysRemaining(weekly, todayDay),
            eventPoints = eventLedger.totalPoints,
            eventRewardAvailable = event.milestones.any { LiveOpsProgression.canClaim(eventLedger, event, it) },
            eventDaysRemaining = (event.endEpochDayExclusive - todayDay).coerceAtLeast(0L).toInt(),
            eventTheme = event.theme.takeIf { remote.features.liveOps },
            dailyEnabled = remote.features.dailyChallenge,
            contractsEnabled = remote.features.dailyContracts,
            weeklyEnabled = remote.features.weeklyChallenge,
            eventEnabled = remote.features.liveOps,
            hasSavedRun = savedGame != null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
