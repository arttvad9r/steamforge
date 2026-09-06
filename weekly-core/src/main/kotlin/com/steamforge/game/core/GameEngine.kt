package com.steamforge.game.core

import kotlin.random.Random

/** Уровень плитки: level 1 = значение 2, level 11 = значение 2048 (Механическое ядро). */
data class Tile(
    val id: Long,
    val level: Int,
    val row: Int,
    val col: Int,
) {
    val value: Int
        get() = 1 shl level
}

enum class Move { LEFT, RIGHT, UP, DOWN }

enum class GameStatus { PLAYING, GAME_OVER }

data class GameState(
    val size: Int = 4,
    val tiles: List<Tile> = emptyList(),
    val score: Int = 0,
    val nextTileId: Long = 1L,
    val status: GameStatus = GameStatus.PLAYING,
    val won: Boolean = false,
    val moves: Int = 0,
) {
    val maxLevel: Int
        get() = tiles.maxOfOrNull { it.level } ?: 0

    fun tileAt(row: Int, col: Int): Tile? =
        tiles.firstOrNull { it.row == row && it.col == col }
}

/** Событие объединения двух плиток в одну. Нужно UI (анимации) и мета-системам (pressure). */
data class MergeEvent(
    val consumedIds: List<Long>,
    val tile: Tile,
)

data class MoveResult(
    val state: GameState,
    val moved: Boolean = false,
    val scoreGained: Int = 0,
    val merges: List<MergeEvent> = emptyList(),
    val spawned: Tile? = null,
)

/** Конфигурация правил. Magic numbers собраны здесь, а не размазаны по коду. */
data class GameRules(
    /** Вероятность спавна "2" (level 1); иначе "4" (level 2). */
    val spawnLowProbability: Double = 0.9,
    val initialTiles: Int = 2,
    /** Уровень, считающийся победным (11 = 2048 = Механическое ядро). */
    val winLevel: Int = 11,
)

/**
 * Чистая механика 2048. Без зависимостей от Android/Compose.
 * GameState + Move -> новый GameState. Determinism: одинаковый Random(seed) — одинаковый результат.
 */
class GameEngine(private val rules: GameRules = GameRules()) {

    fun newGame(size: Int = 4, rng: Random = Random.Default): GameState {
        var state = GameState(size = size, nextTileId = 1L)
        repeat(rules.initialTiles) { state = spawn(state, rng) }
        return state
    }

    fun applyMove(
        state: GameState,
        move: Move,
        rng: Random = Random.Default,
        scoreMultiplier: Int = 1,
    ): MoveResult {
        if (state.status == GameStatus.GAME_OVER) return MoveResult(state)

        val merges = mutableListOf<MergeEvent>()
        var gained = 0
        var moved = false
        var nextId = state.nextTileId
        val resultTiles = mutableListOf<Tile>()

        for (lineIndex in 0 until state.size) {
            val line = lineTiles(state.tiles, state.size, move, lineIndex)
            val processed = processLine(line, nextId, scoreMultiplier)
            nextId = processed.nextId
            merges += processed.merges
            gained += processed.gained
            val placed = ArrayList<Tile>(processed.tiles.size)
            for ((step, tile) in processed.tiles.withIndex()) {
                val (r, c) = cellAt(move, state.size, lineIndex, step)
                placed += tile.copy(row = r, col = c)
            }
            resultTiles += placed
            // Merge всегда сдвигает плитки; иначе ходом считается изменение позиции любой плитки линии.
            if (!moved) {
                moved = processed.merges.isNotEmpty() ||
                    placed.zip(line).any { (a, b) -> a.id == b.id && (a.row != b.row || a.col != b.col) }
            }
        }

        if (!moved) return MoveResult(state)

        var newState = state.copy(
            tiles = resultTiles,
            score = state.score + gained,
            nextTileId = nextId,
            moves = state.moves + 1,
            won = state.won || resultTiles.any { it.level >= rules.winLevel },
        )
        val spawned = spawnTile(newState, nextId, rng)
        if (spawned != null) {
            newState = newState.copy(tiles = newState.tiles + spawned, nextTileId = nextId + 1)
        }
        if (!hasAnyMove(newState)) newState = newState.copy(status = GameStatus.GAME_OVER)

        return MoveResult(
            state = newState,
            moved = true,
            scoreGained = gained,
            merges = merges,
            spawned = spawned,
        )
    }

