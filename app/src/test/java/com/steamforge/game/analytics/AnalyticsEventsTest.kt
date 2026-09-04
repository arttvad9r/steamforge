package com.steamforge.game.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsEventsTest {
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
}
