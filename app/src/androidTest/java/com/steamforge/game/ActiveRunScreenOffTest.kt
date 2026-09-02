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
import com.steamforge.game.progression.Onboarding
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveRunScreenOffTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun activeRunSurvivesScreenOffAndWakeInSameProcess() {
        val app = ApplicationProvider.getApplicationContext<SteamforgeApp>()
        runBlocking {
            app.container.repo.clearGame()
            app.container.repo.updateProgress { it.copy(analyticsConsent = false) }
            app.container.onboarding.setStep(Onboarding.COMPLETE)
            app.container.repo.saveGame(testRun)
        }

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        if (!device.isScreenOn) {
            device.wakeUp()
            waitForScreen(device, expectedOn = true)
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            waitForText("ПРОДОЛЖИТЬ")
            composeRule.onNodeWithText("ПРОДОЛЖИТЬ").performClick()
            waitForContentDescription(SCORE_DESCRIPTION)
            composeRule.onNodeWithText(MOVE_TEXT).fetchSemanticsNode()

            val beforePid = Process.myPid()
            val beforeTiles = tileBounds()
            assertTrue("Screen must be on before the interruption", device.isScreenOn)

            device.sleep()
            waitForScreen(device, expectedOn = false)
            assertFalse("UiDevice.sleep() must turn the default display off", device.isScreenOn)
            assertEquals("Screen off must not replace the Steamforge Linux process", beforePid, Process.myPid())

            device.wakeUp()
            waitForScreen(device, expectedOn = true)
            assertTrue("UiDevice.wakeUp() must turn the default display on", device.isScreenOn)
            device.executeShellCommand("wm dismiss-keyguard")

            waitForContentDescription(SCORE_DESCRIPTION)
            composeRule.onNodeWithText(MOVE_TEXT).fetchSemanticsNode()
            val afterTiles = tileBounds()

            assertEquals("Screen off/wake must keep the Steamforge Linux process alive", beforePid, Process.myPid())
            assertEquals("Exact tile geometry changed across screen off/wake", beforeTiles, afterTiles)
        }
    }

    private fun waitForScreen(device: UiDevice, expectedOn: Boolean) {
        val deadline = SystemClock.uptimeMillis() + SCREEN_STATE_TIMEOUT_MS
        while (SystemClock.uptimeMillis() < deadline) {
            if (device.isScreenOn == expectedOn) return
            SystemClock.sleep(100)
        }
        assertEquals("Default display did not reach expected power state", expectedOn, device.isScreenOn)
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForContentDescription(description: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tileBounds(): Map<String, Rect> = TILE_DESCRIPTIONS.associateWith { description ->
        composeRule.onNodeWithContentDescription(description).getBoundsInRoot().let { bounds ->
            Rect(
                left = bounds.left.value,
                top = bounds.top.value,
                right = bounds.right.value,
                bottom = bounds.bottom.value,
            )
        }
    }

    private companion object {
        const val SCREEN_STATE_TIMEOUT_MS = 5_000L
        const val SCORE_DESCRIPTION = "СЧЁТ: 96"
        const val MOVE_TEXT = "ХОД 7"

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
                    Tile(id = 1L, level = 1, row = 0, col = 0),
                    Tile(id = 2L, level = 2, row = 0, col = 3),
                    Tile(id = 3L, level = 3, row = 2, col = 1),
                    Tile(id = 4L, level = 4, row = 3, col = 2),
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
