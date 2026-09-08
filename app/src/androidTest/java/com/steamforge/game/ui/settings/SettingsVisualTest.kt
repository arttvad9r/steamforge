package com.steamforge.game.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.steamforge.game.testing.captureVisualScreenshot
import com.steamforge.game.theme.SteamforgeTheme
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

        captureVisualScreenshot(
            fileName = SCREENSHOT_FILE,
            label = "Settings",
        )
    }

    private companion object {
        const val SCREENSHOT_FILE = "settings-controls.png"
    }
}
