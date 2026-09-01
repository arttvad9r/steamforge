package com.steamforge.game.billing

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.billingDataStore by preferencesDataStore(name = "steamforge_billing")

data class CosmeticEntitlements(
    val tilePackOwned: Boolean = false,
    val workshopPackOwned: Boolean = false,
    val starterBundleOwned: Boolean = false,
) {
    val tileSetOwned: Boolean get() = tilePackOwned || starterBundleOwned
    val workshopThemeOwned: Boolean get() = workshopPackOwned || starterBundleOwned
}

/** Purchase entitlements live outside player progress so Reset Progress cannot revoke purchases. */
class BillingEntitlementStore(context: Context) {
    private val appContext = context.applicationContext

    val removeAdsOwned: Flow<Boolean> = appContext.billingDataStore.data
        .map { prefs -> prefs[REMOVE_ADS_OWNED] ?: false }
        .distinctUntilChanged()

    val cosmeticEntitlements: Flow<CosmeticEntitlements> = combine(
        appContext.billingDataStore.data.map { it[TILE_PACK_OWNED] ?: false },
        appContext.billingDataStore.data.map { it[WORKSHOP_PACK_OWNED] ?: false },
        appContext.billingDataStore.data.map { it[STARTER_BUNDLE_OWNED] ?: false },
    ) { tile, workshop, bundle ->
        CosmeticEntitlements(tile, workshop, bundle)
    }.distinctUntilChanged()

    suspend fun setRemoveAdsOwned(owned: Boolean) {
        appContext.billingDataStore.edit { prefs -> prefs[REMOVE_ADS_OWNED] = owned }
    }

    suspend fun setCosmeticOwnership(
        tilePackOwned: Boolean,
        workshopPackOwned: Boolean,
        starterBundleOwned: Boolean,
    ) {
        appContext.billingDataStore.edit { prefs ->
            prefs[TILE_PACK_OWNED] = tilePackOwned
            prefs[WORKSHOP_PACK_OWNED] = workshopPackOwned
            prefs[STARTER_BUNDLE_OWNED] = starterBundleOwned
        }
    }

    private companion object {
        val REMOVE_ADS_OWNED = booleanPreferencesKey("remove_ads_owned")
        val TILE_PACK_OWNED = booleanPreferencesKey("tile_cosmetic_pack_owned")
        val WORKSHOP_PACK_OWNED = booleanPreferencesKey("workshop_cosmetic_pack_owned")
        val STARTER_BUNDLE_OWNED = booleanPreferencesKey("starter_cosmetic_bundle_owned")
    }
}
