package com.steamforge.game

import android.os.Process
import android.os.SystemClock
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.steamforge.game.core.GameState
import com.steamforge.game.core.GameStatus
import com.steamforge.game.core.Tile
import com.steamforge.game.data.SavedGame
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveRunScreenOffHomeTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun seededRunKeepsExactBoardAcrossScreenOffAndWake() {
        val app = ApplicationProvider.getApplicationContext<SteamforgeApp>()
        runBlocking {
            app.container.repo.clearGame()
            app.container.repo.updateProgress { it.copy(analyticsConsent = false) }
            app.container.repo.saveGame(testRun)
        }

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        if (!device.isScreenOn) {
            device.wakeUp()
            waitForScreen(device, true)
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            waitForText("ПРОДОЛЖИТЬ")
            composeRule.onNodeWithText("ПРОДОЛЖИТЬ").performClick()
            TILE_DESCRIPTIONS.forEach(::waitForTile)

            val pid = Process.myPid()
            val before = tileBounds()

            device.sleep()
            waitForScreen(device, false)
            assertEquals(pid, Process.myPid())

            device.wakeUp()
            waitForScreen(device, true)
            device.executeShellCommand("wm dismiss-keyguard")
            TILE_DESCRIPTIONS.forEach(::waitForTile)

            assertEquals(pid, Process.myPid())
            assertEquals(before, tileBounds())
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTile(description: String) {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForScreen(device: UiDevice, on: Boolean) {
        val deadline = SystemClock.uptimeMillis() + 5_000L
        while (SystemClock.uptimeMillis() < deadline) {
            if (device.isScreenOn == on) return
            SystemClock.sleep(100)
        }
        assertEquals(on, device.isScreenOn)
    }

    private fun tileBounds(): Map<String, Rect> = TILE_DESCRIPTIONS.associateWith { description ->
        composeRule.onNodeWithContentDescription(description).getBoundsInRoot().let { bounds ->
            Rect(bounds.left.value, bounds.top.value, bounds.right.value, bounds.bottom.value)
        }
    }

    private companion object {
        val TILE_DESCRIPTIONS = listOf(
            "Уголь, 2",
            "Медная шестерня, 4",
            "Клапан, 8",
            "Поршень, 16",
        )

        val testRun = SavedGame(
            state = GameState(
                size = 4,
                tiles = listOf(
                    Tile(1L, 1, 0, 0),
                    Tile(2L, 2, 0, 3),
                    Tile(3L, 3, 2, 1),
                    Tile(4L, 4, 3, 2),
                ),
                score = 96,
                nextTileId = 5L,
                status = GameStatus.PLAYING,
                won = false,
                moves = 7,
            ),
            seed = 0x5EEDL,
            pressure = 37,
            overdriveRemaining = 2,
            freeUndosLeft = 1,
            rngDraws = 9L,
            mergesTotal = 6,
            maxMergesInOneMove = 2,
            overdrivesSession = 1,
            undosSession = 1,
            highMergesSession = 2,
        )
    }
}
