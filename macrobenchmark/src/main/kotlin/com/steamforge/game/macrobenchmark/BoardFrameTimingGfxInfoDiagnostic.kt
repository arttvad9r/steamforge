package com.steamforge.game.macrobenchmark

import android.content.Intent
import android.os.SystemClock
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingGfxInfoMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val DIAGNOSTIC_TARGET_PACKAGE = "com.steamforge.game"
private const val DIAGNOSTIC_BOARD_ACTION = "com.steamforge.game.BENCHMARK_BOARD"
private const val DIAGNOSTIC_ITERATIONS = 3

/**
 * Hosted-emulator execution diagnostic for the dense merge render workload.
 *
 * SwiftShader runners can omit the RenderThread DrawFrame slices required by FrameTimingMetric.
 * This CI-only diagnostic therefore uses the AndroidX dumpsys-gfxinfo metric. The authoritative
 * physical-device Gate A benchmark remains BoardFrameTimingBenchmark with FrameTimingMetric.
 */
@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class BoardFrameTimingGfxInfoDiagnostic {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun denseMergeBurstGfxInfo() = benchmarkRule.measureRepeated(
        packageName = DIAGNOSTIC_TARGET_PACKAGE,
        metrics = listOf(FrameTimingGfxInfoMetric()),
        compilationMode = CompilationMode.Full(),
        iterations = DIAGNOSTIC_ITERATIONS,
        setupBlock = {
            pressHome()
            startActivityAndWait(
                Intent(DIAGNOSTIC_BOARD_ACTION).setPackage(DIAGNOSTIC_TARGET_PACKAGE),
            )
            device.waitForIdle()
        },
    ) {
        val y = device.displayHeight / 2
        val startX = (device.displayWidth * 0.18f).toInt()
        val endX = (device.displayWidth * 0.82f).toInt()

        device.swipe(startX, y, endX, y, 24)
        SystemClock.sleep(450)
    }
}
