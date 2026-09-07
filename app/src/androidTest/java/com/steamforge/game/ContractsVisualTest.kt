package com.steamforge.game

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.steamforge.game.progression.ContractDef
import com.steamforge.game.progression.ContractReward
import com.steamforge.game.progression.ContractType
import com.steamforge.game.theme.SteamforgeTheme
import com.steamforge.game.ui.contracts.ContractItemUi
import com.steamforge.game.ui.contracts.ContractsContent
import com.steamforge.game.ui.contracts.ContractsUiState
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContractsVisualTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contractTaskListRendersClearRewardAndProgressStates() {
        val mergeContract = ContractDef(
            id = "merge_shift",
            type = ContractType.MERGE_COUNT,
            target = 24,
            reward = ContractReward.WorkshopParts(30),
            title = "Соберите 24 механизма",
            description = "Объединяйте одинаковые плитки в обычных партиях",
        )
        val blueprintContract = ContractDef(
            id = "blueprint_shift",
            type = ContractType.SCORE,
            target = 12_000,
            reward = ContractReward.BlueprintPiece(
                collectionId = "steam_engine",
                fallbackParts = 55,
            ),
            title = "Наберите 12 000 очков",
            description = "Лучший результат одной партии засчитывается автоматически",
        )
        val runContract = ContractDef(
            id = "runs_shift",
            type = ContractType.PLAY_RUNS,
            target = 2,
            reward = ContractReward.WorkshopParts(20),
            title = "Завершите 2 партии",
            description = "Любая завершённая обычная партия идёт в прогресс",
        )

        composeRule.setContent {
            SteamforgeTheme {
                ContractsContent(
                    ui = ContractsUiState(
                        day = 20_000L,
                        workshopParts = 145,
                        items = listOf(
                            ContractItemUi(
                                def = mergeContract,
                                progress = 11,
                                claimed = false,
                                recommended = true,
                            ),
                            ContractItemUi(
                                def = blueprintContract,
                                progress = 12_000,
                                claimed = false,
                            ),
                            ContractItemUi(
                                def = runContract,
                                progress = 2,
                                claimed = true,
                            ),
                        ),
                        firstContractOnboarding = true,
                    ),
                    onBack = {},
                    onClaim = {},
                )
            }
        }

        composeRule.onNodeWithText("КОНТРАКТЫ").fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("Контракты сегодня: выполнено 2 из 3").fetchSemanticsNode()
        composeRule.onNodeWithContentDescription(
            "Рекомендуемый первый контракт. Соберите 24 механизма: 11 из 24. Награда 30 деталей мастерской",
        ).fetchSemanticsNode()
        composeRule.onNodeWithContentDescription(
            "Наберите 12 000 очков: 12000 из 12000. Награда: фрагмент чертежа",
        ).fetchSemanticsNode()
        composeRule.onNodeWithContentDescription(
            "Завершите 2 партии: 2 из 2. Награда 20 деталей мастерской",
        ).fetchSemanticsNode()
        composeRule.waitForIdle()

        val output = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            SCREENSHOT_FILE,
        )
        FileOutputStream(output).use { stream ->
            val written = composeRule.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream)
            assertTrue("Contracts screenshot compression failed", written)
        }
        assertTrue("Contracts screenshot was not written", output.length() > 0L)
    }

    private companion object {
        const val SCREENSHOT_FILE = "contracts-task-list.png"
    }
}
