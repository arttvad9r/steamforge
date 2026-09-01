package com.steamforge.game.billing

import android.content.Context
import com.steamforge.game.analytics.Analytics
import kotlinx.coroutines.CoroutineScope

object BillingProviderFactory {
    fun create(
        context: Context,
        scope: CoroutineScope,
        analytics: Analytics,
    ): BillingProvider = DisabledBillingProvider(
        scope = scope,
        store = BillingEntitlementStore(context),
    )
}
