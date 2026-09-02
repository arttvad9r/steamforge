package com.steamforge.game.ui.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeFeedbackTest {
    @Test
    fun `merge tiers follow feedback hierarchy`() {
        assertEquals(MergeFeedbackTier.LOW, mergeFeedbackProfile(maxLevel = 4, mergeCount = 1).tier)
        assertEquals(MergeFeedbackTier.MID, mergeFeedbackProfile(maxLevel = 5, mergeCount = 1).tier)
        assertEquals(MergeFeedbackTier.HIGH, mergeFeedbackProfile(maxLevel = 8, mergeCount = 1).tier)
    }

    @Test
    fun `combo pitch rises gradually and caps`() {
        assertEquals(1.00f, mergeFeedbackProfile(maxLevel = 5, mergeCount = 1).playbackRate, 0.0001f)
        assertEquals(1.04f, mergeFeedbackProfile(maxLevel = 5, mergeCount = 2).playbackRate, 0.0001f)
        assertEquals(1.08f, mergeFeedbackProfile(maxLevel = 5, mergeCount = 3).playbackRate, 0.0001f)
        assertEquals(1.12f, mergeFeedbackProfile(maxLevel = 5, mergeCount = 4).playbackRate, 0.0001f)
        assertEquals(1.12f, mergeFeedbackProfile(maxLevel = 5, mergeCount = 8).playbackRate, 0.0001f)
    }

    @Test
    fun `merge pop grows by tier and combo without covering board`() {
        val low = mergePopScale(level = 4, mergeCount = 1)
        val mid = mergePopScale(level = 5, mergeCount = 1)
        val high = mergePopScale(level = 8, mergeCount = 1)
        val core = mergePopScale(level = 11, mergeCount = 1)
        val comboCore = mergePopScale(level = 11, mergeCount = 8)

        assertTrue(low < mid)
        assertTrue(mid < high)
        assertTrue(high < core)
        assertTrue(core < comboCore)
        assertEquals(1.20f, comboCore, 0.0001f)
    }
}
