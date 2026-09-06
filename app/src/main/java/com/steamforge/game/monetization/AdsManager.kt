package com.steamforge.game.monetization

import android.app.Activity
import android.content.Context
import com.steamforge.game.analytics.Analytics
import com.steamforge.game.analytics.NoopAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Compatibility shell retained only while old gameplay call sites are removed incrementally.
 *
 * There is no advertising SDK, no ad unit configuration, no network loading, no rewarded flow and
 * no interstitial flow. All operations are permanent no-ops and rewarded readiness is always false.
 */
data class AdsConfig(
    val enabled: Boolean = false,
    val interstitialMinGames: Int = 0,
    val interstitialEveryGames: Int = 0,
    val rewardedEnabled: Boolean = false,
    val interstitialEnabled: Boolean = false,
)

class AdsManager(
    @Suppress("UNUSED_PARAMETER") analytics: Analytics = NoopAnalytics(),
    @Suppress("UNUSED_PARAMETER") cfg: AdsConfig = AdsConfig(),
    @Suppress("UNUSED_PARAMETER") isDebug: Boolean = false,
) {
    val enabled: Boolean = false

    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    @Suppress("UNUSED_PARAMETER")
    fun init(context: Context, userConsent: Boolean) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun showRewarded(activity: Activity, onReward: () -> Unit) = Unit

    fun onGameFinished() = Unit

    @Suppress("UNUSED_PARAMETER")
    fun maybeShowInterstitial(activity: Activity) = Unit
}
