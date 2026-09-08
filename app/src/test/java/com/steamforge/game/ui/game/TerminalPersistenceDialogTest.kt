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
}
