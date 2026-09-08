package com.steamforge.game.ui.achievements

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.steamforge.game.progression.Achievements
import com.steamforge.game.testing.captureVisualScreenshot
import com.steamforge.game.theme.SteamforgeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AchievementsVisualTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun achievementsRenderCollectionRegistryStates() {
        val items = listOf(
            AchievementUi(
                def = requireNotNull(Achievements.byId("merge_1")),
                unlocked = true,
                unlockDate = "04.09.2026",
                progress = 1,
            ),
            AchievementUi(
                def = requireNotNull(Achievements.byId("games_50")),
                unlocked = false,
                unlockDate = null,
                progress = 18,
            ),
            AchievementUi(
                def = requireNotNull(Achievements.byId("daily_7")),
                unlocked = false,
                unlockDate = null,
                progress = 3,
            ),
            AchievementUi(
                def = requireNotNull(Achievements.byId("tile_2048")),
                unlocked = false,
                unlockDate = null,
                progress = 0,
            ),
            AchievementUi(
                def = requireNotNull(Achievements.byId("gems_500")),
                unlocked = false,
                unlockDate = null,
                progress = 0,
            ),
        )

        composeRule.setContent {
            SteamforgeTheme {
                AchievementsContent(
                    achievementItems = items,
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("КОЛЛЕКЦИЯ").fetchSemanticsNode()
        composeRule.onNodeWithText("РЕЕСТР ДОСТИЖЕНИЙ").fetchSemanticsNode()
        composeRule.onNodeWithContentDescription(
            "Коллекция: открыто 1 из 5 достижений, в работе 2",
        ).fetchSemanticsNode()
        composeRule.onNodeWithContentDescription(
            "Первый стык: разблокировано. Награда 3 гемов",
        ).fetchSemanticsNode()
        composeRule.onNodeWithContentDescription(
            "Ветеран цеха: в прогрессе 18 из 50. Награда 15 гемов",
        ).fetchSemanticsNode()
        composeRule.onNodeWithContentDescription(
            "Собран 2048: заблокировано. Награда 25 гемов",
        ).fetchSemanticsNode()
        composeRule.onNodeWithContentDescription(
            "Скрытое достижение: скрыто. Награда 25 гемов",
        ).fetchSemanticsNode()
        composeRule.waitForIdle()

        captureVisualScreenshot(
            fileName = SCREENSHOT_FILE,
            label = "Achievements",
        )
    }

    private companion object {
        const val SCREENSHOT_FILE = "achievements-registry.png"
    }
}
