package com.steamforge.game.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.AchievementDef
import com.steamforge.game.progression.Achievements
import com.steamforge.game.progression.ProgressionConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AchievementUi(
    val def: AchievementDef,
    val unlocked: Boolean,
    val unlockDate: String?,
    val progress: Int,
)

class AchievementsViewModel(
    repo: DataRepo,
    private val cfg: ProgressionConfig = ProgressionConfig(),
) : ViewModel() {

    val ui: StateFlow<List<AchievementUi>> = repo.progress.map { p ->
        Achievements.all.map { def ->
            val unlocked = def.id in p.unlockedAchievements
            AchievementUi(
                def = def,
                unlocked = unlocked,
                unlockDate = p.achievementDays[def.id]?.let(::formatDay),
                progress = if (unlocked) def.maxProgress else def.progressOf(p.stats).coerceIn(0, def.maxProgress),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        /** epochDay -> "dd.MM.yyyy". Полдень UTC гарантирует ту же календарную дату для поясов России. */
        fun formatDay(epochDay: Long): String {
            val cal = java.util.Calendar.getInstance().apply {
                clear()
                timeInMillis = epochDay * 86_400_000L + 6 * 3_600_000L
            }
            val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val month = cal.get(java.util.Calendar.MONTH) + 1
            val year = cal.get(java.util.Calendar.YEAR)
            return "%02d.%02d.%04d".format(day, month, year)
        }
    }
}
