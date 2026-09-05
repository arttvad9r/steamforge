package com.steamforge.game.ui.game

import org.junit.Assert.assertEquals
import org.junit.Test

class FirstRunOnboardingTest {

    @Test
    fun `fresh normal run asks for first swipe`() {
        assertEquals(
            FirstRunOnboardingPhase.SWIPE,
            firstRunOnboardingPhase(
                isDaily = false,
                bestScore = 0,
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
                isDaily = false,
                bestScore = 0,
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
                isDaily = false,
                bestScore = 0,
                moves = 2,
                merges = 1,
                finished = false,
                removingMode = false,
            ),
        )
    }

    @Test
    fun `daily and established players never receive first run hints`() {
        assertEquals(
            FirstRunOnboardingPhase.NONE,
            firstRunOnboardingPhase(
                isDaily = true,
                bestScore = 0,
                moves = 0,
                merges = 0,
                finished = false,
                removingMode = false,
            ),
        )
        assertEquals(
            FirstRunOnboardingPhase.NONE,
            firstRunOnboardingPhase(
                isDaily = false,
                bestScore = 128,
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
                isDaily = false,
                bestScore = 0,
                moves = 1,
                merges = 0,
                finished = true,
                removingMode = false,
            ),
        )
        assertEquals(
            FirstRunOnboardingPhase.NONE,
            firstRunOnboardingPhase(
                isDaily = false,
                bestScore = 0,
                moves = 1,
                merges = 0,
                finished = false,
                removingMode = true,
            ),
        )
    }
}
