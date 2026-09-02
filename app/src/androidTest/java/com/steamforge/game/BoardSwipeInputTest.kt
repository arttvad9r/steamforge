package com.steamforge.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.steamforge.game.core.GameState
import com.steamforge.game.core.GameStatus
import com.steamforge.game.core.Move
import com.steamforge.game.theme.SteamforgeTheme
import com.steamforge.game.ui.game.BoardView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoardSwipeInputTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun boardUsesTouchSlopAndDispatchesOnlyOneMovePerGesture() {
        val moves = mutableListOf<Move>()
        var touchSlopPx = 0f

        composeRule.setContent {
            touchSlopPx = LocalViewConfiguration.current.touchSlop
            SteamforgeTheme {
                Box {
                    BoardView(
                        state = emptyState,
                        lastResult = null,
                        previousTiles = emptyList(),
                        animationsActive = false,
                        removingMode = false,
                        canRemove = { false },
                        onTileClick = { },
                        onSwipe = { moves += it },
                        modifier = Modifier.size(320.dp).testTag(BOARD_TAG),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        assertTrue("Expected a positive platform touch slop", touchSlopPx > 0f)

        val board = composeRule.onNodeWithTag(BOARD_TAG)

        board.performTouchInput {
            val start = center
            down(start)
            moveTo(Offset(start.x + touchSlopPx * 0.5f, start.y))
            up()
        }
        composeRule.runOnIdle {
            assertTrue("Sub-touch-slop drag must not dispatch a move", moves.isEmpty())
        }

        board.performTouchInput {
            val start = Offset(width * 0.25f, height * 0.5f)
            down(start)
            moveTo(Offset(start.x + touchSlopPx * 1.25f, start.y))
            moveTo(Offset(start.x + touchSlopPx * 2f, start.y))
            moveTo(Offset(start.x + touchSlopPx * 3f, start.y))
            up()
        }
        composeRule.runOnIdle {
            assertEquals("One swipe must dispatch exactly one command", listOf(Move.RIGHT), moves)
        }
    }

    private companion object {
        const val BOARD_TAG = "game-board"

        val emptyState = GameState(
            size = 4,
            tiles = emptyList(),
            score = 0,
            nextTileId = 1L,
            status = GameStatus.PLAYING,
            won = false,
            moves = 0,
        )
    }
}
