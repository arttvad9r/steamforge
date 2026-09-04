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
        assertTrue(
            first.all {
                it.target > 0 && (it.reward as? ContractReward.WorkshopParts)?.amount?.let { amount -> amount > 0 } == true
            },
        )
    }

    @Test
    fun `different days rotate contract identity`() {
        val first = DailyContracts.forEpochDay(20_000L).map { it.id }
        val second = DailyContracts.forEpochDay(20_001L).map { it.id }
        assertNotEquals(first, second)
    }

    @Test
    fun `typed game events reduce into rich contract counters`() {
        val counters = ContractCounters().record(
            listOf(
                GameEvent.ScoreAdded(1_250),
                GameEvent.ScoreReached(1_250),
                GameEvent.TilesMerged(7),
                GameEvent.TileCreated(level = 7, count = 3),
                GameEvent.MovesSurvived(14),
                GameEvent.TileReached(8),
                GameEvent.ComboReached(3),
                GameEvent.OverdriveActivated(2),
                GameEvent.RunFinished,
            ),
        )

        assertEquals(
            ContractCounters(
                score = 1_250,
                bestRunScore = 1_250,
                merges = 7,
                moves = 14,
                runs = 1,
                maxTileLevel = 8,
                maxCombo = 3,
                overdrives = 2,
                madeTilesByLevel = mapOf(7 to 3),
            ),
            counters,
        )
    }

    @Test
    fun `game event reducer ignores invalid negative facts`() {
        val initial = ContractCounters(
            score = 9,
            bestRunScore = 20,
            merges = 4,
            moves = 3,
            maxTileLevel = 7,
            maxCombo = 2,
            overdrives = 1,
            madeTilesByLevel = mapOf(7 to 2),
        )
        val updated = initial.record(
            listOf(
                GameEvent.ScoreAdded(-100),
                GameEvent.ScoreReached(-10),
                GameEvent.TilesMerged(-4),
                GameEvent.TileCreated(-1, 3),
                GameEvent.TileCreated(7, -3),
                GameEvent.MovesSurvived(-3),
                GameEvent.TileReached(5),
                GameEvent.ComboReached(-2),
                GameEvent.OverdriveActivated(-1),
            ),
        )

        assertEquals(initial, updated)
    }

    @Test
    fun `rich live snapshot preserves high water through undo and adds only new tile counts`() {
        val day = 150L
        val seed = 44L
        val first = DailyContracts.recordLiveSnapshot(
            progress = PlayerProgress(),
            day = day,
            runSeed = seed,
            snapshot = ContractCounters(
                score = 800,
                bestRunScore = 800,
                merges = 9,
                moves = 15,
                maxTileLevel = 7,
                maxCombo = 2,
                madeTilesByLevel = mapOf(7 to 2),
            ),
        )
        val afterUndo = DailyContracts.recordLiveSnapshot(
            progress = first,
            day = day,
            runSeed = seed,
            snapshot = ContractCounters(
                score = 600,
                bestRunScore = 600,
                merges = 7,
                moves = 12,
                maxTileLevel = 6,
                maxCombo = 1,
                madeTilesByLevel = mapOf(7 to 1),
            ),
        )
        val replay = DailyContracts.recordLiveSnapshot(
            progress = afterUndo,
            day = day,
            runSeed = seed,
            snapshot = ContractCounters(
                score = 950,
                bestRunScore = 950,
                merges = 11,
                moves = 17,
                maxTileLevel = 8,
                maxCombo = 3,
                madeTilesByLevel = mapOf(7 to 2, 8 to 1),
            ),
        )

        assertEquals(950, replay.contracts.totals.score)
        assertEquals(950, replay.contracts.totals.bestRunScore)
        assertEquals(11, replay.contracts.totals.merges)
        assertEquals(17, replay.contracts.totals.moves)
        assertEquals(8, replay.contracts.totals.maxTileLevel)
        assertEquals(3, replay.contracts.totals.maxCombo)
        assertEquals(mapOf(7 to 2, 8 to 1), replay.contracts.totals.madeTilesByLevel)
    }

    @Test
    fun `claim grants workshop parts once and marks contract claimed`() {
        val day = 20_000L
        val contract = DailyContracts.forEpochDay(day).first()
        val reward = contract.reward as ContractReward.WorkshopParts
        val ledger = completedLedgerFor(contract, day)
        val initial = PlayerProgress(
            gems = 17,
            workshopParts = 5,
            contracts = ledger,
        )

        val claimed = DailyContracts.claim(initial, day, contract.id)
        val replay = DailyContracts.claim(claimed, day, contract.id)

        assertEquals(17, claimed.gems)
        assertEquals(5 + reward.amount, claimed.workshopParts)
        assertTrue(contract.id in claimed.contracts.claimedIds)
        assertEquals(claimed, replay)
    }

    @Test
    fun `incomplete contract cannot be claimed`() {
        val day = 20_001L
        val contract = DailyContracts.forEpochDay(day).first()
        val initial = PlayerProgress(
            workshopParts = 9,
            contracts = ContractLedger(day = day),
        )

        assertEquals(initial, DailyContracts.claim(initial, day, contract.id))
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
            snapshot = ContractCounters(score = 900, bestRunScore = 900, merges = 15, moves = 30, maxTileLevel = 6, maxCombo = 2, overdrives = 1),
        )

        val finished = DailyContracts.recordFinishedRun(
            progress = live,
            day = day,
            runSeed = seed,
            summary = GameSummary(score = 1200, merges = 20, moves = 36, maxTileLevel = 7, maxMergesInOneMove = 3, overdrives = 2),
        )

        assertEquals(1200, finished.contracts.totals.score)
        assertEquals(1200, finished.contracts.totals.bestRunScore)
        assertEquals(20, finished.contracts.totals.merges)
        assertEquals(36, finished.contracts.totals.moves)
        assertEquals(1, finished.contracts.totals.runs)
        assertEquals(7, finished.contracts.totals.maxTileLevel)
        assertEquals(3, finished.contracts.totals.maxCombo)
        assertEquals(2, finished.contracts.totals.overdrives)
        assertEquals(null, finished.contracts.activeRunSeed)
    }

    @Test
    fun `finishing daily run preserves active normal run high water`() {
        val day = 400L
        val normalSeed = 101L
        val dailySeed = 202L
        val withNormal = DailyContracts.recordLiveSnapshot(
            progress = PlayerProgress(),
            day = day,
            runSeed = normalSeed,
            snapshot = ContractCounters(score = 600, bestRunScore = 600, merges = 10, moves = 20, maxTileLevel = 6),
        )

        val afterDaily = DailyContracts.recordFinishedRun(
            progress = withNormal,
            day = day,
            runSeed = dailySeed,
            summary = GameSummary(score = 300, merges = 5, moves = 12, maxTileLevel = 5),
        )

        assertEquals(normalSeed, afterDaily.contracts.activeRunSeed)
        assertEquals(600, afterDaily.contracts.activeRun.score)
        assertEquals(900, afterDaily.contracts.totals.score)
        assertEquals(600, afterDaily.contracts.totals.bestRunScore)
        assertEquals(1, afterDaily.contracts.totals.runs)

        val resumedNormal = DailyContracts.recordLiveSnapshot(
            progress = afterDaily,
            day = day,
            runSeed = normalSeed,
            snapshot = ContractCounters(score = 650, bestRunScore = 650, merges = 11, moves = 21, maxTileLevel = 6),
        )
        assertEquals(950, resumedNormal.contracts.totals.score)
        assertEquals(650, resumedNormal.contracts.totals.bestRunScore)
    }

    private fun completedLedgerFor(def: ContractDef, day: Long): ContractLedger {
        val totals = when (def.type) {
            ContractType.MAKE_TILE -> ContractCounters(maxTileLevel = tileLevelForValue(def.target))
            ContractType.MERGE_COUNT -> ContractCounters(merges = def.target)
            ContractType.SCORE -> ContractCounters(score = def.target)
            ContractType.PLAY_RUNS -> ContractCounters(runs = def.target)
            ContractType.SURVIVE_MOVES -> ContractCounters(moves = def.target)
            ContractType.OVERDRIVE -> ContractCounters(overdrives = def.target)
        }
        return ContractLedger(day = day, totals = totals)
    }

    private fun tileLevelForValue(value: Int): Int {
        var level = 0
        var current = 1
        while (current < value) {
            current = current shl 1
            level++
        }
        return level
    }
}
