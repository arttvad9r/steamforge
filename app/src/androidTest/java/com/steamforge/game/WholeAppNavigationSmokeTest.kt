package com.steamforge.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.steamforge.game.progression.PlayerStats
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Production-navigation smoke for the player-facing meta surfaces that remain part of the current
 * product. This intentionally exercises MainActivity/MainNavigation rather than isolated screen
 * composables, so an orphaned route or broken entry point fails on the emulator.
 */
@RunWith(AndroidJUnit4::class)
class WholeAppNavigationSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val repo
        get() = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SteamforgeApp)
            .container.repo

    @Before
    fun seedUnlockedMetaState() = runBlocking {
        repo.resetGameProgress()
        repo.updateProgress { progress ->
            progress.copy(
                bestScore = 16_384,
                stats = PlayerStats(
                    gamesPlayed = 3,
                    bestScore = 16_384,
                    totalScore = 24_576,
                    maxTileLevel = 10,
                    totalMerges = 96,
                    maxMergesInOneMove = 3,
                ),
                workshopParts = 80,
                workshopCoreStage = 2,
            )
        }
    }

    @After
    fun clearSeededState() = runBlocking {
        repo.resetGameProgress()
    }

    @Test
    fun unlockedMetaDestinationsAreReachableThroughProductionNavigation() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("МАСТЕРСКАЯ").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription("Коллекция").performClick()
        composeRule.onNodeWithText("ЧЕРТЕЖИ").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Назад").performClick()

        composeRule.onNodeWithContentDescription("Контракты. 3 задания сегодня. Награды за игру")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("КОНТРАКТЫ").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Назад").performClick()

        // Profile used to exist only as an orphaned route. The unlocked Home entry is now the
        // normal player-facing path into permanent statistics and then Achievements.
        composeRule.onNodeWithContentDescription("Профиль")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("ПРОФИЛЬ").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Достижения. Открыто 0")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("ДОСТИЖЕНИЯ").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.onNodeWithContentDescription("Назад").performClick()

        composeRule.onNodeWithContentDescription("Мастерская. Ядро · LV 1 · серия 0")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Мастерская").assertIsDisplayed()
        pressBack()

        composeRule.onNodeWithText("STEAMFORGE").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Настройки")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("НАСТРОЙКИ").assertIsDisplayed()
    }
}
