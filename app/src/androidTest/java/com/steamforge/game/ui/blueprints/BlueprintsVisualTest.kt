package com.steamforge.game.ui.blueprints

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.steamforge.game.progression.BlueprintCollections
import com.steamforge.game.testing.captureVisualScreenshot
import com.steamforge.game.theme.SteamforgeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlueprintsVisualTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun collectionSurfaceShowsPersistedSteamEnginePieces() {
        val pieces = BlueprintCollections.steamEngine.pieces
        composeRule.setContent {
            SteamforgeTheme {
                BlueprintsContent(
                    ui = BlueprintsUiState(
                        loaded = true,
                        ownedPieceIds = pieces.take(3).map { it.id }.toSet(),
                        steamEngineOwned = 3,
                        steamEngineTotal = pieces.size,
                        steamEngineComplete = false,
                        animationsEnabled = false,
                    ),
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Чертёж Steam Engine: собрано 3 из 6 частей").fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("Детали Steam Engine: собрано 3 из 6").fetchSemanticsNode()
        composeRule.onNodeWithText("Котёл").fetchSemanticsNode()
        composeRule.onNodeWithText("Манометр").fetchSemanticsNode()
        composeRule.waitForIdle()

        captureVisualScreenshot(
            fileName = SCREENSHOT_FILE,
            label = "Blueprints",
        )
    }

    private companion object {
        const val SCREENSHOT_FILE = "blueprints-catalog.png"
    }
}
