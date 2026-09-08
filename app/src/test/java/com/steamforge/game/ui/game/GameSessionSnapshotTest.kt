package com.steamforge.game.ui.game

import com.steamforge.game.core.GameState
import com.steamforge.game.data.SavedGame
import org.junit.Assert.assertEquals
import org.junit.Test

class GameSessionSnapshotTest {
    @Test
    fun `snapshot copies every persisted session field`() {
        val state = GameState(score = 321, moves = 7)
        val ui = GameUiState(
            state = state,
            pressure = 41,
            overdriveRemaining = 3,
            freeUndosLeft = 2,
            mergesTotal = 19,
            maxMergesInOneMove = 4,
            overdrivesSession = 5,
            undosSession = 6,
            highMergesSession = 8,
        )

        val actual = buildSavedGameSnapshot(
            ui = ui,
            sessionSeed = 987654321L,
            rngDraws = 123L,
        )

        assertEquals(
            SavedGame(
                state = state,
                seed = 987654321L,
                pressure = 41,
                overdriveRemaining = 3,
                freeUndosLeft = 2,
                rngDraws = 123L,
                mergesTotal = 19,
                maxMergesInOneMove = 4,
                overdrivesSession = 5,
                undosSession = 6,
                highMergesSession = 8,
            ),
            actual,
        )
    }
}
