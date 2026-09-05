package com.steamforge.game

import com.steamforge.game.progression.WeeklyRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRunPolicyTest {
    @Test
    fun `normal run keeps existing save progression and tooling behavior`() {
        val policy = GameRunPolicies.resolve(GameRunMode.NORMAL)

        assertTrue(policy.persistActiveRun)
        assertTrue(policy.persistFinishedResult)
        assertTrue(policy.restoreFinishedResult)
        assertTrue(policy.grantProgressionOnFinish)
        assertTrue(policy.allowUndo)
        assertTrue(policy.allowWrench)
        assertTrue(policy.allowOverdrive)
        assertFalse(policy.isDaily)
        assertFalse(policy.isWeekly)
    }

    @Test
    fun `daily run keeps current progression but does not persist active run`() {
        val policy = GameRunPolicies.resolve(GameRunMode.DAILY)

        assertFalse(policy.persistActiveRun)
        assertTrue(policy.persistFinishedResult)
        assertTrue(policy.restoreFinishedResult)
        assertTrue(policy.grantProgressionOnFinish)
        assertTrue(policy.allowUndo)
        assertTrue(policy.allowWrench)
        assertTrue(policy.allowOverdrive)
        assertTrue(policy.isDaily)
        assertFalse(policy.isWeekly)
    }

    @Test
    fun `weekly run is isolated from normal persistence economy and account tools`() {
        val policy = GameRunPolicies.resolve(
            mode = GameRunMode.WEEKLY,
            weeklyRules = WeeklyRules(
                allowUndo = false,
                allowWrench = false,
                allowOverdrive = false,
            ),
        )

        assertFalse(policy.persistActiveRun)
        assertFalse(policy.persistFinishedResult)
        assertFalse(policy.restoreFinishedResult)
        assertFalse(policy.grantProgressionOnFinish)
        assertFalse(policy.allowUndo)
        assertFalse(policy.allowWrench)
        assertFalse(policy.allowOverdrive)
        assertFalse(policy.isDaily)
        assertTrue(policy.isWeekly)
    }
}
