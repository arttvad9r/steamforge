package com.steamforge.game.testing

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue

internal fun captureVisualScreenshot(
    fileName: String,
    label: String,
) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.waitForIdleSync()
    val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) {
        "$label screenshot capture returned null"
    }
    val output = File(instrumentation.targetContext.cacheDir, fileName)
    try {
        assertScreenshotHasVisualContent(screenshot, label)
        FileOutputStream(output).use { stream ->
            val written = screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream)
            assertTrue("$label screenshot compression failed", written)
        }
    } finally {
        screenshot.recycle()
    }
    assertTrue("$label screenshot was not written", output.length() > 0L)
}

private fun assertScreenshotHasVisualContent(bitmap: Bitmap, label: String) {
    assertTrue("$label screenshot width is too small: ${bitmap.width}", bitmap.width >= 720)
    assertTrue("$label screenshot height is too small: ${bitmap.height}", bitmap.height >= 1_280)

    val quantizedColors = mutableSetOf<Int>()
    var minLuma = 255
    var maxLuma = 0
    val left = bitmap.width / 10
    val right = bitmap.width * 9 / 10
    val top = bitmap.height / 10
    val bottom = bitmap.height * 9 / 10
    val stepX = ((right - left) / 24).coerceAtLeast(1)
    val stepY = ((bottom - top) / 40).coerceAtLeast(1)

    var y = top
    while (y < bottom) {
        var x = left
        while (x < right) {
            val pixel = bitmap.getPixel(x, y)
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            val luma = (red * 299 + green * 587 + blue * 114) / 1_000
            minLuma = minOf(minLuma, luma)
            maxLuma = maxOf(maxLuma, luma)
            quantizedColors += ((red shr 4) shl 8) or ((green shr 4) shl 4) or (blue shr 4)
            x += stepX
        }
        y += stepY
    }

    assertTrue(
        "$label screenshot has too little visual variation: ${quantizedColors.size} sampled colors",
        quantizedColors.size >= 6,
    )
    assertTrue(
        "$label screenshot has too little luminance range: ${maxLuma - minLuma}",
        maxLuma - minLuma >= 20,
    )
}
