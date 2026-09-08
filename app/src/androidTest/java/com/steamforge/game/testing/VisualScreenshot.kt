package com.steamforge.game.testing

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue

internal fun captureVisualScreenshot(
    fileName: String,
    label: String,
    minBytes: Long = 1L,
) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.waitForIdleSync()
    val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) {
        "$label screenshot capture returned null"
    }
    val output = File(instrumentation.targetContext.cacheDir, fileName)
    try {
        FileOutputStream(output).use { stream ->
            val written = screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream)
            assertTrue("$label screenshot compression failed", written)
        }
    } finally {
        screenshot.recycle()
    }
    assertTrue(
        "$label screenshot looks blank or incomplete: ${output.length()} bytes",
        output.length() > minBytes,
    )
}
