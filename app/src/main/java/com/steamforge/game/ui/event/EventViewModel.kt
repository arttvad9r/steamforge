package com.steamforge.game.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.EventDefinition
import com.steamforge.game.progression.EventMilestone
import com.steamforge.game.progression.LiveOpsCatalog
import com.steamforge.game.progression.LiveOpsProgression
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.RewardTrackProgression
import com.steamforge.game.progression.RewardTrackSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EventMilestoneUi(
    val definition: EventMilestone,
    val claimed: Boolean,
    val claimable: Boolean,
)

data class EventUiState(
    val event: EventDefinition = LiveOpsCatalog.activeForEpochDay(0L),
    val points: Int = 0,
    val nextTarget: Int? = null,
    val daysRemaining: Int = 0,
    val gems: Int = 0,
    val milestones: List<EventMilestoneUi> = emptyList(),
    val rewardTrack: RewardTrackSnapshot = RewardTrackSnapshot(),
)

class EventViewModel(
    private val repo: DataRepo,
    private val today: () -> Long = { LocalDay.todayEpochDay() },
) : ViewModel() {

    val ui: StateFlow<EventUiState> = repo.progress.map { progress ->
        val day = today()
        val event = LiveOpsCatalog.activeForEpochDay(day)
        val ledger = LiveOpsProgression.normalized(progress.liveOps, event)
        val track = RewardTrackProgression.forEvent(event)
        val trackSnapshot = RewardTrackProgression.snapshot(
            definition = track,
            progress = ledger.totalPoints,
            claimedFreeIds = ledger.claimedMilestones,
        )
        EventUiState(
            event = event,
            points = ledger.totalPoints,
            nextTarget = trackSnapshot.nextLevel?.progressRequirement,
            daysRemaining = (event.endEpochDayExclusive - day).coerceAtLeast(0L).toInt(),
            gems = progress.gems,
            milestones = event.milestones.map { milestone ->
                EventMilestoneUi(
                    definition = milestone,
                    claimed = milestone.id in ledger.claimedMilestones,
                    claimable = LiveOpsProgression.canClaim(ledger, event, milestone),
                )
            },
            rewardTrack = trackSnapshot,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventUiState())

    fun claim(rewardId: String) {
        val event = ui.value.event
        viewModelScope.launch { repo.claimEventMilestone(event, rewardId) }
    }
}
