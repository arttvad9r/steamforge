package com.steamforge.game.data

import com.steamforge.game.core.GameState
import com.steamforge.game.core.GameStatus
import com.steamforge.game.core.Tile

/** Активная партия: доска + мета-состояние, переживает process death. */
data class SavedGame(
    val state: GameState,
    val seed: Long?,
    val pressure: Int,
    val overdriveRemaining: Int,
    val freeUndosLeft: Int,
    /** Количество nextBits-вызовов детерминированного RNG, уже потреблённых в этой партии. */
    val rngDraws: Long = 0L,
    /** Сессионные счётчики нужны для точной статистики после process death. */
    val mergesTotal: Int = 0,
    val maxMergesInOneMove: Int = 0,
    val overdrivesSession: Int = 0,
    val undosSession: Int = 0,
    val highMergesSession: Int = 0,
)

/**
 * Компактное кодирование партии для DataStore.
 * v1: v1|size|score|nextTileId|won|moves|tiles
 * v2: v2|size|score|nextTileId|won|moves|seed|pressure|overdrive|freeUndos|tiles
 * v3: v3|size|score|nextTileId|won|moves|seed|pressure|overdrive|freeUndos|rngDraws|tiles
 * v4: v4|size|score|nextTileId|won|moves|seed|pressure|overdrive|freeUndos|rngDraws|
 *     mergesTotal|maxMergesInOneMove|overdrivesSession|undosSession|highMergesSession|tiles
 */
object GameSaveCodec {

    private const val VERSION = "v4"
    private const val MAX_COUNTER = 1_000_000

    fun encode(game: SavedGame): String = buildString {
        append(VERSION).append('|')
        append(game.state.size).append('|')
        append(game.state.score).append('|')
        append(game.state.nextTileId).append('|')
        append(if (game.state.won) 1 else 0).append('|')
        append(game.state.moves).append('|')
        append(game.seed ?: -1L).append('|')
        append(game.pressure).append('|')
        append(game.overdriveRemaining).append('|')
        append(game.freeUndosLeft).append('|')
        append(game.rngDraws.coerceAtLeast(0L)).append('|')
        append(game.mergesTotal.coerceAtLeast(0)).append('|')
        append(game.maxMergesInOneMove.coerceAtLeast(0)).append('|')
        append(game.overdrivesSession.coerceAtLeast(0)).append('|')
        append(game.undosSession.coerceAtLeast(0)).append('|')
        append(game.highMergesSession.coerceAtLeast(0)).append('|')
        game.state.tiles.joinTo(this, ";") { "${it.id},${it.level},${it.row},${it.col}" }
    }

    fun decode(raw: String): SavedGame? {
        val parts = raw.split('|')
        val isV4 = parts.size == 17 && parts[0] == "v4"
        val isV3 = parts.size == 12 && parts[0] == "v3"
        val isV2 = parts.size == 11 && parts[0] == "v2"
        val isV1 = parts.size == 7 && parts[0] == "v1"
        if (!isV4 && !isV3 && !isV2 && !isV1) return null
        return runCatching {
            val size = parts[1].toInt().also { require(it in 2..8) }
            val score = parts[2].toInt().also { require(it >= 0) }
            val nextId = parts[3].toLong().also { require(it > 0L) }
            require(parts[4] == "0" || parts[4] == "1")
            val won = parts[4] == "1"
            val moves = parts[5].toInt().also { require(it >= 0) }
            var seed: Long? = null
            var pressure = 0
            var overdrive = 0
            var freeUndos = 0
            var rngDraws = 0L
            var mergesTotal = 0
            var maxMergesInOneMove = 0
            var overdrivesSession = 0
            var undosSession = 0
            var highMergesSession = 0
            val tilesIndex: Int
            when {
                isV4 -> {
                    seed = parts[6].toLong().takeIf { it >= 0 }
                    pressure = parts[7].toInt().coerceIn(0, 1000)
                    overdrive = parts[8].toInt().coerceIn(0, 1000)
                    freeUndos = parts[9].toInt().coerceIn(0, 1000)
                    rngDraws = parts[10].toLong().coerceIn(0L, 1_000_000L)
                    mergesTotal = parts[11].toInt().coerceIn(0, MAX_COUNTER)
                    maxMergesInOneMove = parts[12].toInt().coerceIn(0, MAX_COUNTER)
                    overdrivesSession = parts[13].toInt().coerceIn(0, MAX_COUNTER)
                    undosSession = parts[14].toInt().coerceIn(0, MAX_COUNTER)
                    highMergesSession = parts[15].toInt().coerceIn(0, MAX_COUNTER)
                    tilesIndex = 16
                }
                isV3 -> {
                    seed = parts[6].toLong().takeIf { it >= 0 }
                    pressure = parts[7].toInt().coerceIn(0, 1000)
                    overdrive = parts[8].toInt().coerceIn(0, 1000)
                    freeUndos = parts[9].toInt().coerceIn(0, 1000)
                    rngDraws = parts[10].toLong().coerceIn(0L, 1_000_000L)
                    tilesIndex = 11
                }
                isV2 -> {
                    seed = parts[6].toLong().takeIf { it >= 0 }
                    pressure = parts[7].toInt().coerceIn(0, 1000)
                    overdrive = parts[8].toInt().coerceIn(0, 1000)
                    freeUndos = parts[9].toInt().coerceIn(0, 1000)
                    tilesIndex = 10
                }
                else -> tilesIndex = 6
            }
            val tiles = if (parts[tilesIndex].isEmpty()) {
                emptyList()
            } else {
                parts[tilesIndex].split(';').map { t ->
                    val f = t.split(',')
                    require(f.size == 4)
                    val id = f[0].toLong().also { require(it > 0L) }
                    val level = f[1].toInt().also { require(it in 1..30) }
                    val row = f[2].toInt().also { require(it in 0 until size) }
                    val col = f[3].toInt().also { require(it in 0 until size) }
                    Tile(id = id, level = level, row = row, col = col)
                }
            }
            require(tiles.size <= size * size)
            require(tiles.map { it.id }.toSet().size == tiles.size)
            require(tiles.map { it.row to it.col }.toSet().size == tiles.size)
            require(nextId > (tiles.maxOfOrNull { it.id } ?: 0L))

            SavedGame(
                state = GameState(
                    size = size,
                    tiles = tiles,
                    score = score,
                    nextTileId = nextId,
                    status = GameStatus.PLAYING,
                    won = won,
                    moves = moves,
                ),
                seed = seed,
                pressure = pressure,
                overdriveRemaining = overdrive,
                freeUndosLeft = freeUndos,
                rngDraws = rngDraws,
                mergesTotal = mergesTotal,
                maxMergesInOneMove = maxMergesInOneMove,
                overdrivesSession = overdrivesSession,
                undosSession = undosSession,
                highMergesSession = highMergesSession,
            )
        }.getOrNull()
    }
}
