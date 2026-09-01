package com.steamforge.game.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractsTest {

    @Test
    fun `same day produces same three unique contracts`() {
        val first = DailyContracts.forEpochDay(20_000L)
        val second = DailyContracts.forEpochDay(20_000L)

        assertEquals(3, first.size)
        assertEquals(first, second)
        assertEquals(3, first.map { it.type }.toSet().size)
        assertTrue(first.all { it.target > 0 && it.rewardGems > 0 })
    }

    @Test
    fun `different days rotate contract identity`() {
        val first = DailyContracts.forEpochDay(20_000L).map { it.id }
        val second = DailyContracts.forEpochDay(20_001L).map { it.id }
        assertNotEquals(first, second)
    }

    @Test
    fun `new day resets counters claims and active run`() {
        val old = ContractLedger(
            day = 100L,
            totals = ContractCounters(score = 9000, merges = 90, moves = 120, runs = 3, maxTileLevel = 9),
            claimedIds = setOf("claimed"),
            activeRunSeed = 42L,
            activeRun = ContractCounters(score = 500),
        )

        val normalized = DailyContracts.normalized(old, 101L)

        assertEquals(101L, normalized.day)
        assertEquals(ContractCounters(), normalized.totals)
        assertTrue(normalized.claimedIds.isEmpty())
        assertEquals(null, normalized.activeRunSeed)
    }

    @Test
    fun `live snapshots only add high water delta for the same run`() {
        val day = 200L
        val seed = 77L
        val first = DailyContracts.recordLiveSnapshot(
            progress = PlayerProgress(),
            day = day,
            runSeed = seed,
            snapshot = ContractCounters(score = 400, merges = 8, moves = 12, maxTileLevel = 5, overdrives = 1),
        )
        val afterUndo = DailyContracts.recordLiveSnapshot(
            progress = first,
            day = day,
            runSeed = seed,
            snapshot = ContractCounters(score = 250, merges = 5, moves = 9, maxTileLevel = 4, overdrives = 0),
        )
        val replay = DailyContracts.recordLiveSnapshot(
            progress = afterUndo,
            day = day,
            runSeed = seed,
            snapshot = ContractCounters(score = 450, merges = 9, moves = 13, maxTileLevel = 5, overdrives = 1),
        )

        assertEquals(450, replay.contracts.totals.score)
        assertEquals(9, replay.contracts.totals.merges)
        assertEquals(13, replay.contracts.totals.moves)
        assertEquals(5, replay.contracts.totals.maxTileLevel)
        assertEquals(1, replay.contracts.totals.overdrives)
    }

    @Test
    fun `finishing a run adds only missing snapshot delta and one run`() {
        val day = 300L
        val seed = 88L
        val live = DailyContracts.recordLiveSnapshot(
            progress = PlayerProgress(),
            day = day,
            runSeed = seed,
            snapshot = ContractCounters(score = 900, merges = 15, moves = 30, maxTileLevel = 6, overdrives = 1),
        )

        val finished = DailyContracts.recordFinishedRun(
            progress = live,
            day = day,
            runSeed = seed,
            summary = GameSummary(score = 1200, merges = 20, moves = 36, maxTileLevel = 7, overdrives = 2),
        )

        assertEquals(1200, finished.contracts.totals.score)
        assertEquals(20, finished.contracts.totals.merges)
        assertEquals(36, finished.contracts.totals.moves)
        assertEquals(1, finished.contracts.totals.runs)
        assertEquals(7, finished.contracts.totals.maxTileLevel)
        assertEquals(2, finished.contracts.totals.overdrives)
        assertEquals(null, finished.contracts.activeRunSeed)
    }
}
