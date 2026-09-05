package com.steamforge.game.progression

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyRewardStreakTest {
    private val today = 1_000L

    @Test
    fun `same day yesterday and one missed day keep streak`() {
        assertEquals(6, continuingDailyRewardStreak(today, 6, today))
        assertEquals(6, continuingDailyRewardStreak(today - 1, 6, today))
        assertEquals(6, continuingDailyRewardStreak(today - 2, 6, today))
    }

    @Test
    fun `two missed days reset streak`() {
        assertEquals(0, continuingDailyRewardStreak(today - 3, 6, today))
        assertEquals(0, continuingDailyRewardStreak(today - 20, 6, today))
    }

    @Test
    fun `invalid stored streak never becomes positive`() {
        assertEquals(0, continuingDailyRewardStreak(today - 1, 0, today))
        assertEquals(0, continuingDailyRewardStreak(today - 1, -4, today))
    }
}
