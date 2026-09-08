package com.steamforge.game

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.steamforge.game.progression.LevelInfo
import com.steamforge.game.progression.WorkshopMechanism
import com.steamforge.game.theme.Background
import com.steamforge.game.theme.SteamforgeTheme
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.ui.workshop.SteamEngineBlueprintModule
import com.steamforge.game.ui.workshop.WorkshopHero
import com.steamforge.game.ui.workshop.WorkshopMechanismUi
import com.steamforge.game.ui.workshop.WorkshopMetaDock
import com.steamforge.game.ui.workshop.WorkshopUpgradeDeck
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkshopBlueprintVisualTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun blueprintStatesRenderWithProductionWorkshopStyling() {
        composeRule.setContent {
            SteamforgeTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background)
                        .padding(18.dp),
                ) {
                    SteamEngineBlueprintModule(
                        piecesOwned = 3,
                        piecesTotal = 6,
                        unlocked = false,
                        animationsEnabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(18.dp))
                    SteamEngineBlueprintModule(
                        piecesOwned = 6,
                        piecesTotal = 6,
                        unlocked = true,
                        animationsEnabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Чертёж Steam Engine: собрано 3 из 6 частей").fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("Steam Engine собран и установлен в мастерской").fetchSemanticsNode()
        composeRule.waitForIdle()
        saveRootScreenshot(BLUEPRINT_SCREENSHOT_FILE, "Workshop blueprint")
    }

    @Test
    fun machineryHeroRendersAsPrimaryWorkshopModule() {
        composeRule.setContent {
            SteamforgeTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background)
                        .padding(18.dp),
                ) {
                    WorkshopHero(
                        level = 7,
                        levelInfo = LevelInfo(level = 7, xpIntoLevel = 164, xpToNext = 360),
                        animationsEnabled = false,
                        accent = TealGlow,
                        gamesPlayed = 18,
                        bestScore = 61_440,
                        coreStage = 3,
                        coreStageLabel = "РАБОТАЕТ",
                        pressureStage = 2,
                        gearPressStage = 1,
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription(
            "Цех мастерской. Ядро: стадия 3. Генератор: стадия 2. Пресс: стадия 1",
        ).fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("Прогресс уровня: 45 процентов").fetchSemanticsNode()
        composeRule.waitForIdle()
        saveRootScreenshot(HERO_SCREENSHOT_FILE, "Workshop machinery hero")
    }

    @Test
    fun machineryHeroMakesRestorationVisibleAcrossStages() {
        composeRule.setContent {
            SteamforgeTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background)
                        .padding(18.dp),
                ) {
                    WorkshopHero(
                        level = 1,
                        levelInfo = LevelInfo(level = 1, xpIntoLevel = 0, xpToNext = 120),
                        animationsEnabled = false,
                        accent = TealGlow,
                        gamesPlayed = 1,
                        bestScore = 1_024,
                        coreStage = 0,
                        coreStageLabel = "СЛОМАНО",
                        pressureStage = 0,
                        gearPressStage = 0,
                    )
                    Spacer(Modifier.height(12.dp))
                    WorkshopHero(
                        level = 12,
                        levelInfo = LevelInfo(level = 12, xpIntoLevel = 420, xpToNext = 600),
                        animationsEnabled = false,
                        accent = TealGlow,
                        gamesPlayed = 84,
                        bestScore = 131_072,
                        coreStage = 4,
                        coreStageLabel = "УСИЛЕНО",
                        pressureStage = 4,
                        gearPressStage = 4,
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription(
            "Цех мастерской. Ядро: стадия 0. Генератор: стадия 0. Пресс: стадия 0",
        ).fetchSemanticsNode()
        composeRule.onNodeWithContentDescription(
            "Цех мастерской. Ядро: стадия 4. Генератор: стадия 4. Пресс: стадия 4",
        ).fetchSemanticsNode()
        composeRule.waitForIdle()
        saveRootScreenshot(RESTORATION_SCREENSHOT_FILE, "Workshop restoration states")
    }

    @Test
    fun upgradeBaysRenderDistinctMechanismStates() {
        composeRule.setContent {
            SteamforgeTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background)
                        .padding(18.dp),
                ) {
                    WorkshopUpgradeDeck(
                        mechanisms = listOf(
                            WorkshopMechanismUi(
                                mechanism = WorkshopMechanism.CORE,
                                stage = 4,
                                stageLabel = "УСИЛЕНО",
                                nextCost = null,
                                canUpgrade = false,
                            ),
                            WorkshopMechanismUi(
                                mechanism = WorkshopMechanism.PRESSURE_GENERATOR,
                                stage = 2,
                                stageLabel = "МЕХАНИЗМЫ",
                                nextCost = 55,
                                canUpgrade = true,
                            ),
                            WorkshopMechanismUi(
                                mechanism = WorkshopMechanism.GEAR_PRESS,
                                stage = 1,
                                stageLabel = "КАРКАС",
                                nextCost = 35,
                                canUpgrade = false,
                            ),
                        ),
                        onUpgrade = {},
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription(
            "МЕХАНИЧЕСКОЕ ЯДРО: УСИЛЕНО. Узел полностью улучшен",
        ).fetchSemanticsNode()
        composeRule.onNodeWithContentDescription(
            "ГЕНЕРАТОР ДАВЛЕНИЯ: МЕХАНИЗМЫ. Улучшить за 55 деталей",
        ).fetchSemanticsNode()
        composeRule.onNodeWithContentDescription(
            "ШЕСТЕРЁНОЧНЫЙ ПРЕСС: КАРКАС. Нужно 35 деталей",
        ).fetchSemanticsNode()
        composeRule.waitForIdle()
        saveRootScreenshot(UPGRADES_SCREENSHOT_FILE, "Workshop upgrade bays")
    }

    @Test
    fun metaDockRendersDailyAndRewardAsSecondaryWorkshopActions() {
        composeRule.setContent {
            SteamforgeTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background)
                        .padding(18.dp),
                ) {
                    WorkshopMetaDock(
                        dailyDone = false,
                        dailyRewardAvailable = true,
                        dailyRewardDay = 3,
                        dailyRewardGems = 6,
                        dailyRewardWorkshopParts = 2,
                        onDaily = {},
                        onClaimReward = {},
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription(
            "Испытание дня. Новая задача на сегодня. ОТКРЫТЬ",
        ).fetchSemanticsNode()
        composeRule.onNodeWithContentDescription(
            "Ежедневная награда. День 3 · ◆ +6 · ⚙ +2. ПОЛУЧИТЬ",
        ).fetchSemanticsNode()
        composeRule.waitForIdle()
        saveRootScreenshot(META_DOCK_SCREENSHOT_FILE, "Workshop meta dock")
    }

    private fun saveRootScreenshot(fileName: String, label: String) {
        val output = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            fileName,
        )
        FileOutputStream(output).use { stream ->
            val written = composeRule.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream)
            assertTrue("$label screenshot compression failed", written)
        }
        assertTrue("$label screenshot was not written", output.length() > 0L)
    }

    private companion object {
        const val BLUEPRINT_SCREENSHOT_FILE = "workshop-blueprint.png"
        const val HERO_SCREENSHOT_FILE = "workshop-hero.png"
        const val RESTORATION_SCREENSHOT_FILE = "workshop-restoration.png"
        const val UPGRADES_SCREENSHOT_FILE = "workshop-upgrades.png"
        const val META_DOCK_SCREENSHOT_FILE = "workshop-meta-dock.png"
    }
}
