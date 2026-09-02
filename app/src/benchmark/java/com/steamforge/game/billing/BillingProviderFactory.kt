package com.steamforge.game.billing

import android.content.Context
import com.steamforge.game.analytics.Analytics
import kotlinx.coroutines.CoroutineScope

/**
 * Benchmark variants must stay release-like without initializing store billing.
 * The benchmark fixture exercises rendering only, so use the same disabled provider
 * as debug while keeping production release billing untouched.
 */
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
