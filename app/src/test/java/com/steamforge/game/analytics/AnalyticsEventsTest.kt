package com.steamforge.game.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AnalyticsEventsTest {
    @Test
    fun `run lifecycle events use one stable product schema`() {
        val normalStart = AnalyticsEvents.gameStarted(runId = "run-normal", daily = false)
        val dailyStart = AnalyticsEvents.gameStarted(
            runId = "run-daily",
            daily = true,
            dailyType = "REACH_SCORE",
        )
        val finished = AnalyticsEvents.gameFinished(
            runId = "run-daily",
            score = -10,
            maxTile = 2048,
            moves = -2,
            daily = true,
        )

        assertEquals("game_started", normalStart.name)
        assertEquals("run-normal", normalStart.params["run_id"])
        assertEquals(false, normalStart.params["daily"])
        assertFalse(normalStart.params.containsKey("daily_type"))

        assertEquals("game_started", dailyStart.name)
        assertEquals("run-daily", dailyStart.params["run_id"])
        assertEquals(true, dailyStart.params["daily"])
        assertEquals("REACH_SCORE", dailyStart.params["daily_type"])

        assertEquals("game_finished", finished.name)
        assertEquals("run-daily", finished.params["run_id"])
        assertEquals(0, finished.params["score"])
        assertEquals(2048, finished.params["max_tile"])
        assertEquals(0, finished.params["moves"])
        assertEquals(true, finished.params["daily"])
    }

    @Test
    fun `contract event uses stable product schema`() {
        val event = AnalyticsEvents.contractCompleted(
            contractId = "daily-1",
            type = "MERGE_COUNT",
            target = 25,
            rewardType = "workshop_parts",
            rewardAmount = 30,
        )

        assertEquals("contract_completed", event.name)
        assertEquals("daily-1", event.params["contract_id"])
        assertEquals("MERGE_COUNT", event.params["type"])
        assertEquals(25, event.params["target"])
        assertEquals("workshop_parts", event.params["reward_type"])
        assertEquals(30, event.params["reward_amount"])
    }

    @Test
    fun `numeric product fields are normalized non-negative`() {
        val event = AnalyticsEvents.workshopUpgrade(
            mechanism = "CORE",
            fromStage = -3,
            toStage = 2,
            partsSpent = -10,
        )

        assertEquals(0, event.params["from_stage"])
        assertEquals(2, event.params["to_stage"])
        assertEquals(0, event.params["parts_spent"])
    }

    @Test
    fun `economy events share stable normalized schema`() {
        val earned = AnalyticsEvents.resourceEarned(
            resourceType = "workshop_parts",
            source = "daily_contract",
            amount = -5,
            balanceAfter = -1,
        )
        val spent = AnalyticsEvents.resourceSpent(
            resourceType = "workshop_parts",
            source = "workshop_upgrade",
            amount = 20,
            balanceAfter = 4,
        )

        assertEquals("resource_earned", earned.name)
        assertEquals("workshop_parts", earned.params["resource_type"])
        assertEquals("daily_contract", earned.params["source"])
        assertEquals(0, earned.params["amount"])
        assertEquals(0, earned.params["balance_after"])

        assertEquals("resource_spent", spent.name)
        assertEquals("workshop_parts", spent.params["resource_type"])
        assertEquals("workshop_upgrade", spent.params["source"])
        assertEquals(20, spent.params["amount"])
        assertEquals(4, spent.params["balance_after"])
    }

    @Test
    fun `rewarded funnel events share a stable product schema`() {
        val offered = AnalyticsEvents.rewardedOfferShown(
            placement = "post_run_result",
            rewardType = "gems",
            rewardAmount = -12,
            daily = false,
        )
        val completed = AnalyticsEvents.rewardedCompleted(
            placement = "post_run_result",
            rewardType = "gems",
            rewardAmount = 12,
            daily = true,
        )

        assertEquals("rewarded_offer_shown", offered.name)
        assertEquals("post_run_result", offered.params["placement"])
        assertEquals("gems", offered.params["reward_type"])
        assertEquals(0, offered.params["reward_amount"])
        assertEquals(false, offered.params["daily"])

        assertEquals("rewarded_completed", completed.name)
        assertEquals("post_run_result", completed.params["placement"])
        assertEquals("gems", completed.params["reward_type"])
        assertEquals(12, completed.params["reward_amount"])
        assertEquals(true, completed.params["daily"])
    }
}
