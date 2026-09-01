package com.steamforge.game.billing

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.billingDataStore by preferencesDataStore(name = "steamforge_billing")

/** Purchase entitlements live outside player progress so Reset Progress cannot revoke purchases. */
class BillingEntitlementStore(context: Context) {
    private val appContext = context.applicationContext

    val removeAdsOwned: Flow<Boolean> = appContext.billingDataStore.data
        .map { prefs -> prefs[REMOVE_ADS_OWNED] ?: false }
        .distinctUntilChanged()

    suspend fun setRemoveAdsOwned(owned: Boolean) {
        appContext.billingDataStore.edit { prefs -> prefs[REMOVE_ADS_OWNED] = owned }
    }

    private companion object {
        val REMOVE_ADS_OWNED = booleanPreferencesKey("remove_ads_owned")
    }
}
