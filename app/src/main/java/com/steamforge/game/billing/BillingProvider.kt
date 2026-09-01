package com.steamforge.game.billing

import android.app.Activity
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RemoveAdsState(
    val configured: Boolean = false,
    val owned: Boolean = false,
    val productAvailable: Boolean = false,
    val loading: Boolean = false,
    val purchaseInProgress: Boolean = false,
    val priceLabel: String? = null,
    val message: String? = null,
)

/** Store-neutral boundary. Game and UI code must not depend on a concrete billing SDK. */
interface BillingProvider {
    val removeAds: StateFlow<RemoveAdsState>
    fun refresh()
    fun purchaseRemoveAds(activity: Activity)
    fun proceedIntent(intent: Intent?)
}

/** Debug/offline implementation: preserves an already cached entitlement but never starts a payment. */
class DisabledBillingProvider(
    scope: CoroutineScope,
    store: BillingEntitlementStore,
) : BillingProvider {
    private val state = MutableStateFlow(RemoveAdsState())
    override val removeAds: StateFlow<RemoveAdsState> = state.asStateFlow()

    init {
        scope.launch {
            store.removeAdsOwned.collect { owned ->
                state.update { it.copy(owned = owned) }
            }
        }
    }

    override fun refresh() = Unit
    override fun purchaseRemoveAds(activity: Activity) = Unit
    override fun proceedIntent(intent: Intent?) = Unit
}
