package com.steamforge.game

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.steamforge.game.core.GameState
import com.steamforge.game.core.GameStatus
import com.steamforge.game.core.Tile
import com.steamforge.game.theme.Background
import com.steamforge.game.theme.SteamforgeTheme
import com.steamforge.game.ui.game.BoardView
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HighTierBoardReadabilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun highTierTilesRenderWithProductionBoardStyling() {
        composeRule.setContent {
            SteamforgeTheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(Background),
                    contentAlignment = Alignment.Center,
                ) {
                    BoardView(
                        state = highTierState,
                        lastResult = null,
                        previousTiles = emptyList(),
                        animationsActive = false,
                        removingMode = false,
                        canRemove = { false },
                        onTileClick = { },
                        onSwipe = { },
                        modifier = Modifier.fillMaxWidth().padding(24.dp).aspectRatio(1f),
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Турбина, 512").assertExists()
        composeRule.onNodeWithContentDescription("Реактор, 1024").assertExists()
        composeRule.onNodeWithContentDescription("Механическое ядро, 2048").assertExists()
        composeRule.waitForIdle()

        val output = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            SCREENSHOT_FILE,
        )
        FileOutputStream(output).use { stream ->
            val written = composeRule.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream)
            assertTrue("High-tier screenshot compression failed", written)
        }
        assertTrue("High-tier screenshot was not written", output.length() > 0L)
    }

    private companion object {
        const val SCREENSHOT_FILE = "high-tier-board.png"

        val highTierState = GameState(
            size = 4,
            tiles = listOf(
                Tile(id = 1L, level = 9, row = 0, col = 0),
                Tile(id = 2L, level = 10, row = 0, col = 1),
                Tile(id = 3L, level = 11, row = 0, col = 2),
                Tile(id = 4L, level = 8, row = 1, col = 0),
                Tile(id = 5L, level = 9, row = 1, col = 1),
                Tile(id = 6L, level = 10, row = 1, col = 2),
                Tile(id = 7L, level = 11, row = 1, col = 3),
            ),
            score = 8192,
            nextTileId = 8L,
            status = GameStatus.PLAYING,
            won = true,
            moves = 64,
        )
    }
}
