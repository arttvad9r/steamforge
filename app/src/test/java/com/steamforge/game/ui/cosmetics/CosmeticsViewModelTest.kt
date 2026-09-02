package com.steamforge.game.ui.cosmetics

import android.app.Activity
import android.content.Intent
import com.steamforge.game.billing.BillingProvider
import com.steamforge.game.billing.CosmeticProduct
import com.steamforge.game.billing.CosmeticsBillingState
import com.steamforge.game.billing.RemoveAdsState
import com.steamforge.game.billing.StoreProductState
import com.steamforge.game.cosmetics.CosmeticCatalog
import com.steamforge.game.cosmetics.CosmeticLoadout
import com.steamforge.game.cosmetics.CosmeticLoadoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CosmeticsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private class FakeBilling(initial: CosmeticsBillingState) : BillingProvider {
        override val removeAds = MutableStateFlow(RemoveAdsState())
        override val cosmetics = MutableStateFlow(initial)
        override fun refresh() = Unit
        override fun purchaseRemoveAds(activity: Activity) = Unit
        override fun purchaseCosmetic(activity: Activity, product: CosmeticProduct) = Unit
        override fun proceedIntent(intent: Intent?) = Unit
    }

    private class FakeLoadouts(initial: CosmeticLoadout = CosmeticLoadout()) : CosmeticLoadoutRepository {
        override val loadout = MutableStateFlow(initial)
        override suspend fun equipTileSet(id: String) {
            loadout.value = loadout.value.copy(tileSet = id)
        }
        override suspend fun equipWorkshopTheme(id: String) {
            loadout.value = loadout.value.copy(workshopTheme = id)
        }
    }

    @Test
    fun `paid style cannot be equipped before entitlement`() = runTest(dispatcher) {
        val billing = FakeBilling(CosmeticsBillingState(configured = true))
        val loadouts = FakeLoadouts()
        val model = CosmeticsViewModel(billing, loadouts)
        advanceUntilIdle()

        model.equipTileSet(CosmeticCatalog.TILE_PATINA)
        advanceUntilIdle()

        assertEquals(CosmeticCatalog.TILE_CLASSIC, loadouts.loadout.value.tileSet)
        assertFalse(model.ui.value.tileSetOwned)

        billing.cosmetics.value = billing.cosmetics.value.copy(
            tilePack = StoreProductState(owned = true, productAvailable = true, priceLabel = "99 ₽"),
        )
        advanceUntilIdle()
        model.equipTileSet(CosmeticCatalog.TILE_PATINA)
        advanceUntilIdle()

        assertTrue(model.ui.value.tileSetOwned)
        assertEquals(CosmeticCatalog.TILE_PATINA, model.ui.value.effective.tileSet)
    }

    @Test
    fun `starter bundle unlocks both cosmetic categories`() = runTest(dispatcher) {
        val billing = FakeBilling(
            CosmeticsBillingState(
                configured = true,
                starterBundle = StoreProductState(owned = true),
            ),
        )
        val loadouts = FakeLoadouts(
            CosmeticLoadout(
                tileSet = CosmeticCatalog.TILE_PATINA,
                workshopTheme = CosmeticCatalog.WORKSHOP_FOUNDRY,
            ),
        )
        val model = CosmeticsViewModel(billing, loadouts)
        advanceUntilIdle()

        assertTrue(model.ui.value.tileSetOwned)
        assertTrue(model.ui.value.workshopThemeOwned)
        assertTrue(model.ui.value.allCosmeticsOwned)
        assertEquals(CosmeticCatalog.TILE_PATINA, model.ui.value.effective.tileSet)
        assertEquals(CosmeticCatalog.WORKSHOP_FOUNDRY, model.ui.value.effective.workshopTheme)
    }
}
