package com.steamforge.game.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTest {

    @Test
    fun `fresh install starts on real core board`() {
        assertEquals(Onboarding.CORE, Onboarding.resolveInitialStep(null, hasLegacyProgress = false))
    }

    @Test
    fun `legacy install skips newly introduced onboarding`() {
        assertEquals(Onboarding.COMPLETE, Onboarding.resolveInitialStep(null, hasLegacyProgress = true))
    }

    @Test
    fun `persisted onboarding stage wins over legacy evidence`() {
        assertEquals(Onboarding.WORKSHOP, Onboarding.resolveInitialStep(Onboarding.WORKSHOP, hasLegacyProgress = true))
    }

    @Test
    fun `stored stage is clamped`() {
        assertEquals(Onboarding.CORE, Onboarding.resolveInitialStep(-99, hasLegacyProgress = false))
        assertEquals(Onboarding.COMPLETE, Onboarding.resolveInitialStep(99, hasLegacyProgress = false))
    }

    @Test
    fun `empty player is not treated as legacy`() {
        assertFalse(Onboarding.hasLegacyProgress(PlayerProgress(), hasSavedGame = false))
    }

    @Test
    fun `saved run or previous progression counts as legacy`() {
        assertTrue(Onboarding.hasLegacyProgress(PlayerProgress(), hasSavedGame = true))
        assertTrue(Onboarding.hasLegacyProgress(PlayerProgress(bestScore = 128), hasSavedGame = false))
        assertTrue(Onboarding.hasLegacyProgress(PlayerProgress(stats = PlayerStats(gamesPlayed = 1)), hasSavedGame = false))
        assertTrue(Onboarding.hasLegacyProgress(PlayerProgress(dailyRewardDay = 10), hasSavedGame = false))
    }
}
