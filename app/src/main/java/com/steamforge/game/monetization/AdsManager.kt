package com.steamforge.game.monetization

import android.app.Activity
import android.content.Context
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

/** Частота interstitial и прочая рекламная настройка. */
data class AdsConfig(
    /** Показать interstitial после 3-й завершённой партии, затем каждую 5-ю. */
    val interstitialMinGames: Int = 3,
    val interstitialEveryGames: Int = 5,
    val rewardedEnabled: Boolean = true,
    val interstitialEnabled: Boolean = true,
)

/**
 * Обёртка над Yandex Mobile Ads. Игра никогда не зависит от рекламы:
 * нет сети / ошибки SDK / отсутствие загрузки -> кнопки просто недоступны.
 *
 * Ad unit IDs приходят из BuildConfig (gradle properties). В release пустой ID
 * отключает соответствующий формат (никакого demo-fallback в проде); в debug
 * пустой ID подменяется официальными demo-юнитами Яндекса.
 */
class AdsManager(
    private val analytics: Analytics,
    private val cfg: AdsConfig = AdsConfig(),
    private val isDebug: Boolean = false,
) {

    private var initialized = false
    private var gamesFinished = 0

    private var rewardedLoader: RewardedAdLoader? = null
    private var rewardedAd: RewardedAd? = null
    private var onRewarded: (() -> Unit)? = null

    private var interstitialLoader: InterstitialAdLoader? = null
    private var interstitialAd: InterstitialAd? = null

    /**
     * Инициализация SDK. Вызывается только после решения пользователя о приватности:
     * [userConsent] передаётся в SDK до любых сетевых запросов рекламы
     * (false -> неперсонализированная реклама).
     */
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
        }.onFailure { initialized = false }
    }

    val rewardedReady: Boolean
        get() = initialized && cfg.rewardedEnabled && rewardedAd != null

    fun showRewarded(activity: Activity, onReward: () -> Unit) {
        val ad = rewardedAd ?: return
        onRewarded = onReward
        analytics.logEvent("rewarded_started")
        ad.setAdEventListener(object : RewardedAdEventListener {
            override fun onAdShown() = Unit

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

    /** Вызывается при завершении партии. */
    fun onGameFinished() {
        gamesFinished++
    }

    /** Показ interstitial только в естественной паузе и по частотной политике. */
    fun maybeShowInterstitial(activity: Activity) {
        val ad = interstitialAd ?: return
        if (gamesFinished < cfg.interstitialMinGames) return
        if ((gamesFinished - cfg.interstitialMinGames) % cfg.interstitialEveryGames != 0) return
        ad.setAdEventListener(object : InterstitialAdEventListener {
            override fun onAdShown() {
                analytics.logEvent("interstitial_shown")
            }

            override fun onAdFailedToShow(adError: com.yandex.mobile.ads.common.AdError) {
                analytics.logEvent("interstitial_show_failed")
                cleanupInterstitial()
            }

            override fun onAdDismissed() = cleanupInterstitial()
            override fun onAdClicked() = Unit
            override fun onAdImpression(impressionData: com.yandex.mobile.ads.common.ImpressionData?) = Unit
        })
        runCatching { ad.show(activity) }.onFailure { cleanupInterstitial() }
    }

    private fun rewardedId(): String =
        BuildConfig.REWARDED_AD_UNIT_ID.ifBlank {
            // ponytail: demo-подмена только для developer-сборок; release остаётся пустым (формат выключен)
            if (isDebugBuild()) "demo-rewarded-yandex" else ""
        }

    private fun interstitialId(): String =
        BuildConfig.INTERSTITIAL_AD_UNIT_ID.ifBlank {
            if (isDebugBuild()) "demo-interstitial-yandex" else ""
        }

    private fun isDebugBuild(): Boolean = isDebug

    private fun cleanupRewarded() {
        rewardedAd?.setAdEventListener(null)
        rewardedAd = null
        onRewarded = null
        loadRewarded()
    }

    private fun cleanupInterstitial() {
        interstitialAd?.setAdEventListener(null)
        interstitialAd = null
        loadInterstitial()
    }

    private fun loadRewarded() {
        val loader = rewardedLoader ?: return
        val id = rewardedId()
        if (id.isBlank()) return
        runCatching {
            loader.loadAd(
                AdRequest.Builder(id).build(),
                object : RewardedAdLoadListener {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        analytics.logEvent(
                            "ad_load_failed",
                            mapOf("format" to "rewarded", "code" to error.code, "desc" to error.description),
                        )
                    }
                },
            )
        }
    }

    private fun loadInterstitial() {
        val loader = interstitialLoader ?: return
        val id = interstitialId()
        if (id.isBlank()) return
        runCatching {
            loader.loadAd(
                AdRequest.Builder(id).build(),
                object : InterstitialAdLoadListener {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        analytics.logEvent(
                            "ad_load_failed",
                            mapOf("format" to "interstitial", "code" to error.code, "desc" to error.description),
                        )
                    }
                },
            )
        }
    }
}
