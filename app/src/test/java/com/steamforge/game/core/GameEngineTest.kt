package com.steamforge.game.core

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    private val engine = GameEngine()

    // ---------- helpers ----------

    private class ScriptedRng(
        ints: List<Int> = emptyList(),
        doubles: List<Double> = emptyList(),
    ) : Random() {
        private val ints = ArrayDeque(ints)
        private val doubles = ArrayDeque(doubles)
        override fun nextBits(bitCount: Int): Int = if (ints.isEmpty()) 0 else ints.removeFirst()
        override fun nextDouble(): Double = if (doubles.isEmpty()) super.nextDouble() else doubles.removeFirst()
    }

    /** Матрица значений (0 = пусто). excludeId — id заспавненной плитки. */
    private fun boardOf(result: MoveResult): Array<IntArray> {
        val exclude = result.spawned?.id ?: -1L
        val grid = Array(result.state.size) { IntArray(result.state.size) }
        for (t in result.state.tiles) if (t.id != exclude) grid[t.row][t.col] = t.value
        return grid
    }

    private fun board(state: GameState): Array<IntArray> {
        val grid = Array(state.size) { IntArray(state.size) }
        for (t in state.tiles) grid[t.row][t.col] = t.value
        return grid
    }

    private fun state(vararg rows: IntArray): GameState {
        val tiles = mutableListOf<Tile>()
        var id = 1L
        for (r in rows.indices) {
            for (c in rows[r].indices) {
                val v = rows[r][c]
                if (v != 0) tiles += Tile(id++, Integer.numberOfTrailingZeros(v), r, c)
            }
        }
        return GameState(size = 4, tiles = tiles, nextTileId = id)
    }

    private fun assertBoard(expected: Array<IntArray>, actual: Array<IntArray>) {
        assertEquals("board rows", 4, actual.size)
        for (r in expected.indices) {
            val exp = if (expected[r].isEmpty()) IntArray(actual.size) else expected[r]
            assertEquals("row $r", exp.toList(), actual[r].toList())
        }
    }

    // ---------- движение ----------

    @Test
    fun `left merges pairs not triple - 2 2 2 2 becomes 4 4`() {
        val s = state(intArrayOf(2, 2, 2, 2))
        val res = engine.applyMove(s, Move.LEFT)
        assertBoard(arrayOf(intArrayOf(4, 4, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(0, 0, 0, 0)), boardOf(res))
        assertTrue(res.moved)
        assertEquals(8, res.scoreGained)
    }

    @Test
    fun `left 4 4 8 8 becomes 8 16`() {
        val s = state(intArrayOf(4, 4, 8, 8))
        val res = engine.applyMove(s, Move.LEFT)
        assertBoard(arrayOf(intArrayOf(8, 16, 0, 0), intArrayOf(), intArrayOf(), intArrayOf()), boardOf(res))
        assertEquals(8 + 16, res.scoreGained)
    }

    @Test
    fun `no chain merge after merge - 2 2 4 becomes 4 4`() {
        val s = state(intArrayOf(2, 2, 4, 0))
        val res = engine.applyMove(s, Move.LEFT)
        assertBoard(arrayOf(intArrayOf(4, 4, 0, 0), intArrayOf(), intArrayOf(), intArrayOf()), boardOf(res))
        assertEquals(4, res.scoreGained)
    }

    @Test
    fun `merge only nearest pair - 4 2 2 becomes 4 4`() {
        val s = state(intArrayOf(4, 2, 2, 0))
        val res = engine.applyMove(s, Move.LEFT)
        assertBoard(arrayOf(intArrayOf(4, 4, 0, 0), intArrayOf(), intArrayOf(), intArrayOf()), boardOf(res))
        assertEquals(4, res.scoreGained)
    }

    @Test
    fun `right movement`() {
        val s = state(intArrayOf(2, 0, 0, 0), intArrayOf(2, 2, 0, 0))
        val res = engine.applyMove(s, Move.RIGHT)
        val b = boardOf(res)
        assertEquals(2, b[0][3])
        assertEquals(4, b[1][3])
        assertTrue(res.moved)
    }

    @Test
    fun `up movement`() {
        val s = state(intArrayOf(2, 0, 0, 0), intArrayOf(2, 0, 0, 0), intArrayOf(4, 0, 0, 0), intArrayOf(4, 0, 0, 0))
        val res = engine.applyMove(s, Move.UP)
        val b = boardOf(res)
        assertEquals(4, b[0][0])
        assertEquals(8, b[1][0])
        assertTrue(res.moved)
    }

    @Test
    fun `down movement`() {
        val s = state(intArrayOf(2, 0, 0, 0), intArrayOf(2, 0, 0, 0))
        val res = engine.applyMove(s, Move.DOWN)
        val b = boardOf(res)
        assertEquals(4, b[3][0])
        assertEquals(0, b[0][0])
        assertTrue(res.moved)
    }

    @Test
    fun `no move when board is locked`() {
        val s = state(
            intArrayOf(2, 4, 2, 4),
            intArrayOf(4, 2, 4, 2),
            intArrayOf(2, 4, 2, 4),
            intArrayOf(4, 2, 4, 2),
        )
        for (move in Move.entries) {
            val res = engine.applyMove(s, move)
            assertFalse(res.moved)
            assertEquals(0, res.scoreGained)
            assertTrue(res.merges.isEmpty())
            assertNull(res.spawned)
        }
    }

    @Test
    fun `move that only slides without merge counts as moved`() {
        val s = state(intArrayOf(0, 2, 0, 0), intArrayOf(0, 4, 0, 0))
        val res = engine.applyMove(s, Move.LEFT)
        assertTrue(res.moved)
        assertEquals(0, res.scoreGained)
        assertTrue(res.merges.isEmpty())
    }

    // ---------- spawn ----------

    @Test
    fun `spawn happens after successful move`() {
        val s = state(intArrayOf(0, 2, 0, 0))
        val res = engine.applyMove(s, Move.LEFT, Random(1))
        assertEquals(2, res.state.tiles.size)
        assertTrue(res.spawned != null)
        assertTrue(res.spawned!!.level == 1 || res.spawned!!.level == 2)
    }

    @Test
    fun `no spawn after no-op move`() {
        val s = state(intArrayOf(2, 4, 2, 4), intArrayOf(4, 2, 4, 2), intArrayOf(2, 4, 2, 4), intArrayOf(4, 2, 4, 2))
        val res = engine.applyMove(s, Move.LEFT, Random(1))
        assertFalse(res.moved)
        assertEquals(s.tiles, res.state.tiles)
    }

    @Test
    fun `spawn value uses probability from rules`() {
        val s = state(intArrayOf(2, 2, 2, 2), intArrayOf(2, 0, 0, 0), intArrayOf(2, 0, 0, 0), intArrayOf(2, 0, 0, 0))
        val low = engine.applyMove(s, Move.LEFT, ScriptedRng(doubles = listOf(0.5)))
        assertEquals(1, low.spawned!!.level) // 2
        val high = engine.applyMove(s, Move.LEFT, ScriptedRng(doubles = listOf(0.95)))
        assertEquals(2, high.spawned!!.level) // 4
    }

    // ---------- score / win ----------

    @Test
    fun `score accumulates with multiplier`() {
        val s = state(intArrayOf(2, 2, 0, 0), intArrayOf(4, 4, 0, 0))
        val res = engine.applyMove(s, Move.LEFT, scoreMultiplier = 2)
        assertEquals((4 + 8) * 2, res.scoreGained)
        assertEquals((4 + 8) * 2, res.state.score)
    }

    @Test
    fun `reaching win level sets won and game continues`() {
        val s = state(intArrayOf(1024, 1024, 0, 0))
        val res = engine.applyMove(s, Move.LEFT)
        assertTrue(res.state.won)
        assertEquals(GameStatus.PLAYING, res.state.status)
        assertEquals(2048, boardOf(res)[0][0])
    }

    // ---------- game over ----------

    @Test
    fun `locked full board has no moves`() {
        val s = state(
            intArrayOf(2, 4, 2, 4),
            intArrayOf(4, 2, 4, 2),
            intArrayOf(2, 4, 2, 4),
            intArrayOf(4, 2, 4, 2),
        )
        assertFalse(engine.hasAnyMove(s))
    }

    @Test
    fun `full board with equal neighbours still has moves`() {
        val s = state(
            intArrayOf(2, 4, 2, 4),
            intArrayOf(4, 2, 4, 2),
            intArrayOf(2, 4, 2, 4),
            intArrayOf(4, 2, 2, 2),
        )
        assertTrue(engine.hasAnyMove(s))
    }

    @Test
    fun `game over state ignores moves`() {
        val s = state(intArrayOf(2, 4, 2, 4), intArrayOf(4, 2, 4, 2), intArrayOf(2, 4, 2, 4), intArrayOf(4, 2, 4, 2))
            .copy(status = GameStatus.GAME_OVER)
        val res = engine.applyMove(s, Move.LEFT, Random(1))
        assertFalse(res.moved)
        assertEquals(s, res.state)
    }

    @Test
    fun `random play eventually reaches game over and stays there`() {
        val rng = Random(7)
        var state = engine.newGame(rng = rng)
        var moves = 0
        while (state.status == GameStatus.PLAYING && moves < 100_000) {
            val res = engine.applyMove(state, Move.entries[rng.nextInt(4)], rng)
            if (res.moved) state = res.state
            moves++
            if (moves % 2000 == 0) {
                println("moves=$moves tiles=${state.tiles.size} score=${state.score} maxLevel=${state.maxLevel} canMove=${engine.hasAnyMove(state)}")
            }
        }
        assertEquals(GameStatus.GAME_OVER, state.status)
        assertFalse(engine.hasAnyMove(state))
        assertFalse(engine.applyMove(state, Move.LEFT, rng).moved)
    }

    // ---------- undo / immutability ----------

    @Test
    fun `previous state is unchanged after move - undo source stays valid`() {
        val before = engine.newGame(rng = Random(42))
        val snapshot = before.copy()
        engine.applyMove(before, Move.LEFT, Random(43))
        assertEquals(snapshot, before)
        val replayed = engine.applyMove(snapshot, Move.LEFT, Random(43))
        val direct = engine.applyMove(before, Move.LEFT, Random(43))
        assertEquals(replayed.state, direct.state)
    }

    // ---------- seeded RNG ----------

    @Test
    fun `same seed produces identical games`() {
        val a = GameEngine().newGame(rng = Random(123))
        val b = GameEngine().newGame(rng = Random(123))
        assertEquals(a, b)
        var sa = a
        var sb = b
        val moves = listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN, Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)
        for (m in moves) {
            sa = GameEngine().applyMove(sa, m, Random(123)).state
            sb = GameEngine().applyMove(sb, m, Random(123)).state
        }
        assertEquals(sa, sb)
    }

    @Test
    fun `different seeds diverge`() {
        val a = GameEngine().newGame(rng = Random(1))
        val b = GameEngine().newGame(rng = Random(2))
        assertNotEquals(a.tiles, b.tiles)
    }

    @Test
    fun `new game spawns initial tiles`() {
        val s = engine.newGame(rng = Random(42))
        assertEquals(2, s.tiles.size)
        assertEquals(0, s.score)
        assertEquals(GameStatus.PLAYING, s.status)
    }

    @Test
    fun `merge events reference consumed ids and result tile`() {
        val s = state(intArrayOf(2, 2, 0, 0))
        val res = engine.applyMove(s, Move.LEFT)
        assertEquals(1, res.merges.size)
        val merge = res.merges[0]
        assertEquals(setOf(1L, 2L), merge.consumedIds.toSet())
        assertEquals(4, merge.tile.value)
        assertEquals(0, merge.tile.col)
    }
}
