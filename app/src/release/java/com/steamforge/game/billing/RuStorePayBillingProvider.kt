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
        removeAdsProductId = BuildConfig.RUSTORE_REMOVE_ADS_PRODUCT_ID,
        tilePackProductId = BuildConfig.RUSTORE_TILE_COSMETIC_PRODUCT_ID,
        workshopPackProductId = BuildConfig.RUSTORE_WORKSHOP_COSMETIC_PRODUCT_ID,
        starterBundleProductId = BuildConfig.RUSTORE_STARTER_COSMETIC_BUNDLE_PRODUCT_ID,
    )
}

/** Release adapter for the current RuStore Pay SDK. */
private class RuStorePayBillingProvider(
    private val scope: CoroutineScope,
    private val store: BillingEntitlementStore,
    private val analytics: Analytics,
    private val configured: Boolean,
    private val removeAdsProductId: String,
    private val tilePackProductId: String,
    private val workshopPackProductId: String,
    private val starterBundleProductId: String,
) : BillingProvider {
    private val removeAdsState = MutableStateFlow(RemoveAdsState(configured = configured))
    override val removeAds: StateFlow<RemoveAdsState> = removeAdsState.asStateFlow()

    private val cosmeticState = MutableStateFlow(CosmeticsBillingState(configured = configured))
    override val cosmetics: StateFlow<CosmeticsBillingState> = cosmeticState.asStateFlow()

    private val client by lazy { RuStorePayClient.instance }

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
        if (configured) refresh()
    }

    override fun refresh() {
        if (!configured) return
        removeAdsState.update { it.copy(loading = true, message = null) }
        cosmeticState.update { it.copy(loading = true, message = null) }
        refreshProducts()
        refreshEntitlements()
    }

    private fun refreshProducts() {
        val ids = allProductIds().filter { it.isNotBlank() }.map(::ProductId)
        if (ids.isEmpty()) {
            removeAdsState.update { it.copy(loading = false) }
            cosmeticState.update { it.copy(loading = false) }
            return
        }
        client.getProductInteractor()
            .getProducts(productsId = ids)
            .addOnSuccessListener { products ->
                fun product(id: String) = products.firstOrNull {
                    it.productId.value == id && it.type == ProductType.NON_CONSUMABLE_PRODUCT
                }
                val removeAdsProduct = product(removeAdsProductId)
                val tileProduct = product(tilePackProductId)
                val workshopProduct = product(workshopPackProductId)
                val bundleProduct = product(starterBundleProductId)
                removeAdsState.update {
                    it.copy(
                        productAvailable = removeAdsProduct != null,
                        priceLabel = removeAdsProduct?.amountLabel?.value,
                    )
                }
                cosmeticState.update {
                    it.copy(
                        tilePack = it.tilePack.copy(
                            productAvailable = tileProduct != null,
                            priceLabel = tileProduct?.amountLabel?.value,
                        ),
                        workshopPack = it.workshopPack.copy(
                            productAvailable = workshopProduct != null,
                            priceLabel = workshopProduct?.amountLabel?.value,
                        ),
                        starterBundle = it.starterBundle.copy(
                            productAvailable = bundleProduct != null,
                            priceLabel = bundleProduct?.amountLabel?.value,
                        ),
                    )
                }
            }
            .addOnFailureListener { error ->
                analytics.logEvent("billing_product_load_failed", mapOf("store" to "rustore", "error" to error.javaClass.simpleName))
                removeAdsState.update { it.copy(productAvailable = false, message = "Покупка временно недоступна") }
                cosmeticState.update {
                    it.copy(
                        tilePack = it.tilePack.copy(productAvailable = false),
                        workshopPack = it.workshopPack.copy(productAvailable = false),
                        starterBundle = it.starterBundle.copy(productAvailable = false),
                        message = "Покупки оформления временно недоступны",
                    )
                }
            }
    }

    private fun refreshEntitlements() {
        client.getPurchaseInteractor()
            .getPurchases()
            .addOnSuccessListener { purchases ->
                val confirmed = purchases.filterIsInstance<ProductPurchase>().filter {
                    it.productType == ProductType.NON_CONSUMABLE_PRODUCT &&
                        it.status == ProductPurchaseStatus.CONFIRMED
                }
                fun owned(id: String): Boolean = confirmed.any { it.productId.value == id }

                val removeAdsOwned = owned(removeAdsProductId)
                val tileOwned = owned(tilePackProductId)
                val workshopOwned = owned(workshopPackProductId)
                val bundleOwned = owned(starterBundleProductId)

                scope.launch {
                    store.setRemoveAdsOwned(removeAdsOwned)
                    store.setCosmeticOwnership(tileOwned, workshopOwned, bundleOwned)
                }
                removeAdsState.update { it.copy(owned = removeAdsOwned, loading = false, message = null) }
                cosmeticState.update {
                    it.copy(
                        loading = false,
                        tilePack = it.tilePack.copy(owned = tileOwned),
                        workshopPack = it.workshopPack.copy(owned = workshopOwned),
                        starterBundle = it.starterBundle.copy(owned = bundleOwned),
                        message = null,
                    )
                }
            }
            .addOnFailureListener { error ->
                // A transient store/network failure must never revoke the last known entitlements.
                analytics.logEvent("billing_reconcile_failed", mapOf("store" to "rustore", "error" to error.javaClass.simpleName))
                removeAdsState.update { it.copy(loading = false, message = "Не удалось проверить покупки") }
                cosmeticState.update { it.copy(loading = false, message = "Не удалось проверить покупки оформления") }
            }
    }

    override fun purchaseRemoveAds(activity: Activity) {
        if (removeAdsState.value.owned || removeAdsState.value.purchaseInProgress) return
        purchaseProduct(
            productId = removeAdsProductId,
            analyticsProduct = "remove_ads",
            markInProgress = { inProgress -> removeAdsState.update { it.copy(purchaseInProgress = inProgress, message = null) } },
            markOwned = {
                scope.launch { store.setRemoveAdsOwned(true) }
                removeAdsState.update { it.copy(owned = true) }
            },
        )
    }

    override fun purchaseCosmetic(activity: Activity, product: CosmeticProduct) {
        val current = cosmeticState.value.product(product)
        if (current.owned || current.purchaseInProgress) return
        val productId = cosmeticProductId(product)
        purchaseProduct(
            productId = productId,
            analyticsProduct = when (product) {
                CosmeticProduct.TILE_PACK -> "tile_cosmetic_pack"
                CosmeticProduct.WORKSHOP_PACK -> "workshop_cosmetic_pack"
                CosmeticProduct.STARTER_BUNDLE -> "starter_cosmetic_bundle"
            },
            markInProgress = { inProgress -> updateCosmeticProduct(product) { it.copy(purchaseInProgress = inProgress) } },
            markOwned = {
                scope.launch {
                    when (product) {
                        CosmeticProduct.TILE_PACK -> store.setTilePackOwned(true)
                        CosmeticProduct.WORKSHOP_PACK -> store.setWorkshopPackOwned(true)
                        CosmeticProduct.STARTER_BUNDLE -> store.setStarterBundleOwned(true)
                    }
                }
                updateCosmeticProduct(product) { it.copy(owned = true) }
            },
        )
    }

    private fun purchaseProduct(
        productId: String,
        analyticsProduct: String,
        markInProgress: (Boolean) -> Unit,
        markOwned: () -> Unit,
    ) {
        if (!configured || productId.isBlank()) return
        markInProgress(true)
        analytics.logEvent("billing_purchase_started", mapOf("store" to "rustore", "product" to analyticsProduct))
        client.getPurchaseInteractor()
            .purchase(params = ProductPurchaseParams(ProductId(productId)))
            .addOnSuccessListener { result ->
                val matches = result.productId.value == productId && result.productType == ProductType.NON_CONSUMABLE_PRODUCT
                if (matches) {
                    markOwned()
                    analytics.logEvent("billing_purchase_completed", mapOf("store" to "rustore", "product" to analyticsProduct))
                    markInProgress(false)
                } else {
                    analytics.logEvent("billing_purchase_failed", mapOf("store" to "rustore", "product" to analyticsProduct, "error" to "unknown_product"))
                    markInProgress(false)
                    cosmeticState.update { it.copy(message = "Получен неизвестный товар") }
                }
                refreshEntitlements()
            }
            .addOnFailureListener { error ->
                analytics.logEvent(
                    "billing_purchase_failed",
                    mapOf("store" to "rustore", "product" to analyticsProduct, "error" to error.javaClass.simpleName),
                )
                markInProgress(false)
                if (analyticsProduct == "remove_ads") {
                    removeAdsState.update { it.copy(message = "Покупка не завершена") }
                } else {
                    cosmeticState.update { it.copy(message = "Покупка оформления не завершена") }
                }
                refreshEntitlements()
            }
    }

    private fun updateCosmeticProduct(
        product: CosmeticProduct,
        update: (StoreProductState) -> StoreProductState,
    ) {
        cosmeticState.update {
            when (product) {
                CosmeticProduct.TILE_PACK -> it.copy(tilePack = update(it.tilePack), message = null)
                CosmeticProduct.WORKSHOP_PACK -> it.copy(workshopPack = update(it.workshopPack), message = null)
                CosmeticProduct.STARTER_BUNDLE -> it.copy(starterBundle = update(it.starterBundle), message = null)
            }
        }
    }

    private fun cosmeticProductId(product: CosmeticProduct): String = when (product) {
        CosmeticProduct.TILE_PACK -> tilePackProductId
        CosmeticProduct.WORKSHOP_PACK -> workshopPackProductId
        CosmeticProduct.STARTER_BUNDLE -> starterBundleProductId
    }

    private fun allProductIds(): List<String> = listOf(
        removeAdsProductId,
        tilePackProductId,
        workshopPackProductId,
        starterBundleProductId,
    )

    override fun proceedIntent(intent: Intent?) {
        if (!configured) return
        runCatching { client.getIntentInteractor().proceedIntent(intent) }
            .onFailure { error ->
                analytics.logEvent("billing_deeplink_failed", mapOf("store" to "rustore", "error" to error.javaClass.simpleName))
            }
    }
}
