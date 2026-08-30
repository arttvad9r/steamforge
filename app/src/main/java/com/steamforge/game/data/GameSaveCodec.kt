package com.steamforge.game.data

import com.steamforge.game.core.GameState
import com.steamforge.game.core.GameStatus
import com.steamforge.game.core.Tile

/** Активная партия: доска + мета-состояние (pressure/overdrive/undo/seed), переживает process death. */
data class SavedGame(
    val state: GameState,
    val seed: Long?,
    val pressure: Int,
    val overdriveRemaining: Int,
    val freeUndosLeft: Int,
)

/**
 * Компактное кодирование партии для DataStore. Без внешних зависимостей.
 * Формат v1: v1|size|score|nextTileId|won|moves|id,level,row,col;...
 * Формат v2: v2|size|score|nextTileId|won|moves|seed|pressure|overdrive|freeUndos|tiles
 * Старые v1-сейвы декодируются с дефолтными мета-полями (без crash).
 */
object GameSaveCodec {

    private const val VERSION = "v2"

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
        game.state.tiles.joinTo(this, ";") { "${it.id},${it.level},${it.row},${it.col}" }
    }

    fun decode(raw: String): SavedGame? {
        val parts = raw.split('|')
        val isV2 = parts.size == 11 && parts[0] == VERSION
        val isV1 = parts.size == 7 && parts[0] == "v1"
        if (!isV2 && !isV1) return null
        return runCatching {
            val size = parts[1].toInt()
            val score = parts[2].toInt()
            val nextId = parts[3].toLong()
            val won = parts[4] == "1"
            val moves = parts[5].toInt()
            var seed: Long? = null
            var pressure = 0
            var overdrive = 0
            var freeUndos = 0
            val tilesIndex: Int
            if (isV2) {
                seed = parts[6].toLong().takeIf { it >= 0 }
                pressure = parts[7].toInt().coerceIn(0, 1000)
                overdrive = parts[8].toInt().coerceIn(0, 1000)
                freeUndos = parts[9].toInt().coerceIn(0, 1000)
                tilesIndex = 10
            } else {
                tilesIndex = 6
            }
            val tiles = if (parts[tilesIndex].isEmpty()) {
                emptyList()
            } else {
                parts[tilesIndex].split(';').map { t ->
                    val f = t.split(',')
                    Tile(f[0].toLong(), f[1].toInt(), f[2].toInt(), f[3].toInt())
                }
            }
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
            )
        }.getOrNull()
    }
}
