package com.steamforge.game.core

import java.io.File
import java.util.Locale
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameBalanceSimulationTest {

    @Test
    fun `core balance baseline is deterministic and exported`() {
        val rules = GameRules()
        val runs = simulateRuns(rules)

        assertEquals(SAMPLE_COUNT, runs.size)
        assertTrue(runs.all { it.moves in 1..MAX_ACCEPTED_MOVES })
        assertTrue(runs.all { it.maxLevel >= 1 })
        assertEquals(
            simulateRun(GameEngine(rules), DETERMINISM_SEED),
            simulateRun(GameEngine(rules), DETERMINISM_SEED),
        )

        val report = buildReport(rules = rules, runs = runs)
        val output = File("build/reports/steamforge-balance/core-baseline.json")
        output.parentFile?.mkdirs()
        output.writeText(report)
        println("Steamforge core balance baseline: ${output.absolutePath}")
        println(report)
    }

    @Test
    fun `spawn probability sensitivity is exported without changing production rules`() {
        val baselineProbability = GameRules().spawnLowProbability
        val scenarios = SPAWN_LOW_PROBABILITIES.map { probability ->
            SpawnScenario(
                spawnLowProbability = probability,
                runs = simulateRuns(GameRules(spawnLowProbability = probability)),
            )
        }
        val baseline = scenarios.single { it.spawnLowProbability == baselineProbability }

        assertEquals(SPAWN_LOW_PROBABILITIES.size, scenarios.size)
        assertTrue(scenarios.all { it.runs.size == SAMPLE_COUNT })
        assertTrue(scenarios.all { scenario -> scenario.runs.all { it.moves in 1..MAX_ACCEPTED_MOVES } })

        val report = buildSensitivityReport(
            baselineProbability = baselineProbability,
            baselineRuns = baseline.runs,
            scenarios = scenarios,
        )
        val output = File("build/reports/steamforge-balance/spawn-sensitivity.json")
        output.parentFile?.mkdirs()
        output.writeText(report)
        println("Steamforge spawn sensitivity report: ${output.absolutePath}")
        println(report)
    }

    private fun simulateRuns(rules: GameRules): List<RunMetrics> {
        val engine = GameEngine(rules)
        return (0 until SAMPLE_COUNT).map { seed ->
            simulateRun(engine = engine, seed = seed.toLong())
        }
    }

    private fun simulateRun(engine: GameEngine, seed: Long): RunMetrics {
        val rng = SimulationRandom(seed)
        var state = engine.newGame(rng = rng)
        var acceptedMoves = 0

        while (state.status == GameStatus.PLAYING && acceptedMoves < MAX_ACCEPTED_MOVES) {
            val move = chooseMove(engine, state, acceptedMoves) ?: break
            val result = engine.applyMove(state, move, rng)
            check(result.moved) { "Selected move must be legal: seed=$seed move=$move" }
            state = result.state
            acceptedMoves++
        }

        check(acceptedMoves < MAX_ACCEPTED_MOVES) {
            "Simulation safety cap reached for seed=$seed"
        }
        check(state.status == GameStatus.GAME_OVER || !engine.hasAnyMove(state)) {
            "Simulation stopped before terminal state for seed=$seed"
        }

        return RunMetrics(
            seed = seed,
            moves = state.moves,
            score = state.score,
            maxLevel = state.maxLevel,
            won = state.won,
        )
    }

    private fun chooseMove(engine: GameEngine, state: GameState, acceptedMoves: Int): Move? {
        val order = MOVE_ORDERS[acceptedMoves % MOVE_ORDERS.size]
        return order.firstOrNull { move ->
            // moved is decided before spawn, so a disposable RNG can probe direction legality
            // without advancing the real per-run sequence.
            engine.applyMove(state, move, Random(0)).moved
        }
    }

    private fun buildReport(rules: GameRules, runs: List<RunMetrics>): String {
        val moves = runs.map { it.moves }.sorted()
        val scores = runs.map { it.score }.sorted()
        val maxTileCounts = maxTileCounts(runs)
        val wonCount = runs.count { it.won }

        return buildString {
            appendLine("{")
            appendLine("  \"schemaVersion\": 1,")
            appendLine("  \"policy\": \"corner-v1-core-only\",")
            appendLine("  \"sampleCount\": ${runs.size},")
            appendLine("  \"rules\": {")
            appendLine("    \"spawnLowProbability\": ${format(rules.spawnLowProbability)},")
            appendLine("    \"initialTiles\": ${rules.initialTiles},")
            appendLine("    \"winLevel\": ${rules.winLevel}")
            appendLine("  },")
            appendLine("  \"moves\": ${distributionJson(moves)},")
            appendLine("  \"score\": ${distributionJson(scores)},")
            appendLine("  \"wonCount\": $wonCount,")
            appendLine("  \"winRate\": ${format(wonCount.toDouble() / runs.size)},")
            appendLine("  \"maxTileLevelCounts\": {")
            appendMaxTileCounts(maxTileCounts)
            appendLine("  },")
            appendLine("  \"runs\": [")
            runs.forEachIndexed { index, run ->
                val suffix = if (index == runs.lastIndex) "" else ","
                appendLine(
                    "    {\"seed\":${run.seed},\"moves\":${run.moves},\"score\":${run.score}," +
                        "\"maxLevel\":${run.maxLevel},\"won\":${run.won}}$suffix",
                )
            }
            appendLine("  ]")
            appendLine("}")
        }
    }

    private fun buildSensitivityReport(
        baselineProbability: Double,
        baselineRuns: List<RunMetrics>,
        scenarios: List<SpawnScenario>,
    ): String = buildString {
        appendLine("{")
        appendLine("  \"schemaVersion\": 1,")
        appendLine("  \"policy\": \"corner-v1-core-only\",")
        appendLine("  \"sampleCountPerScenario\": $SAMPLE_COUNT,")
        appendLine("  \"baselineSpawnLowProbability\": ${format(baselineProbability)},")
        appendLine("  \"scenarios\": [")
        scenarios.forEachIndexed { scenarioIndex, scenario ->
            val runs = scenario.runs
            val moves = runs.map { it.moves }.sorted()
            val scores = runs.map { it.score }.sorted()
            val counts = maxTileCounts(runs)
            val suffix = if (scenarioIndex == scenarios.lastIndex) "" else ","
            appendLine("    {")
            appendLine("      \"spawnLowProbability\": ${format(scenario.spawnLowProbability)},")
            appendLine("      \"moves\": ${distributionJson(moves)},")
            appendLine("      \"score\": ${distributionJson(scores)},")
            appendLine("      \"maxTileLevelMean\": ${format(runs.map { it.maxLevel }.average())},")
            appendLine("      \"atLeast512Rate\": ${format(levelRate(runs, 9))},")
            appendLine("      \"atLeast1024Rate\": ${format(levelRate(runs, 10))},")
            appendLine("      \"winRate\": ${format(runs.count { it.won }.toDouble() / runs.size)},")
            appendLine(
                "      \"movesMeanDeltaVsBaseline\": ${format(meanDelta(runs, baselineRuns) { it.moves })},",
            )
            appendLine(
                "      \"scoreMeanDeltaVsBaseline\": ${format(meanDelta(runs, baselineRuns) { it.score })},",
            )
            appendLine(
                "      \"maxTileLevelMeanDeltaVsBaseline\": ${format(meanDelta(runs, baselineRuns) { it.maxLevel })},",
            )
            appendLine("      \"maxTileLevelCounts\": {")
            appendMaxTileCounts(counts, indent = "        ")
            appendLine("      }")
            appendLine("    }$suffix")
        }
        appendLine("  ]")
        appendLine("}")
    }

    private fun StringBuilder.appendMaxTileCounts(
        counts: Map<Int, Int>,
        indent: String = "    ",
    ) {
        counts.entries.forEachIndexed { index, entry ->
            val suffix = if (index == counts.size - 1) "" else ","
            appendLine("$indent\"${entry.key}\": ${entry.value}$suffix")
        }
    }

    private fun maxTileCounts(runs: List<RunMetrics>): Map<Int, Int> =
        runs.groupingBy { it.maxLevel }.eachCount().toSortedMap()

    private fun levelRate(runs: List<RunMetrics>, minimumLevel: Int): Double =
        runs.count { it.maxLevel >= minimumLevel }.toDouble() / runs.size

    private fun meanDelta(
        runs: List<RunMetrics>,
        baselineRuns: List<RunMetrics>,
        value: (RunMetrics) -> Int,
    ): Double {
        require(runs.size == baselineRuns.size)
        return runs.indices.map { index ->
            value(runs[index]) - value(baselineRuns[index])
        }.average()
    }

    private fun distributionJson(values: List<Int>): String =
        "{\"min\":${values.first()},\"p50\":${percentile(values, 0.50)}," +
            "\"p90\":${percentile(values, 0.90)},\"p95\":${percentile(values, 0.95)}," +
            "\"max\":${values.last()},\"mean\":${format(values.average())}}"

    private fun percentile(sorted: List<Int>, p: Double): Int {
        val index = (p.coerceIn(0.0, 1.0) * (sorted.size - 1)).toInt()
        return sorted[index]
    }

    private fun format(value: Double): String = String.format(Locale.US, "%.4f", value)

    private data class RunMetrics(
        val seed: Long,
        val moves: Int,
        val score: Int,
        val maxLevel: Int,
        val won: Boolean,
    )

    private data class SpawnScenario(
        val spawnLowProbability: Double,
        val runs: List<RunMetrics>,
    )

    /** Mirrors the replayable RNG used by GameViewModel so seed samples match production draws. */
    private class SimulationRandom(private val seed: Long) : Random() {
        private var draws = 0L

        override fun nextBits(bitCount: Int): Int {
            require(bitCount in 0..32)
            if (bitCount == 0) return 0
            val index = draws++
            var z = seed + GOLDEN_GAMMA * (index + 1L)
            z = (z xor (z ushr 30)) * MIX_1
            z = (z xor (z ushr 27)) * MIX_2
            z = z xor (z ushr 31)
            return (z ushr (64 - bitCount)).toInt()
        }
    }

    private companion object {
        const val SAMPLE_COUNT = 256
        const val MAX_ACCEPTED_MOVES = 10_000
        const val DETERMINISM_SEED = 42L

        val SPAWN_LOW_PROBABILITIES = listOf(0.80, 0.85, 0.90, 0.95)

        val MOVE_ORDERS = listOf(
            listOf(Move.DOWN, Move.LEFT, Move.RIGHT, Move.UP),
            listOf(Move.LEFT, Move.DOWN, Move.RIGHT, Move.UP),
            listOf(Move.DOWN, Move.RIGHT, Move.LEFT, Move.UP),
            listOf(Move.RIGHT, Move.DOWN, Move.LEFT, Move.UP),
        )

        const val GOLDEN_GAMMA = -7046029254386353131L
        const val MIX_1 = -4658895280553007687L
        const val MIX_2 = -7723592293110705685L
    }
}
