package com.steamforge.game.ui.profile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.steamforge.game.progression.PermanentProfileSnapshot
import com.steamforge.game.testing.captureVisualScreenshot
import com.steamforge.game.theme.SteamforgeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileVisualTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun profileRendersQuietLedgerAndPermanentProgress() {
        val profile = PermanentProfileSnapshot(
            level = 12,
            gamesPlayed = 128,
            totalScore = 2_840_500L,
            bestScore = 61_440,
            highestTile = 4096,
            totalMerges = 8_720,
            largestCombo = 7,
            highestDailyStreak = 19,
            collectionsCompleted = 3,
            collectionsTotal = 8,
            workshopStagesCompleted = 11,
            workshopStagesTotal = 24,
            achievementsUnlocked = 8,
        )

        composeRule.setContent {
            SteamforgeTheme {
                ProfileContent(
                    profile = profile,
                    onBack = {},
                    onAchievements = {},
                )
            }
        }

        composeRule.onNodeWithText("ПРОФИЛЬ").fetchSemanticsNode()
        composeRule.onNodeWithContentDescription(
            "Профиль мастерской: уровень 12, рекорд 61440, лучшая деталь 4096",
        ).fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("ПАРТИЙ: 128").fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("ВСЕГО ОЧКОВ: 2 840 500").fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("ЛУЧШАЯ ЕЖЕДНЕВНАЯ СЕРИЯ: 19 дн.").fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("КОЛЛЕКЦИИ: 3/8").fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("МАСТЕРСКАЯ: 11/24 этапов").fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("Достижения. Открыто 8").fetchSemanticsNode()
        composeRule.waitForIdle()

        captureVisualScreenshot(
            fileName = SCREENSHOT_FILE,
            label = "Profile",
        )
    }

    private companion object {
        const val SCREENSHOT_FILE = "profile-ledger.png"
    }
}
