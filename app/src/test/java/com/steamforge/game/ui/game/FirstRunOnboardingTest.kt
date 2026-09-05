package com.steamforge.game.ui.game

import org.junit.Assert.assertEquals
import org.junit.Test

class FirstRunOnboardingTest {

    @Test
    fun `fresh first game asks for first swipe`() {
        assertEquals(
            FirstRunOnboardingPhase.SWIPE,
            firstRunOnboardingPhase(
                isFirstGame = true,
                moves = 0,
                merges = 0,
                finished = false,
                removingMode = false,
            ),
        )
    }

    @Test
    fun `after first accepted move hint advances to first merge`() {
        assertEquals(
            FirstRunOnboardingPhase.MERGE,
            firstRunOnboardingPhase(
                isFirstGame = true,
                moves = 1,
                merges = 0,
                finished = false,
                removingMode = false,
            ),
        )
    }

    @Test
    fun `first merge completes lightweight gameplay onboarding`() {
        assertEquals(
            FirstRunOnboardingPhase.NONE,
            firstRunOnboardingPhase(
                isFirstGame = true,
                moves = 2,
                merges = 1,
                finished = false,
                removingMode = false,
            ),
        )
    }

    @Test
    fun `established player never receives first run hints`() {
        assertEquals(
            FirstRunOnboardingPhase.NONE,
            firstRunOnboardingPhase(
                isFirstGame = false,
                moves = 0,
                merges = 0,
                finished = false,
                removingMode = false,
            ),
        )
    }

    @Test
    fun `result and wrench mode suppress onboarding hint`() {
        assertEquals(
            FirstRunOnboardingPhase.NONE,
            firstRunOnboardingPhase(
                isFirstGame = true,
                moves = 1,
                merges = 0,
                finished = true,
                removingMode = false,
            ),
        )
        assertEquals(
            FirstRunOnboardingPhase.NONE,
            firstRunOnboardingPhase(
                isFirstGame = true,
                moves = 1,
                merges = 0,
                finished = false,
                removingMode = true,
            ),
        )
    }
}
