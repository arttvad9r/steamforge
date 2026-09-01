package com.steamforge.game.ui.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.Blueprints
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.WeeklyChallenge
import com.steamforge.game.progression.WeeklyChallenges
import com.steamforge.game.progression.WeeklyReplayCodec
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeeklyUiState(
    val challenge: WeeklyChallenge = WeeklyChallenges.forEpochDay(0L),
    val bestScore: Int = 0,
    val bestMoveCount: Int = 0,
    val rewardClaimed: Boolean = false,
    val daysRemaining: Int = 0,
    val gems: Int = 0,
    val blueprintsCollected: Int = 0,
    val blueprintsTotal: Int = Blueprints.steamEngine.pieces.size,
) {
    val hasVerifiedRun: Boolean get() = bestScore > 0
    val rewardAvailable: Boolean get() = hasVerifiedRun && !rewardClaimed
}

class WeeklyViewModel(
    private val repo: DataRepo,
    private val today: () -> Long = { LocalDay.todayEpochDay() },
) : ViewModel() {

    val ui: StateFlow<WeeklyUiState> = repo.progress.map { progress ->
        val day = today()
        val challenge = WeeklyChallenges.forEpochDay(day)
        val record = progress.weekly.takeIf { it.challengeId == challenge.id }
        WeeklyUiState(
            challenge = challenge,
            bestScore = record?.bestScore ?: 0,
            bestMoveCount = record?.bestMoves?.let(WeeklyReplayCodec::decode)?.size ?: 0,
            rewardClaimed = record?.rewardClaimed ?: false,
            daysRemaining = WeeklyChallenges.daysRemaining(challenge, day),
            gems = progress.gems,
            blueprintsCollected = Blueprints.collectedCount(Blueprints.steamEngine, progress.blueprintPieces),
            blueprintsTotal = Blueprints.steamEngine.pieces.size,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        WeeklyUiState(),
    )

    fun claimReward() {
        val challenge = ui.value.challenge
        if (!ui.value.rewardAvailable) return
        viewModelScope.launch { repo.claimWeeklyReward(challenge) }
    }
}
