package com.steamforge.game

import android.app.Application
import android.content.Context
import com.steamforge.game.config.CachingRemoteConfigProvider
import com.steamforge.game.config.HttpsRemoteConfigFetcher
import com.steamforge.game.config.LocalDefaultRemoteConfigProvider
import com.steamforge.game.config.PreferencesRemoteConfigCache
import com.steamforge.game.config.RemoteConfigProvider
import com.steamforge.game.data.SteamforgeRepository
import com.steamforge.game.monetization.AdsManager
import com.steamforge.game.progression.UnavailableWeeklyRankingProvider
import com.steamforge.game.progression.WeeklyRankingProvider
import com.steamforge.game.sound.SfxPlayer
import kotlinx.coroutines.CancellationException
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
    val repo = SteamforgeRepository(appContext)
    val sfx = SfxPlayer(appContext)

    // Advertising compatibility remains temporarily isolated until gameplay call sites are removed.
    val ads = AdsManager()
    val remoteConfig: RemoteConfigProvider = createRemoteConfigProvider(appContext)
    val weeklyRankingProvider: WeeklyRankingProvider = UnavailableWeeklyRankingProvider

    init {
        appScope.launch {
            // A network/cache failure must never make startup depend on connectivity, while
            // structured-concurrency cancellation must still propagate normally.
            try {
                remoteConfig.refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Provider keeps the compiled local snapshot active.
            }
        }
        appScope.launch {
            repo.progress
                .map { it.soundEnabled }
                .distinctUntilChanged()
                .collect(sfx::setEnabled)
        }
    }
}

private fun createRemoteConfigProvider(context: Context): RemoteConfigProvider {
    val endpoint = BuildConfig.REMOTE_CONFIG_URL.trim()
    if (endpoint.isBlank()) return LocalDefaultRemoteConfigProvider()

    return CachingRemoteConfigProvider(
        cache = PreferencesRemoteConfigCache(context),
        fetcher = HttpsRemoteConfigFetcher(endpoint),
    )
}

class SteamforgeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
