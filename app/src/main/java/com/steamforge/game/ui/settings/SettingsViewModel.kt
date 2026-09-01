package com.steamforge.game.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.billing.BillingProvider
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.PlayerProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
    /** null = пользователь ещё не решал (решение запрашивается на главном экране). */
    val analyticsConsent: Boolean? = null,
    val removeAdsConfigured: Boolean = false,
    val removeAdsOwned: Boolean = false,
    val removeAdsProductAvailable: Boolean = false,
    val removeAdsLoading: Boolean = false,
    val removeAdsPurchaseInProgress: Boolean = false,
    val removeAdsPriceLabel: String? = null,
    val billingMessage: String? = null,
)

class SettingsViewModel(
    private val repo: DataRepo,
    private val billing: BillingProvider,
) : ViewModel() {

    val ui: StateFlow<SettingsUiState> = combine(repo.progress, billing.removeAds) { p, purchase ->
        SettingsUiState(
            soundEnabled = p.soundEnabled,
            hapticsEnabled = p.hapticsEnabled,
            animationsEnabled = p.animationsEnabled,
            analyticsConsent = p.analyticsConsent,
            removeAdsConfigured = purchase.configured,
            removeAdsOwned = purchase.owned,
            removeAdsProductAvailable = purchase.productAvailable,
            removeAdsLoading = purchase.loading,
            removeAdsPurchaseInProgress = purchase.purchaseInProgress,
            removeAdsPriceLabel = purchase.priceLabel,
            billingMessage = purchase.message,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun setSound(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }
    fun setHaptics(enabled: Boolean) = update { it.copy(hapticsEnabled = enabled) }
    fun setAnimations(enabled: Boolean) = update { it.copy(animationsEnabled = enabled) }
    fun setAnalyticsConsent(granted: Boolean) = update { it.copy(analyticsConsent = granted) }

    fun purchaseRemoveAds(activity: Activity) = billing.purchaseRemoveAds(activity)
    fun refreshPurchases() = billing.refresh()

    fun resetProgress() {
        viewModelScope.launch { repo.resetGameProgress() }
    }

    private fun update(block: (PlayerProgress) -> PlayerProgress) {
        viewModelScope.launch { repo.updateProgress(block) }
    }
}
