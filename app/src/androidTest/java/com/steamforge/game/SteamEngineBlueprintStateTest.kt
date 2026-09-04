package com.steamforge.game

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.steamforge.game.progression.BlueprintCollections
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Validation-only state injector used by the Steam Engine visual capture workflow. */
@RunWith(AndroidJUnit4::class)
class SteamEngineBlueprintStateTest {

    @Test
    fun installCompletedSteamEngineBlueprintState() {
        val app = ApplicationProvider.getApplicationContext<SteamforgeApp>()
        val allPieces = BlueprintCollections.steamEngine.pieces.map { it.id }.toSet()

        runBlocking {
            app.container.repo.updateProgress { progress ->
                progress.copy(
                    analyticsConsent = false,
                    blueprintPieces = allPieces,
                )
            }
        }

        assertTrue(BlueprintCollections.isSteamEngineComplete(allPieces))
    }
}
