package com.steamforge.game.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.analytics.Analytics
import com.steamforge.game.analytics.NoopAnalytics
import com.steamforge.game.config.FallbackGameConfigProvider
import com.steamforge.game.config.GameConfigProvider
import com.steamforge.game.config.LocalDefaultConfig
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.EventDefinition
import com.steamforge.game.progression.EventMilestone
import com.steamforge.game.progression.LiveOpsProgression
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.RewardTrackProgression
import com.steamforge.game.progression.RewardTrackSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EventMilestoneUi(
    val definition: EventMilestone,
    val claimed: Boolean,
    val claimable: Boolean,
)

data class EventUiState(
    val event: EventDefinition = LocalDefaultConfig.foundryTemplate.instantiateForEpochDay(0L),
    val points: Int = 0,
    val nextTarget: Int? = null,
    val daysRemaining: Int = 0,
    val gems: Int = 0,
    val milestones: List<EventMilestoneUi> = emptyList(),
    val rewardTrack: RewardTrackSnapshot = RewardTrackSnapshot(),
)

class EventViewModel(
    private val repo: DataRepo,
    private val configProvider: GameConfigProvider = FallbackGameConfigProvider(),
    private val today: () -> Long = { LocalDay.todayEpochDay() },
    private val analytics: Analytics = NoopAnalytics(),
) : ViewModel() {

    init {
        val day = today()
        configProvider.config.value.activeEvent(day)?.let { event ->
            analytics.logEvent(
                "event_entered",
                mapOf(
                    "event_id" to event.id,
                    "theme_id" to event.theme.id,
                    "surface" to "reward_track",
                    "track_levels" to event.milestones.size,
                    "days_remaining" to (event.endEpochDayExclusive - day).coerceAtLeast(0L).toInt(),
                ),
            )
        }
    }

    val ui: StateFlow<EventUiState> = combine(repo.progress, configProvider.config) { progress, remote ->
        val day = today()
        val event = remote.activeEvent(day) ?: LocalDefaultConfig.foundryTemplate.instantiateForEpochDay(day)
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
        val milestone = event.milestones.firstOrNull { it.id == rewardId } ?: return
        viewModelScope.launch {
            val granted = repo.claimEventMilestone(event, rewardId)
            if (granted) {
                analytics.logEvent(
                    "event_milestone",
                    mapOf(
                        "event_id" to event.id,
                        "milestone_id" to milestone.id,
                        "target_points" to milestone.targetPoints,
                        "event_points" to ui.value.points,
                        "reward_gems" to milestone.reward.gems,
                        "reward_blueprint_pieces" to milestone.reward.blueprintPieces,
                        "reward_cosmetic" to (milestone.reward.cosmeticId ?: "none"),
                    ),
                )
            }
        }
    }
}
