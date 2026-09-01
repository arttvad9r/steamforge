package com.steamforge.game.billing

import android.app.Activity
import android.content.Intent
import com.steamforge.game.BuildConfig
import com.steamforge.game.analytics.Analytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.rustore.sdk.pay.RuStorePayClient
import ru.rustore.sdk.pay.model.ProductId
import ru.rustore.sdk.pay.model.ProductPurchase
import ru.rustore.sdk.pay.model.ProductPurchaseParams
import ru.rustore.sdk.pay.model.ProductPurchaseStatus
import ru.rustore.sdk.pay.model.ProductType

object BillingProviderFactory {
    fun create(
        context: android.content.Context,
        scope: CoroutineScope,
        analytics: Analytics,
    ): BillingProvider = RuStorePayBillingProvider(
        scope = scope,
        store = BillingEntitlementStore(context),
        analytics = analytics,
        configured = BuildConfig.RUSTORE_PAY_CONFIGURED,
        productId = BuildConfig.RUSTORE_REMOVE_ADS_PRODUCT_ID,
    )
}

/** Release adapter for the current RuStore Pay SDK. */
private class RuStorePayBillingProvider(
    private val scope: CoroutineScope,
    private val store: BillingEntitlementStore,
    private val analytics: Analytics,
    private val configured: Boolean,
    private val productId: String,
) : BillingProvider {
    private val state = MutableStateFlow(RemoveAdsState(configured = configured))
    override val removeAds: StateFlow<RemoveAdsState> = state.asStateFlow()

    private val client by lazy { RuStorePayClient.instance }

    init {
        scope.launch {
            store.removeAdsOwned.collect { owned ->
                state.update { it.copy(owned = owned) }
            }
        }
        if (configured) refresh()
    }

    override fun refresh() {
        if (!configured || productId.isBlank()) return
        state.update { it.copy(loading = true, message = null) }
        refreshProduct()
        refreshEntitlement()
    }

    private fun refreshProduct() {
        client.getProductInteractor()
            .getProducts(productsId = listOf(ProductId(productId)))
            .addOnSuccessListener { products ->
                val product = products.firstOrNull {
                    it.productId.value == productId && it.type == ProductType.NON_CONSUMABLE_PRODUCT
                }
                state.update {
                    it.copy(
                        productAvailable = product != null,
                        priceLabel = product?.amountLabel?.value,
                    )
                }
            }
            .addOnFailureListener { error ->
                analytics.logEvent("billing_product_load_failed", mapOf("store" to "rustore", "error" to error.javaClass.simpleName))
                state.update { it.copy(productAvailable = false, message = "Покупка временно недоступна") }
            }
    }

    private fun refreshEntitlement() {
        client.getPurchaseInteractor()
            .getPurchases()
            .addOnSuccessListener { purchases ->
                val owned = purchases.filterIsInstance<ProductPurchase>().any { purchase ->
                    purchase.productId.value == productId &&
                        purchase.productType == ProductType.NON_CONSUMABLE_PRODUCT &&
                        purchase.status == ProductPurchaseStatus.CONFIRMED
                }
                scope.launch { store.setRemoveAdsOwned(owned) }
                state.update { it.copy(owned = owned, loading = false, message = null) }
            }
            .addOnFailureListener { error ->
                // A transient store/network failure must never revoke the last known entitlement.
                analytics.logEvent("billing_reconcile_failed", mapOf("store" to "rustore", "error" to error.javaClass.simpleName))
                state.update { it.copy(loading = false, message = "Не удалось проверить покупки") }
            }
    }

    override fun purchaseRemoveAds(activity: Activity) {
        if (!configured || productId.isBlank() || state.value.owned || state.value.purchaseInProgress) return
        state.update { it.copy(purchaseInProgress = true, message = null) }
        analytics.logEvent("remove_ads_purchase_started", mapOf("store" to "rustore"))
        client.getPurchaseInteractor()
            .purchase(params = ProductPurchaseParams(ProductId(productId)))
            .addOnSuccessListener { result ->
                val matches = result.productId.value == productId && result.productType == ProductType.NON_CONSUMABLE_PRODUCT
                if (matches) {
                    scope.launch { store.setRemoveAdsOwned(true) }
                    analytics.logEvent("remove_ads_purchase_completed", mapOf("store" to "rustore"))
                    state.update { it.copy(owned = true, purchaseInProgress = false, message = null) }
                } else {
                    state.update { it.copy(purchaseInProgress = false, message = "Получен неизвестный товар") }
                }
                refreshEntitlement()
            }
            .addOnFailureListener { error ->
                analytics.logEvent("remove_ads_purchase_failed", mapOf("store" to "rustore", "error" to error.javaClass.simpleName))
                state.update { it.copy(purchaseInProgress = false, message = "Покупка не завершена") }
                refreshEntitlement()
            }
    }

    override fun proceedIntent(intent: Intent?) {
        if (!configured) return
        runCatching { client.getIntentInteractor().proceedIntent(intent) }
            .onFailure { error ->
                analytics.logEvent("billing_deeplink_failed", mapOf("store" to "rustore", "error" to error.javaClass.simpleName))
            }
    }
}
