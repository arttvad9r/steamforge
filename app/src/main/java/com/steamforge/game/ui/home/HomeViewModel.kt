package com.steamforge.game.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.ProgressionConfig
import com.steamforge.game.progression.continuingDailyRewardStreak
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeFeatureVisibility(
    val showStatusRail: Boolean = false,
    val showWorkshop: Boolean = false,
    val showContracts: Boolean = false,
    val showDaily: Boolean = false,
    val showCollection: Boolean = false,
)

internal fun homeFeatureVisibility(
    gamesPlayed: Int,
    activeRunMerges: Int,
    hasBlueprintPieces: Boolean,
): HomeFeatureVisibility {
    val completedRun = gamesPlayed > 0
    val meaningfulProgress = completedRun || activeRunMerges > 0 || hasBlueprintPieces
    return HomeFeatureVisibility(
        showStatusRail = meaningfulProgress,
        showWorkshop = meaningfulProgress,
        showContracts = completedRun,
        showDaily = completedRun,
        showCollection = completedRun || hasBlueprintPieces,
    )
}

data class HomeUiState(
    val loaded: Boolean = false,
    val gems: Int = 0,
    val bestScore: Int = 0,
    val workshopLevel: Int = 1,
    val achievementsUnlocked: Int = 0,
    val dailyDone: Boolean = false,
    val dailyRewardStreak: Int = 0,
    val hasSavedRun: Boolean = false,
    val featureVisibility: HomeFeatureVisibility = HomeFeatureVisibility(),
)

class HomeViewModel(
    repo: DataRepo,
    private val cfg: ProgressionConfig = ProgressionConfig(),
    private val today: () -> Long = { LocalDay.todayEpochDay() },
) : ViewModel() {

    val ui: StateFlow<HomeUiState> = combine(repo.progress, repo.savedGame) { progress, savedGame ->
        val todayDay = today()
        HomeUiState(
            loaded = true,
            gems = progress.gems,
            bestScore = progress.bestScore,
            workshopLevel = progress.levelInfo(cfg).level,
            achievementsUnlocked = progress.unlockedAchievements.size,
            dailyDone = progress.dailyChallengeDay == todayDay && progress.dailyChallengeDone,
            dailyRewardStreak = continuingDailyRewardStreak(
                lastClaimDay = progress.dailyRewardDay,
                storedStreak = progress.dailyRewardStreak,
                today = todayDay,
            ),
            hasSavedRun = savedGame != null,
            featureVisibility = homeFeatureVisibility(
                gamesPlayed = progress.stats.gamesPlayed,
                activeRunMerges = savedGame?.mergesTotal ?: 0,
                hasBlueprintPieces = progress.blueprintPieces.isNotEmpty(),
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
