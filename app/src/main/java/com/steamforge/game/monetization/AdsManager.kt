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
    private var initialized = false
    private var gamesFinished = 0
    private val handler = Handler(Looper.getMainLooper())

    private var rewardedLoader: RewardedAdLoader? = null
    private var rewardedAd: RewardedAd? = null
    private var onRewarded: (() -> Unit)? = null
    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()
    private var rewardedRetryAttempt = 0
    private var rewardedRetryScheduled = false
    /** Rewarded уже был показан в текущей result-паузе: interstitial в этой же паузе подавляется. */
    private var rewardedShownSincePause = false

    private var interstitialLoader: InterstitialAdLoader? = null
    private var interstitialAd: InterstitialAd? = null
    private var interstitialPending = false
    private var interstitialRetryAttempt = 0
    private var interstitialRetryScheduled = false

    fun init(context: Context, userConsent: Boolean) {
        if (initialized) return
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
            _rewardedReady.value = false
        }
    }

    fun showRewarded(activity: Activity, onReward: () -> Unit) {
        val ad = rewardedAd ?: return
        onRewarded = onReward
        analytics.logEvent("rewarded_started")
        ad.setAdEventListener(object : RewardedAdEventListener {
            override fun onAdShown() {
                rewardedShownSincePause = true
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
                analytics.logEvent("rewarded_completed", mapOf("amount" to reward.amount))
                onRewarded?.invoke()
            }
        })
        runCatching { ad.show(activity) }.onFailure { cleanupRewarded() }
    }

    /** Отмечает рекламный момент; если ad ещё не загружен, момент сохраняется до следующей естественной паузы. */
    fun onGameFinished() {
        gamesFinished++
        if (
            gamesFinished >= cfg.interstitialMinGames &&
            (gamesFinished - cfg.interstitialMinGames) % cfg.interstitialEveryGames == 0
        ) {
            interstitialPending = true
        }
        if (rewardedAd == null) loadRewarded()
        if (interstitialAd == null) loadInterstitial()
    }

    fun maybeShowInterstitial(activity: Activity) {
        // Никогда не ставим interstitial сразу после rewarded на одном и том же result screen.
        if (rewardedShownSincePause) {
            rewardedShownSincePause = false
            return
        }
        if (!interstitialPending || !cfg.interstitialEnabled) return
        val ad = interstitialAd ?: run {
            loadInterstitial()
            return
        }
        interstitialPending = false
        ad.setAdEventListener(object : InterstitialAdEventListener {
            override fun onAdShown() {
                analytics.logEvent("interstitial_shown")
            }

            override fun onAdFailedToShow(adError: com.yandex.mobile.ads.common.AdError) {
                analytics.logEvent("interstitial_show_failed")
                interstitialPending = true
                cleanupInterstitial()
            }

            override fun onAdDismissed() = cleanupInterstitial()
            override fun onAdClicked() = Unit
            override fun onAdImpression(impressionData: com.yandex.mobile.ads.common.ImpressionData?) = Unit
        })
        runCatching { ad.show(activity) }.onFailure {
            interstitialPending = true
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
        _rewardedReady.value = false
        onRewarded = null
        loadRewarded()
    }

    private fun cleanupInterstitial() {
        interstitialAd?.setAdEventListener(null)
        interstitialAd = null
        loadInterstitial()
    }

    private fun loadRewarded() {
        if (!initialized || rewardedAd != null) return
        val loader = rewardedLoader ?: return
        val id = rewardedId()
        if (id.isBlank()) return
        runCatching {
            loader.loadAd(
                AdRequest.Builder(id).build(),
                object : RewardedAdLoadListener {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedRetryAttempt = 0
                        rewardedRetryScheduled = false
                        rewardedAd = ad
                        _rewardedReady.value = true
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        _rewardedReady.value = false
                        analytics.logEvent(
                            "ad_load_failed",
                            mapOf("format" to "rewarded", "code" to error.code, "desc" to error.description),
                        )
                        scheduleRewardedRetry()
                    }
                },
            )
        }.onFailure { scheduleRewardedRetry() }
    }

    private fun loadInterstitial() {
        if (!initialized || interstitialAd != null) return
        val loader = interstitialLoader ?: return
        val id = interstitialId()
        if (id.isBlank()) return
        runCatching {
            loader.loadAd(
                AdRequest.Builder(id).build(),
                object : InterstitialAdLoadListener {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialRetryAttempt = 0
                        interstitialRetryScheduled = false
                        interstitialAd = ad
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        analytics.logEvent(
                            "ad_load_failed",
                            mapOf("format" to "interstitial", "code" to error.code, "desc" to error.description),
                        )
                        scheduleInterstitialRetry()
                    }
                },
            )
        }.onFailure { scheduleInterstitialRetry() }
    }

    private fun scheduleRewardedRetry() {
        if (!initialized || rewardedRetryScheduled || rewardedId().isBlank()) return
        rewardedRetryScheduled = true
        val delayMs = retryDelayMs(rewardedRetryAttempt++)
        handler.postDelayed({
            rewardedRetryScheduled = false
            if (rewardedAd == null) loadRewarded()
        }, delayMs)
    }

    private fun scheduleInterstitialRetry() {
        if (!initialized || interstitialRetryScheduled || interstitialId().isBlank()) return
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
