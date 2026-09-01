package com.steamforge.game.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReturnLoopTest {

    private val cycle = 7

    @Test
    fun `claim already made today stays visible and cannot be claimed twice`() {
        val plan = ReturnLoop.dailyRewardPlan(
            lastClaimDay = 100L,
            streakDay = 4,
            graceUsed = false,
            today = 100L,
            cycleDays = cycle,
        )

        assertFalse(plan.canClaim)
        assertEquals(4, plan.visibleStreak)
        assertEquals(4, plan.rewardDay)
        assertEquals(4, ReturnLoop.visibleStreak(100L, 4, false, 100L, cycle))
    }

    @Test
    fun `consecutive day advances streak and restores grace`() {
        val plan = ReturnLoop.dailyRewardPlan(
            lastClaimDay = 100L,
            streakDay = 4,
            graceUsed = true,
            today = 101L,
            cycleDays = cycle,
        )

        assertTrue(plan.canClaim)
        assertEquals(5, plan.rewardDay)
        assertFalse(plan.usesGrace)
        assertFalse(plan.graceUsedAfterClaim)
    }

    @Test
    fun `one missed day is forgiven once`() {
        val plan = ReturnLoop.dailyRewardPlan(
            lastClaimDay = 100L,
            streakDay = 4,
            graceUsed = false,
            today = 102L,
            cycleDays = cycle,
        )

        assertTrue(plan.canClaim)
        assertEquals(4, plan.visibleStreak)
        assertEquals(5, plan.rewardDay)
        assertTrue(plan.usesGrace)
        assertTrue(plan.graceUsedAfterClaim)
        assertEquals(4, ReturnLoop.visibleStreak(100L, 4, false, 102L, cycle))
    }

    @Test
    fun `second missed day while grace is spent resets cycle`() {
        val plan = ReturnLoop.dailyRewardPlan(
            lastClaimDay = 100L,
            streakDay = 5,
            graceUsed = true,
            today = 102L,
            cycleDays = cycle,
        )

        assertTrue(plan.canClaim)
        assertEquals(0, plan.visibleStreak)
        assertEquals(1, plan.rewardDay)
        assertFalse(plan.usesGrace)
        assertFalse(plan.graceUsedAfterClaim)
    }

    @Test
    fun `returning next day after grace restores future protection`() {
        val protectedClaim = ReturnLoop.dailyRewardPlan(100L, 4, false, 102L, cycle)
        assertTrue(protectedClaim.graceUsedAfterClaim)

        val nextDay = ReturnLoop.dailyRewardPlan(
            lastClaimDay = 102L,
            streakDay = protectedClaim.rewardDay,
            graceUsed = protectedClaim.graceUsedAfterClaim,
            today = 103L,
            cycleDays = cycle,
        )

        assertEquals(6, nextDay.rewardDay)
        assertFalse(nextDay.graceUsedAfterClaim)
    }

    @Test
    fun `long absence resets without punishment debt`() {
        val plan = ReturnLoop.dailyRewardPlan(
            lastClaimDay = 100L,
            streakDay = 6,
            graceUsed = false,
            today = 105L,
            cycleDays = cycle,
        )

        assertTrue(plan.canClaim)
        assertEquals(1, plan.rewardDay)
        assertEquals(0, plan.visibleStreak)
        assertFalse(plan.graceUsedAfterClaim)
    }

    @Test
    fun `day seven wraps to day one on continuous cycle`() {
        val plan = ReturnLoop.dailyRewardPlan(
            lastClaimDay = 100L,
            streakDay = 7,
            graceUsed = false,
            today = 101L,
            cycleDays = cycle,
        )

        assertEquals(1, plan.rewardDay)
        assertEquals(7, plan.visibleStreak)
    }

    @Test
    fun `fresh player starts at day one`() {
        val plan = ReturnLoop.dailyRewardPlan(
            lastClaimDay = -1L,
            streakDay = 0,
            graceUsed = false,
            today = 100L,
            cycleDays = cycle,
        )

        assertTrue(plan.canClaim)
        assertEquals(1, plan.rewardDay)
        assertEquals(0, plan.visibleStreak)
    }
}
