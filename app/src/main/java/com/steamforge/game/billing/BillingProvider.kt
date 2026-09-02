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

enum class CosmeticProduct {
    TILE_PACK,
    WORKSHOP_PACK,
    STARTER_BUNDLE,
}

data class StoreProductState(
    val owned: Boolean = false,
    val productAvailable: Boolean = false,
    val purchaseInProgress: Boolean = false,
    val priceLabel: String? = null,
)

data class CosmeticsBillingState(
    val configured: Boolean = false,
    val loading: Boolean = false,
    val tilePack: StoreProductState = StoreProductState(),
    val workshopPack: StoreProductState = StoreProductState(),
    val starterBundle: StoreProductState = StoreProductState(),
    val message: String? = null,
) {
    val tileSetOwned: Boolean get() = tilePack.owned || starterBundle.owned
    val workshopThemeOwned: Boolean get() = workshopPack.owned || starterBundle.owned

    fun product(product: CosmeticProduct): StoreProductState = when (product) {
        CosmeticProduct.TILE_PACK -> tilePack
        CosmeticProduct.WORKSHOP_PACK -> workshopPack
        CosmeticProduct.STARTER_BUNDLE -> starterBundle
    }
}

/** Store-neutral boundary. Game and UI code must not depend on a concrete billing SDK. */
interface BillingProvider {
    val removeAds: StateFlow<RemoveAdsState>
    val cosmetics: StateFlow<CosmeticsBillingState>
    fun refresh()
    fun purchaseRemoveAds(activity: Activity)
    fun purchaseCosmetic(activity: Activity, product: CosmeticProduct)
    fun proceedIntent(intent: Intent?)
}

/** Debug/offline implementation: preserves already cached entitlements but never starts a payment. */
class DisabledBillingProvider(
    scope: CoroutineScope,
    store: BillingEntitlementStore,
) : BillingProvider {
    private val removeAdsState = MutableStateFlow(RemoveAdsState())
    override val removeAds: StateFlow<RemoveAdsState> = removeAdsState.asStateFlow()

    private val cosmeticState = MutableStateFlow(CosmeticsBillingState())
    override val cosmetics: StateFlow<CosmeticsBillingState> = cosmeticState.asStateFlow()

    init {
        scope.launch {
            store.removeAdsOwned.collect { owned ->
                removeAdsState.update { it.copy(owned = owned) }
            }
        }
        scope.launch {
            store.cosmeticEntitlements.collect { owned ->
                cosmeticState.update {
                    it.copy(
                        tilePack = it.tilePack.copy(owned = owned.tilePackOwned),
                        workshopPack = it.workshopPack.copy(owned = owned.workshopPackOwned),
                        starterBundle = it.starterBundle.copy(owned = owned.starterBundleOwned),
                    )
                }
            }
        }
    }

    override fun refresh() = Unit
    override fun purchaseRemoveAds(activity: Activity) = Unit
    override fun purchaseCosmetic(activity: Activity, product: CosmeticProduct) = Unit
    override fun proceedIntent(intent: Intent?) = Unit
}
