package com.steamforge.game.ui.home

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.steamforge.game.testing.captureVisualScreenshot
import com.steamforge.game.theme.SteamforgeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeUnlockedVisualTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unlockedHomeRendersPrimaryPlayAndSecondaryNavigationDeck() {
        val collectionOpened = booleanArrayOf(false)
        val profileOpened = booleanArrayOf(false)
        composeRule.setContent {
            SteamforgeTheme {
                HomeContent(
                    ui = HomeUiState(
                        loaded = true,
                        gems = 84,
                        bestScore = 61_440,
                        workshopLevel = 7,
                        achievementsUnlocked = 8,
                        dailyDone = false,
                        dailyRewardStreak = 4,
                        hasSavedRun = true,
                        featureVisibility = HomeFeatureVisibility(
                            showStatusRail = true,
                            showWorkshop = true,
                            showContracts = true,
                            showDaily = true,
                            showCollection = true,
                        ),
                    ),
                    onPlay = {},
                    onWorkshop = {},
                    onContracts = {},
                    onDaily = {},
                    onCollection = { collectionOpened[0] = true },
                    onSettings = {},
                    onProfile = { profileOpened[0] = true },
                )
            }
        }

        composeRule.onNodeWithText("ПРОДОЛЖИТЬ").fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("Мастерская. Ядро · LV 7 · серия 4").fetchSemanticsNode()
        composeRule.onNodeWithContentDescription(
            "Контракты. 3 задания сегодня. Награды за игру",
        ).fetchSemanticsNode()
        composeRule.onNodeWithContentDescription(
            "Испытание дня. Новая задача на сегодня",
        ).fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("Коллекция").fetchSemanticsNode()
        val profile = composeRule.onNode(hasContentDescription("Профиль", substring = true))
        profile.fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("Коллекция").performClick()
        profile.performClick()
        composeRule.waitForIdle()
        assertTrue("Collection control did not invoke collection navigation", collectionOpened[0])
        assertTrue("Profile control did not invoke profile navigation", profileOpened[0])

        captureVisualScreenshot(
            fileName = SCREENSHOT_FILE,
            label = "Unlocked Home",
        )
    }

    private companion object {
        const val SCREENSHOT_FILE = "home-unlocked.png"
    }
}
