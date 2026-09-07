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
import com.steamforge.game.theme.Background
import com.steamforge.game.theme.SteamforgeTheme
import com.steamforge.game.ui.workshop.SteamEngineBlueprintModule
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

        val output = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            SCREENSHOT_FILE,
        )
        FileOutputStream(output).use { stream ->
            val written = composeRule.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream)
            assertTrue("Workshop blueprint screenshot compression failed", written)
        }
        assertTrue("Workshop blueprint screenshot was not written", output.length() > 0L)
    }

    private companion object {
        const val SCREENSHOT_FILE = "workshop-blueprint.png"
    }
}
