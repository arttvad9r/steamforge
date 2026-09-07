package com.steamforge.game.ui.game

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
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
import com.steamforge.game.progression.DailyChallenge
import com.steamforge.game.progression.DailyGoalType
import com.steamforge.game.theme.Background
import com.steamforge.game.theme.SteamforgeTheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyObjectiveVisualTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dailyObjectiveInProgressRendersAboveProductionBoard() {
        renderAndCapture(
            satisfied = false,
            expectedDescription = "Испытание дня. Собери деталь 512. В процессе. Награда 15 гемов",
            screenshotFile = ACTIVE_SCREENSHOT,
        )
    }

    @Test
    fun dailyObjectiveCompleteRendersAsResolvedState() {
        renderAndCapture(
            satisfied = true,
            expectedDescription = "Испытание дня. Собери деталь 512. Выполнено. Награда 15 гемов",
            screenshotFile = COMPLETE_SCREENSHOT,
        )
    }

    private fun renderAndCapture(
        satisfied: Boolean,
        expectedDescription: String,
        screenshotFile: String,
    ) {
        composeRule.setContent {
            SteamforgeTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background)
                        .padding(horizontal = 14.dp, vertical = 18.dp),
                ) {
                    DailyObjectiveStrip(
                        daily = daily,
                        satisfied = satisfied,
                    )
                    Spacer(Modifier.height(8.dp))
                    BoardView(
                        state = boardState,
                        lastResult = null,
                        previousTiles = emptyList(),
                        animationsActive = false,
                        removingMode = false,
                        canRemove = { false },
                        onTileClick = { },
                        onSwipe = { },
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription(expectedDescription).fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("Механическое ядро, 2048").fetchSemanticsNode()
        composeRule.waitForIdle()

        val output = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            screenshotFile,
        )
        FileOutputStream(output).use { stream ->
            val written = composeRule.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream)
            assertTrue("Daily objective screenshot compression failed", written)
        }
        assertTrue("Daily objective screenshot was not written", output.length() > 0L)
    }

    private companion object {
        const val ACTIVE_SCREENSHOT = "daily-objective-active.png"
        const val COMPLETE_SCREENSHOT = "daily-objective-complete.png"

        val daily = DailyChallenge(
            epochDay = 0L,
            type = DailyGoalType.REACH_TILE,
            target = 512,
            mergeLevel = 6,
            seed = 42L,
            rewardGems = 15,
            bonusXp = 60,
        )

        val boardState = GameState(
            size = 4,
            tiles = listOf(
                Tile(id = 1L, level = 1, row = 0, col = 0),
                Tile(id = 2L, level = 4, row = 1, col = 1),
                Tile(id = 3L, level = 8, row = 2, col = 2),
                Tile(id = 4L, level = 11, row = 3, col = 3),
            ),
            score = 4_280,
            nextTileId = 5L,
            status = GameStatus.PLAYING,
            won = true,
            moves = 48,
        )
    }
}
