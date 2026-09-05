package com.steamforge.game.monetization

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.steamforge.game.BuildConfig
import com.steamforge.game.analytics.Analytics
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.InitializationListener
import com.yandex.mobile.ads.common.YandexAds
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.min

data class AdsConfig(
    /** Master switch. Keep the integration compiled, but do not initialize, load or show ads while false. */
    val enabled: Boolean = false,
    val interstitialMinGames: Int = 3,
    val interstitialEveryGames: Int = 5,
    val rewardedEnabled: Boolean = true,
    val interstitialEnabled: Boolean = true,
)

/**
 * Обёртка над Yandex Mobile Ads. Ошибки рекламы никогда не блокируют игру.
 * Debug всегда использует официальные demo unit IDs, независимо от production properties.
 */
class AdsManager(
    private val analytics: Analytics,
    private val cfg: AdsConfig = AdsConfig(),
    private val isDebug: Boolean = false,
) {
    val enabled: Boolean get() = cfg.enabled

    private var initialized = false
    private val handler = Handler(Looper.getMainLooper())
    private val interstitialPolicy = InterstitialSessionPolicy(cfg)

    private var rewardedLoader: RewardedAdLoader? = null
    private var rewardedAd: RewardedAd? = null
    private var rewardedLoading = false
    private var rewardedShowing = false
    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()
    private var rewardedRetryAttempt = 0
    private var rewardedRetryScheduled = false

    private var interstitialLoader: InterstitialAdLoader? = null
    private var interstitialAd: InterstitialAd? = null
    private var interstitialLoading = false
    private var interstitialShowing = false
    private var interstitialRetryAttempt = 0
    private var interstitialRetryScheduled = false

    fun init(context: Context, userConsent: Boolean) {
        if (!cfg.enabled || initialized) return
        initialized = true
        runCatching {
            YandexAds.setUserConsent(userConsent)
            YandexAds.setLocationTracking(false)
            YandexAds.initialize(context, InitializationListener { })
            if (cfg.rewardedEnabled && rewardedId().isNotBlank()) {
                rewardedLoader = RewardedAdLoader(context)
                loadRewarded()
            }
            if (cfg.interstitialEnabled && interstitialId().isNotBlank()) {
                interstitialLoader = InterstitialAdLoader(context)
                loadInterstitial()
            }
        }.onFailure {
            initialized = false
            rewardedLoading = false
            rewardedShowing = false
            interstitialLoading = false
            interstitialShowing = false
            _rewardedReady.value = false
        }
    }

    fun showRewarded(activity: Activity, onReward: () -> Unit) {
        if (!cfg.enabled || rewardedShowing) return
        val ad = rewardedAd ?: return
        rewardedShowing = true
        _rewardedReady.value = false
        analytics.logEvent("rewarded_started")
        ad.setAdEventListener(object : RewardedAdEventListener {
            override fun onAdShown() {
                interstitialPolicy.onRewardedShown()
            }

            override fun onAdFailedToShow(adError: com.yandex.mobile.ads.common.AdError) {
                analytics.logEvent("rewarded_show_failed")
                cleanupRewarded()
            }

            override fun onAdDismissed() {
                analytics.logEvent("rewarded_dismissed")
                cleanupRewarded()
            }

            override fun onAdClicked() = Unit
            override fun onAdImpression(impressionData: com.yandex.mobile.ads.common.ImpressionData?) = Unit

            override fun onRewarded(reward: Reward) {
                analytics.logEvent("rewarded_sdk_completed", mapOf("amount" to reward.amount))
                onReward()
            }
        })
        runCatching { ad.show(activity) }.onFailure { cleanupRewarded() }
    }

    /** Отмечает рекламный момент; если ad ещё не загружен, момент сохраняется до следующей естественной паузы. */
    fun onGameFinished() {
        if (!cfg.enabled) return
        interstitialPolicy.onGameFinished()
        if (rewardedAd == null) loadRewarded()
        if (interstitialAd == null) loadInterstitial()
    }

    fun maybeShowInterstitial(activity: Activity) {
        if (!cfg.enabled || interstitialShowing) return
        // Никогда не ставим interstitial сразу после rewarded на одном и том же result screen.
        if (!interstitialPolicy.shouldAttemptInterstitial()) return
        val ad = interstitialAd ?: run {
            loadInterstitial()
            return
        }
        interstitialPolicy.onInterstitialAttemptStarted()
        interstitialShowing = true
        ad.setAdEventListener(object : InterstitialAdEventListener {
            override fun onAdShown() {
                analytics.logEvent("interstitial_shown")
            }

            override fun onAdFailedToShow(adError: com.yandex.mobile.ads.common.AdError) {
                analytics.logEvent("interstitial_show_failed")
                interstitialPolicy.onInterstitialAttemptFailed()
                cleanupInterstitial()
            }

            override fun onAdDismissed() = cleanupInterstitial()
            override fun onAdClicked() = Unit
            override fun onAdImpression(impressionData: com.yandex.mobile.ads.common.ImpressionData?) = Unit
        })
        runCatching { ad.show(activity) }.onFailure {
            interstitialPolicy.onInterstitialAttemptFailed()
            cleanupInterstitial()
        }
    }

    private fun rewardedId(): String =
        if (isDebug) "demo-rewarded-yandex" else BuildConfig.REWARDED_AD_UNIT_ID

    private fun interstitialId(): String =
        if (isDebug) "demo-interstitial-yandex" else BuildConfig.INTERSTITIAL_AD_UNIT_ID

    private fun cleanupRewarded() {
        rewardedAd?.setAdEventListener(null)
        rewardedAd = null
        rewardedShowing = false
        _rewardedReady.value = false
        loadRewarded()
    }

    private fun cleanupInterstitial() {
        interstitialAd?.setAdEventListener(null)
        interstitialAd = null
        interstitialShowing = false
        loadInterstitial()
    }

    private fun loadRewarded() {
        if (!cfg.enabled || !initialized || rewardedAd != null || rewardedLoading || rewardedShowing) return
        val loader = rewardedLoader ?: return
        val id = rewardedId()
        if (id.isBlank()) return
        rewardedLoading = true
        runCatching {
            loader.loadAd(
                AdRequest.Builder(id).build(),
                object : RewardedAdLoadListener {
                    override fun onAdLoaded(rewarded: RewardedAd) {
                        rewardedLoading = false
                        rewardedRetryAttempt = 0
                        rewardedRetryScheduled = false
                        rewardedAd = rewarded
                        _rewardedReady.value = true
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        rewardedLoading = false
                        _rewardedReady.value = false
                        analytics.logEvent(
                            "ad_load_failed",
                            mapOf("format" to "rewarded", "code" to error.code, "desc" to error.description),
                        )
                        scheduleRewardedRetry()
                    }
                },
            )
        }.onFailure {
            rewardedLoading = false
            scheduleRewardedRetry()
        }
    }

    private fun loadInterstitial() {
        if (!cfg.enabled || !initialized || interstitialAd != null || interstitialLoading || interstitialShowing) return
        val loader = interstitialLoader ?: return
        val id = interstitialId()
        if (id.isBlank()) return
        interstitialLoading = true
        runCatching {
            loader.loadAd(
                AdRequest.Builder(id).build(),
                object : InterstitialAdLoadListener {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        interstitialLoading = false
                        interstitialRetryAttempt = 0
                        interstitialRetryScheduled = false
                        this@AdsManager.interstitialAd = interstitialAd
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        interstitialLoading = false
                        analytics.logEvent(
                            "ad_load_failed",
                            mapOf("format" to "interstitial", "code" to error.code, "desc" to error.description),
                        )
                        scheduleInterstitialRetry()
                    }
                },
            )
        }.onFailure {
            interstitialLoading = false
            scheduleInterstitialRetry()
        }
    }

    private fun scheduleRewardedRetry() {
        if (!cfg.enabled || !initialized || rewardedRetryScheduled || rewardedId().isBlank()) return
        rewardedRetryScheduled = true
        val delayMs = retryDelayMs(rewardedRetryAttempt++)
        handler.postDelayed({
            rewardedRetryScheduled = false
            if (rewardedAd == null) loadRewarded()
        }, delayMs)
    }

    private fun scheduleInterstitialRetry() {
        if (!cfg.enabled || !initialized || interstitialRetryScheduled || interstitialId().isBlank()) return
        interstitialRetryScheduled = true
        val delayMs = retryDelayMs(interstitialRetryAttempt++)
        handler.postDelayed({
            interstitialRetryScheduled = false
            if (interstitialAd == null) loadInterstitial()
        }, delayMs)
    }

    private fun retryDelayMs(attempt: Int): Long {
        val exponent = min(attempt, 5)
        return min(60_000L, 2_000L * (1L shl exponent))
    }
}
