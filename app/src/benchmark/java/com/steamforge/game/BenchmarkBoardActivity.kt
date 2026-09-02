package com.steamforge.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.steamforge.game.core.GameEngine
import com.steamforge.game.core.GameState
import com.steamforge.game.core.MoveResult
import com.steamforge.game.core.Tile
import com.steamforge.game.theme.SteamforgeTheme
import com.steamforge.game.ui.game.BoardView
import kotlin.random.Random

/**
 * Benchmark-only host for the production BoardView animation path.
 *
 * The fixture intentionally creates the densest legal merge burst for a 4x4 board:
 * every row contains two mergeable pairs, so one horizontal swipe produces eight
 * merge events plus a spawn. This Activity exists only in the benchmark build type.
 */
class BenchmarkBoardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SteamforgeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DenseMergeBoard()
                }
            }
        }
    }
}

@Composable
private fun DenseMergeBoard() {
    val engine = remember { GameEngine() }
    val rng = remember { Random(0x5EED) }
    var state by remember { mutableStateOf(denseMergeState()) }
    var previousTiles by remember { mutableStateOf(emptyList<Tile>()) }
    var lastResult by remember { mutableStateOf<MoveResult?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        BoardView(
            state = state,
            lastResult = lastResult,
            previousTiles = previousTiles,
            animationsActive = true,
            removingMode = false,
            canRemove = { false },
            onTileClick = {},
            onSwipe = { move ->
                val before = state
                val result = engine.applyMove(before, move, rng)
                if (result.moved) {
                    previousTiles = before.tiles
                    lastResult = result
                    state = result.state
                }
            },
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
    }
}

private fun denseMergeState(): GameState {
    val tiles = ArrayList<Tile>(16)
    var id = 1L
    repeat(4) { row ->
        listOf(1, 1, 2, 2).forEachIndexed { col, level ->
            tiles += Tile(id = id++, level = level, row = row, col = col)
        }
    }
    return GameState(
        tiles = tiles,
        nextTileId = id,
    )
}
