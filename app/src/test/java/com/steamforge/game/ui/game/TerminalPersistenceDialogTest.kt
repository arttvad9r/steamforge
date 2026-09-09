package com.steamforge.game.ui.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalPersistenceDialogTest {

    @Test
    fun `short in-progress write stays visually silent before delay`() {
        assertFalse(
            terminalPersistenceDialogVisible(
                inProgress = true,
                failed = false,
                delayElapsed = false,
            ),
        )
    }

    @Test
    fun `long in-progress write shows saving dialog after delay`() {
        assertTrue(
            terminalPersistenceDialogVisible(
                inProgress = true,
                failed = false,
                delayElapsed = true,
            ),
        )
    }

    @Test
    fun `persistence failure is visible immediately`() {
        assertTrue(
            terminalPersistenceDialogVisible(
                inProgress = false,
                failed = true,
                delayElapsed = false,
            ),
        )
    }

    @Test
    fun `completed write hides dialog regardless of previous delay`() {
        assertFalse(
            terminalPersistenceDialogVisible(
                inProgress = false,
                failed = false,
                delayElapsed = true,
            ),
        )
    }

    @Test
    fun `ordinary completed exit may navigate immediately`() {
        assertTrue(canNavigateAfterGameExit(GameUiState()))
    }

    @Test
    fun `exit cannot navigate while persistence is in progress`() {
        assertFalse(
            canNavigateAfterGameExit(
                GameUiState(finishPersistenceInProgress = true),
            ),
        )
    }

    @Test
    fun `exit cannot navigate while persistence retry is required`() {
        assertFalse(
            canNavigateAfterGameExit(
                GameUiState(finishPersistenceFailed = true),
            ),
        )
    }

    @Test
    fun `exit handoff is consumed by wrapper before direct navigation is allowed`() {
        assertFalse(
            canNavigateAfterGameExit(
                GameUiState(exitAfterPersistenceReady = true),
            ),
        )
    }
}
