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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.steamforge.game.theme.SteamforgeTheme

class MainActivity : ComponentActivity() {

    private var storeScreenshotMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as SteamforgeApp).container
        storeScreenshotMode = BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_STORE_SCREENSHOT, false)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (storeScreenshotMode) hideSystemBarsForStoreCapture()

        setContent {
            SteamforgeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainNavigation(container)
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && storeScreenshotMode) hideSystemBarsForStoreCapture()
    }

    private fun hideSystemBarsForStoreCapture() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private companion object {
        const val EXTRA_STORE_SCREENSHOT = "store_screenshot"
    }
}
