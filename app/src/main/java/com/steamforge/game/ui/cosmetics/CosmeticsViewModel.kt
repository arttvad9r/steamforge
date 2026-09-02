package com.steamforge.game.ui.cosmetics

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.billing.BillingProvider
import com.steamforge.game.billing.CosmeticProduct
import com.steamforge.game.billing.StoreProductState
import com.steamforge.game.cosmetics.CosmeticCatalog
import com.steamforge.game.cosmetics.CosmeticLoadout
import com.steamforge.game.cosmetics.CosmeticLoadoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CosmeticsUiState(
    val loaded: Boolean = false,
    val configured: Boolean = false,
    val loading: Boolean = false,
    val selected: CosmeticLoadout = CosmeticLoadout(),
    val effective: CosmeticLoadout = CosmeticLoadout(),
    val tileSetOwned: Boolean = false,
    val workshopThemeOwned: Boolean = false,
    val tilePack: StoreProductState = StoreProductState(),
    val workshopPack: StoreProductState = StoreProductState(),
    val starterBundle: StoreProductState = StoreProductState(),
    val message: String? = null,
) {
    val allCosmeticsOwned: Boolean get() = tileSetOwned && workshopThemeOwned
}

class CosmeticsViewModel(
    private val billing: BillingProvider,
    private val loadouts: CosmeticLoadoutRepository,
) : ViewModel() {

    val ui: StateFlow<CosmeticsUiState> = combine(billing.cosmetics, loadouts.loadout) { purchases, selected ->
        CosmeticsUiState(
            loaded = true,
            configured = purchases.configured,
            loading = purchases.loading,
            selected = selected,
            effective = selected.effective(
                tileSetOwned = purchases.tileSetOwned,
                workshopThemeOwned = purchases.workshopThemeOwned,
            ),
            tileSetOwned = purchases.tileSetOwned,
            workshopThemeOwned = purchases.workshopThemeOwned,
            tilePack = purchases.tilePack,
            workshopPack = purchases.workshopPack,
            starterBundle = purchases.starterBundle,
            message = purchases.message,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CosmeticsUiState())

    fun refreshPurchases() = billing.refresh()

    fun purchase(activity: Activity, product: CosmeticProduct) = billing.purchaseCosmetic(activity, product)

    fun equipTileSet(id: String) {
        if (id == CosmeticCatalog.TILE_PATINA && !ui.value.tileSetOwned) return
        viewModelScope.launch { loadouts.equipTileSet(id) }
    }

    fun equipWorkshopTheme(id: String) {
        if (id == CosmeticCatalog.WORKSHOP_FOUNDRY && !ui.value.workshopThemeOwned) return
        viewModelScope.launch { loadouts.equipWorkshopTheme(id) }
    }
}
