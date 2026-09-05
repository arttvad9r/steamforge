package com.steamforge.game

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import com.steamforge.game.analytics.AppMetricaAnalytics
import com.steamforge.game.analytics.MutableAnalytics
import com.steamforge.game.analytics.NoopAnalytics
import com.steamforge.game.config.LocalDefaultRemoteConfigProvider
import com.steamforge.game.config.RemoteConfigProvider
import com.steamforge.game.data.SteamforgeRepository
import com.steamforge.game.monetization.AdsManager
import com.steamforge.game.sound.SfxPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Ручной DI: один контейнер на процесс. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val isDebug: Boolean = (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val repo = SteamforgeRepository(appContext)
    val sfx = SfxPlayer(appContext)

    val analytics = MutableAnalytics(NoopAnalytics(debugLogging = isDebug), debugLogging = isDebug)
    val ads = AdsManager(analytics, isDebug = isDebug)
    val remoteConfig: RemoteConfigProvider = LocalDefaultRemoteConfigProvider()

    private var metrica: AppMetricaAnalytics? = null
    private var adsInitialized = false
    private var appOpenLogged = false

    init {
        appScope.launch {
            remoteConfig.refresh()
        }
        appScope.launch {
            repo.progress
                .map { it.soundEnabled }
                .distinctUntilChanged()
                .collect(sfx::setEnabled)
        }
    }

    fun onConsentUpdated(granted: Boolean) {
        if (granted && metrica == null && BuildConfig.APPMETRICA_API_KEY.isNotBlank()) {
            metrica = runCatching {
                AppMetricaAnalytics(appContext, BuildConfig.APPMETRICA_API_KEY, debugLogging = isDebug)
            }.getOrNull()
        }
        metrica?.setSendingEnabled(granted)
        analytics.setDelegate(if (granted) metrica else null)

        // Legacy ad integration is frozen by docs/ADR_0001_NO_ADS.md. The master switch stays off;
        // do not initialize, load or expose ad surfaces unless a later accepted ADR supersedes it.
        if (ads.enabled) {
            if (!adsInitialized) {
                adsInitialized = true
                runCatching { ads.init(appContext, userConsent = granted) }
            } else {
                runCatching { com.yandex.mobile.ads.common.YandexAds.setUserConsent(granted) }
            }
        }

        if (isDebug && BuildConfig.APPMETRICA_API_KEY.isBlank()) {
            println("Steamforge: APPMETRICA_API_KEY не задан (steamforge.appmetricaApiKey) — аналитика отключена")
        }
    }

    fun logAppOpenOnce() {
        if (appOpenLogged) return
        appOpenLogged = true
        analytics.logEvent("app_open")
    }
}

class SteamforgeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
