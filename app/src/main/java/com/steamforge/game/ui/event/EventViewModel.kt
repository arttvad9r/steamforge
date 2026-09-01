package com.steamforge.game.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.EventDefinition
import com.steamforge.game.progression.EventMilestone
import com.steamforge.game.progression.LiveOpsCatalog
import com.steamforge.game.progression.LiveOpsProgression
import com.steamforge.game.progression.LocalDay
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
)

class EventViewModel(
    private val repo: DataRepo,
    private val today: () -> Long = { LocalDay.todayEpochDay() },
) : ViewModel() {

    val ui: StateFlow<EventUiState> = repo.progress.map { progress ->
        val day = today()
        val event = LiveOpsCatalog.activeForEpochDay(day)
        val ledger = LiveOpsProgression.normalized(progress.liveOps, event)
        EventUiState(
            event = event,
            points = ledger.totalPoints,
            nextTarget = LiveOpsProgression.nextMilestone(ledger, event)?.targetPoints,
            daysRemaining = (event.endEpochDayExclusive - day).coerceAtLeast(0L).toInt(),
            gems = progress.gems,
            milestones = event.milestones.map { milestone ->
                EventMilestoneUi(
                    definition = milestone,
                    claimed = milestone.id in ledger.claimedMilestones,
                    claimable = LiveOpsProgression.canClaim(ledger, event, milestone),
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventUiState())

    fun claim(milestoneId: String) {
        val event = ui.value.event
        viewModelScope.launch { repo.claimEventMilestone(event, milestoneId) }
    }
}
