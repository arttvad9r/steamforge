package com.steamforge.game.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveOpsTest {

    @Test
    fun `foundry week has configured milestones and high merge scoring`() {
        val event = LiveOpsCatalog.foundryWeek(20_000L)

        assertEquals(listOf(100, 250, 500, 900, 1500), event.milestones.map { it.targetPoints })
        assertEquals(EventMetric.HIGH_MERGES, event.scoringRule.metric)
        assertEquals(100, LiveOpsProgression.pointsFor(event.scoringRule, EventRunCounters(highMerges = 4)))
        assertTrue(event.isActive(20_003L))
        assertFalse(event.isActive(20_007L))
    }

    @Test
    fun `same engine supports score based event without new progression code`() {
        val event = EventDefinition(
            id = "calibration-test",
            startEpochDay = 1L,
            endEpochDayExclusive = 8L,
            scoringRule = EventScoringRule(EventMetric.SCORE, pointsPerUnit = 10, unitsPerStep = 500),
            milestones = listOf(EventMilestone("m1", 20, EventReward(gems = 1))),
            theme = EventTheme("calibration", "CALIBRATION", "Score test", "teal"),
        )

        val points = LiveOpsProgression.pointsFor(event.scoringRule, EventRunCounters(score = 1_499))
        assertEquals(20, points)
        assertTrue(LiveOpsProgression.canClaim(LiveOpsLedger(event.id, totalPoints = 20), event, event.milestones.first()))
    }

    @Test
    fun `live snapshot uses high water and undo cannot count same progress twice`() {
        val event = LiveOpsCatalog.foundryWeek(20_000L)
        val first = LiveOpsProgression.recordLiveSnapshot(
            LiveOpsLedger(), event, 77L, EventRunCounters(highMerges = 4),
        )
        val afterMore = LiveOpsProgression.recordLiveSnapshot(
            first, event, 77L, EventRunCounters(highMerges = 6),
        )
        val afterUndoReplay = LiveOpsProgression.recordLiveSnapshot(
            afterMore, event, 77L, EventRunCounters(highMerges = 5),
        )
        val afterReplayingSamePeak = LiveOpsProgression.recordLiveSnapshot(
            afterUndoReplay, event, 77L, EventRunCounters(highMerges = 6),
        )

        assertEquals(100, first.totalPoints)
        assertEquals(150, afterMore.totalPoints)
        assertEquals(150, afterUndoReplay.totalPoints)
        assertEquals(150, afterReplayingSamePeak.totalPoints)
    }

    @Test
    fun `finishing side run preserves baseline of active normal run`() {
        val event = LiveOpsCatalog.foundryWeek(20_000L)
        val normalBaseline = LiveOpsLedger(
            eventId = event.id,
            totalPoints = 100,
            activeRunSeed = 11L,
            activeRunPoints = 100,
        )

        val afterSideRun = LiveOpsProgression.recordFinishedRun(
            normalBaseline,
            event,
            runSeed = 22L,
            counters = EventRunCounters(highMerges = 3),
        )

        assertEquals(175, afterSideRun.totalPoints)
        assertEquals(11L, afterSideRun.activeRunSeed)
        assertEquals(100, afterSideRun.activeRunPoints)

        val resumedNormal = LiveOpsProgression.recordLiveSnapshot(
            afterSideRun,
            event,
            runSeed = 11L,
            counters = EventRunCounters(highMerges = 5),
        )
        assertEquals(200, resumedNormal.totalPoints)
    }

    @Test
    fun `event rotation resets milestones and preserves new run baseline only`() {
        val old = LiveOpsCatalog.foundryWeek(20_000L)
        val next = LiveOpsCatalog.foundryWeek(20_007L)
        val oldLedger = LiveOpsLedger(
            eventId = old.id,
            totalPoints = 900,
            claimedMilestones = setOf("pressure-100", "pressure-250"),
        )

        val normalized = LiveOpsProgression.normalized(oldLedger, next)
        assertEquals(next.id, normalized.eventId)
        assertEquals(0, normalized.totalPoints)
        assertTrue(normalized.claimedMilestones.isEmpty())
    }

    @Test
    fun `milestone claim is gated and idempotent`() {
        val event = LiveOpsCatalog.foundryWeek(20_000L)
        val milestone = event.milestones.first()
        val tooEarly = LiveOpsLedger(eventId = event.id, totalPoints = 75)
        assertFalse(LiveOpsProgression.canClaim(tooEarly, event, milestone))
        assertNull(LiveOpsProgression.markClaimed(tooEarly, event, milestone))

        val ready = tooEarly.copy(totalPoints = 100)
        val claimed = LiveOpsProgression.markClaimed(ready, event, milestone)
        assertNotNull(claimed)
        assertTrue(milestone.id in claimed!!.claimedMilestones)
        assertNull(LiveOpsProgression.markClaimed(claimed, event, milestone))
    }
}
