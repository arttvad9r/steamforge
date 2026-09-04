package com.steamforge.game.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.PermanentProfile
import com.steamforge.game.progression.PermanentProfileSnapshot
import com.steamforge.game.progression.ProgressionConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(
    val loaded: Boolean = false,
    val profile: PermanentProfileSnapshot = PermanentProfileSnapshot(
        level = 1,
        gamesPlayed = 0,
        totalScore = 0L,
        bestScore = 0,
        highestTile = 0,
        totalMerges = 0,
        largestCombo = 0,
        highestDailyStreak = 0,
        collectionsCompleted = 0,
        collectionsTotal = 0,
        workshopStagesCompleted = 0,
        workshopStagesTotal = 0,
        achievementsUnlocked = 0,
    ),
)

class ProfileViewModel(
    repo: DataRepo,
    cfg: ProgressionConfig = ProgressionConfig(),
) : ViewModel() {
    val ui: StateFlow<ProfileUiState> = repo.progress
        .map { progress ->
            ProfileUiState(
                loaded = true,
                profile = PermanentProfile.snapshot(progress, cfg),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())
}