    /** Есть ли у состояния хотя бы один возможный ход. */
    fun hasAnyMove(state: GameState): Boolean {
        if (state.tiles.size < state.size * state.size) return true
        val grid = HashMap<Int, Int>(state.tiles.size)
        for (t in state.tiles) grid[t.row * state.size + t.col] = t.level
        for (r in 0 until state.size) {
            for (c in 0 until state.size) {
                val level = grid.getValue(r * state.size + c)
                if (c + 1 < state.size && grid.getValue(r * state.size + c + 1) == level) return true
                if (r + 1 < state.size && grid.getValue((r + 1) * state.size + c) == level) return true
            }
        }
        return false
    }

    private fun spawn(state: GameState, rng: Random): GameState {
        val tile = spawnTile(state, state.nextTileId, rng) ?: return state
        return state.copy(tiles = state.tiles + tile, nextTileId = tile.id + 1)
    }

    private fun spawnTile(state: GameState, id: Long, rng: Random): Tile? {
        val size = state.size
        val occupied = HashSet<Int>(state.tiles.size * 2)
        for (t in state.tiles) occupied += t.row * size + t.col
        val empty = (0 until size * size).filter { it !in occupied }
        if (empty.isEmpty()) return null
        val cell = empty[rng.nextInt(empty.size)]
        val level = if (rng.nextDouble() < rules.spawnLowProbability) 1 else 2
        return Tile(id, level, cell / size, cell % size)
    }

    /** Плитки одной линии в порядке движения (ближайшая к краю — первая). */
    private fun lineTiles(tiles: List<Tile>, size: Int, move: Move, lineIndex: Int): List<Tile> =
        when (move) {
            Move.LEFT -> tiles.filter { it.row == lineIndex }.sortedBy { it.col }
            Move.RIGHT -> tiles.filter { it.row == lineIndex }.sortedByDescending { it.col }
            Move.UP -> tiles.filter { it.col == lineIndex }.sortedBy { it.row }
            Move.DOWN -> tiles.filter { it.col == lineIndex }.sortedByDescending { it.row }
        }

    /** Куда встаёт step-я плитка линии. */
    private fun cellAt(move: Move, size: Int, lineIndex: Int, step: Int): Pair<Int, Int> =
        when (move) {
            Move.LEFT -> lineIndex to step
            Move.RIGHT -> lineIndex to (size - 1 - step)
            Move.UP -> step to lineIndex
            Move.DOWN -> (size - 1 - step) to lineIndex
        }

    private data class LineResult(
        val tiles: List<Tile>,
        val merges: List<MergeEvent>,
        val gained: Int,
        val nextId: Long,
    )

    /**
     * Уплотнение + объединение одной линии. Плитка участвует в объединении не более одного раза за ход:
     * обе исходные плитки потребляются, результат кладётся в выход и больше не сравнивается.
     * Возвращает nextId — первый СВОБОДНЫЙ id (startId, если объединений не было).
     */
    private fun processLine(line: List<Tile>, startId: Long, scoreMultiplier: Int): LineResult {
        if (line.isEmpty()) return LineResult(emptyList(), emptyList(), 0, startId)
        val out = ArrayList<Tile>(line.size)
        val merges = ArrayList<MergeEvent>()
        var nextId = startId
        var gained = 0
        var i = 0
        while (i < line.size) {
            val current = line[i]
            val next = line.getOrNull(i + 1)
            if (next != null && next.level == current.level) {
                val merged = Tile(nextId++, current.level + 1, current.row, current.col)
                out += merged
                merges += MergeEvent(listOf(current.id, next.id), merged)
                gained += merged.value * scoreMultiplier
                i += 2
            } else {
                out += current
                i++
            }
        }
        return LineResult(out, merges, gained, nextId)
    }
}
