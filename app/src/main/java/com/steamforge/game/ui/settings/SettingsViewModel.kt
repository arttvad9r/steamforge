package com.steamforge.game.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.PlayerProgress
import java.io.IOException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
)

class SettingsViewModel(
    private val repo: DataRepo,
) : ViewModel() {

    val ui: StateFlow<SettingsUiState> = repo.progress.map { p ->
        SettingsUiState(p.soundEnabled, p.hapticsEnabled, p.animationsEnabled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setSound(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }
    fun setHaptics(enabled: Boolean) = update { it.copy(hapticsEnabled = enabled) }
    fun setAnimations(enabled: Boolean) = update { it.copy(animationsEnabled = enabled) }

    fun resetProgress() = launchPersistenceWrite { repo.resetGameProgress() }

    private fun update(block: (PlayerProgress) -> PlayerProgress) {
        launchPersistenceWrite { repo.updateProgress(block) }
    }

    private fun launchPersistenceWrite(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (_: IOException) {
                // Keep the last durable state visible; a later user action can retry the write.
            }
        }
    }
}
