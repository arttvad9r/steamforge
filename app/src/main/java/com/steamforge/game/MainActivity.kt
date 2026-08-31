package com.steamforge.game

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.steamforge.game.theme.SteamforgeTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var sessionStartMs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as SteamforgeApp).container

        lifecycleScope.launch {
            container.repo.progress
                .map { it.analyticsConsent }
                .distinctUntilChanged()
                .collectLatest { consent ->
                    if (consent != null) {
                        container.onConsentUpdated(consent)
                        if (consent) container.logAppOpenOnce()
                    }
                }
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            SteamforgeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainNavigation(container)
                }
            }
        }

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> sessionStartMs = System.currentTimeMillis()
                Lifecycle.Event.ON_STOP -> {
                    if (sessionStartMs > 0L) {
                        val seconds = (System.currentTimeMillis() - sessionStartMs).coerceAtLeast(0L) / 1000
                        container.analytics.logEvent("session_duration", mapOf("seconds" to seconds))
                    }
                }
                else -> Unit
            }
        })
    }
}
