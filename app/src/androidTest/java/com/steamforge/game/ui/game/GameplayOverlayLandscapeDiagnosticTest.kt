package com.steamforge.game.ui.game

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.steamforge.game.core.GameState
import com.steamforge.game.core.GameStatus
import com.steamforge.game.core.Tile
import com.steamforge.game.progression.Achievements
import com.steamforge.game.progression.FinishEffects
import com.steamforge.game.theme.Background
import com.steamforge.game.theme.SteamforgeTheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameplayOverlayLandscapeDiagnosticTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun gameOverLandscapeRenders() {
        val achievement = requireNotNull(Achievements.byId("tile_2048"))
        val ui = GameUiState(
            state = GameState(
                tiles = listOf(Tile(1L, 11, 0, 0)),
                score = 28740,
                status = GameStatus.GAME_OVER,
                won = true,
                moves = 173,
            ),
            best = 28740,
            finished = true,
            effects = FinishEffects(
                xpGained = 230,
                gemsGained = 25,
                levelUps = listOf(7),
                newAchievements = listOf(achievement),
                newBest = true,
            ),
        )
        composeRule.setContent {
            SteamforgeTheme {
                Box(Modifier.fillMaxSize().background(Background)) {
                    GameOverOverlay(ui, true, {}, {}, {})
                }
            }
        }
        composeRule.waitForIdle()
        saveRoot("game-over-landscape.png")
    }

    @Test
    fun coreOnlineLandscapeRenders() {
        composeRule.setContent {
            SteamforgeTheme {
                Box(Modifier.fillMaxSize().background(Background)) {
                    CoreOnlineOverlay(onContinue = {}, onExit = {})
                }
            }
        }
        composeRule.waitForIdle()
        saveRoot("core-online-landscape.png")
    }

    private fun saveRoot(name: String) {
        val output = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            name,
        )
        FileOutputStream(output).use { stream ->
            val written = composeRule.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream)
            assertTrue(written)
        }
        assertTrue(output.length() > 0L)
    }
}
