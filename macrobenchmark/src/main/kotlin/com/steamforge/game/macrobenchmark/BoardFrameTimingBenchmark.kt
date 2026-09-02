package com.steamforge.game.macrobenchmark

import android.content.Intent
import android.os.SystemClock
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TARGET_PACKAGE = "com.steamforge.game"
private const val BOARD_ACTION = "com.steamforge.game.BENCHMARK_BOARD"
private const val ITERATIONS = 10

@RunWith(AndroidJUnit4::class)
class BoardFrameTimingBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun denseMergeBurst() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Full(),
        startupMode = StartupMode.COLD,
        iterations = ITERATIONS,
        setupBlock = {
            pressHome()
            startActivityAndWait(Intent(BOARD_ACTION).setPackage(TARGET_PACKAGE))
            device.waitForIdle()
        },
    ) {
        val y = device.displayHeight / 2
        val startX = (device.displayWidth * 0.18f).toInt()
        val endX = (device.displayWidth * 0.82f).toInt()

        // One right swipe drives the production BoardView through eight merges,
        // ghost movement, merge pop animations and a new-tile spawn.
        device.swipe(startX, y, endX, y, 24)
        SystemClock.sleep(450)
    }
}
