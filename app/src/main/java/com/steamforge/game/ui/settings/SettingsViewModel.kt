package com.steamforge.game.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.PlayerProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
    /** null = пользователь ещё не решал (решение запрашивается на главном экране). */
    val analyticsConsent: Boolean? = null,
)

class SettingsViewModel(
    private val repo: DataRepo,
) : ViewModel() {

    val ui: StateFlow<SettingsUiState> = repo.progress.map { p ->
        SettingsUiState(p.soundEnabled, p.hapticsEnabled, p.animationsEnabled, p.analyticsConsent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setSound(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }
    fun setHaptics(enabled: Boolean) = update { it.copy(hapticsEnabled = enabled) }
    fun setAnimations(enabled: Boolean) = update { it.copy(animationsEnabled = enabled) }
    fun setAnalyticsConsent(granted: Boolean) = update { it.copy(analyticsConsent = granted) }

    fun resetProgress() {
        viewModelScope.launch { repo.resetGameProgress() }
    }

    private fun update(block: (PlayerProgress) -> PlayerProgress) {
        viewModelScope.launch { repo.updateProgress(block) }
    }
}
