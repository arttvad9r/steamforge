package com.steamforge.game.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.steamforge.game.theme.SteamforgeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeUnlockedVisualTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unlockedHomeRendersPrimaryPlayAndSecondaryNavigationDeck() {
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
                    onAchievements = {},
                    onSettings = {},
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
        composeRule.waitForIdle()
    }
}
