package com.steamforge.game.ui.settings

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.steamforge.game.theme.SteamforgeTheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsVisualTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsRendersGameControlsAndDestructiveDataAction() {
        composeRule.setContent {
            SteamforgeTheme {
                SettingsContent(
                    ui = SettingsUiState(
                        soundEnabled = true,
                        hapticsEnabled = false,
                        animationsEnabled = true,
                    ),
                    onBack = {},
                    onSoundChange = {},
                    onHapticsChange = {},
                    onAnimationsChange = {},
                    onReset = {},
                )
            }
        }

        composeRule.onNodeWithText("Настройки").fetchSemanticsNode()
        composeRule.onNodeWithText("Звуковые эффекты").fetchSemanticsNode()
        composeRule.onNodeWithText("Виброотклик на действия").fetchSemanticsNode()
        composeRule.onNodeWithText("Визуальные эффекты и движение").fetchSemanticsNode()
        composeRule.onNodeWithText("СБРОСИТЬ ПРОГРЕСС").fetchSemanticsNode()
        composeRule.waitForIdle()

        val output = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            SCREENSHOT_FILE,
        )
        FileOutputStream(output).use { stream ->
            val written = composeRule.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream)
            assertTrue("Settings screenshot compression failed", written)
        }
        assertTrue("Settings screenshot was not written", output.length() > 0L)
    }

    private companion object {
        const val SCREENSHOT_FILE = "settings-controls.png"
    }
}
