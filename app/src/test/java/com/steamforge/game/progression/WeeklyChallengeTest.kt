package com.steamforge.game.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyChallengeTest {
    @Test
    fun `all days in one Monday aligned week resolve to the same challenge`() {
        val monday = 4L // 1970-01-05
        val expected = WeeklyChallenges.forEpochDay(monday)

        for (day in monday until monday + WeeklyChallenges.DAYS_PER_WEEK) {
            assertEquals(expected, WeeklyChallenges.forEpochDay(day))
        }

        assertEquals(monday, expected.startEpochDay)
        assertEquals(monday + 7L, expected.endEpochDayExclusive)
        assertEquals("weekly-$monday", expected.challengeId)
        assertEquals(WeeklyRuleType.STANDARD_SCORE_ATTACK, expected.rules.type)
        assertFalse(expected.rules.allowUndo)
        assertFalse(expected.rules.allowWrench)
    }

    @Test
    fun `crossing Monday creates a new id and deterministic seed`() {
        val previous = WeeklyChallenges.forEpochDay(10L) // Sunday
        val next = WeeklyChallenges.forEpochDay(11L) // Monday

        assertNotEquals(previous.challengeId, next.challengeId)
        assertNotEquals(previous.seed, next.seed)
        assertEquals(next, WeeklyChallenges.forEpochDay(17L))
    }

    @Test
    fun `weekly rollover happens at the same Monday UTC instant for every client`() {
        val msPerDay = 86_400_000L
        val mondayUtcMs = 4L * msPerDay // 1970-01-05T00:00:00Z

        val justBefore = WeeklyChallenges.forUtcMillis(mondayUtcMs - 1L)
        val atBoundary = WeeklyChallenges.forUtcMillis(mondayUtcMs)

        assertEquals(3L, WeeklyChallenges.utcEpochDay(mondayUtcMs - 1L))
        assertEquals(4L, WeeklyChallenges.utcEpochDay(mondayUtcMs))
        assertNotEquals(justBefore.challengeId, atBoundary.challengeId)
        assertNotEquals(justBefore.seed, atBoundary.seed)
        assertEquals("weekly-4", atBoundary.challengeId)
    }

    @Test
    fun `epoch boundary still aligns to the Monday that started the week`() {
        val challenge = WeeklyChallenges.forEpochDay(0L) // Thursday 1970-01-01

        assertEquals(-3L, challenge.startEpochDay)
        assertEquals(4L, challenge.endEpochDayExclusive)
        assertTrue(0L in challenge.startEpochDay until challenge.endEpochDayExclusive)
    }
}
