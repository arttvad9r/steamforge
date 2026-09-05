package com.steamforge.game.ui.contracts

import com.steamforge.game.progression.ContractDef
import com.steamforge.game.progression.ContractReward
import com.steamforge.game.progression.ContractType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstContractFocusTest {

    @Test
    fun `first contract focus only applies on first-run day before claim`() {
        assertTrue(
            shouldFocusFirstContract(
                gamesPlayed = 1,
                ledgerDay = 10,
                today = 10,
                claimedCount = 0,
            ),
        )
        assertFalse(
            shouldFocusFirstContract(
                gamesPlayed = 1,
                ledgerDay = 9,
                today = 10,
                claimedCount = 0,
            ),
        )
        assertFalse(
            shouldFocusFirstContract(
                gamesPlayed = 1,
                ledgerDay = 10,
                today = 10,
                claimedCount = 1,
            ),
        )
        assertFalse(
            shouldFocusFirstContract(
                gamesPlayed = 2,
                ledgerDay = 10,
                today = 10,
                claimedCount = 0,
            ),
        )
    }

    @Test
    fun `already completed contract wins first-step focus`() {
        val merge = item("merge", ContractType.MERGE_COUNT, target = 25, progress = 4)
        val score = item("score", ContractType.SCORE, target = 3_000, progress = 3_000)
        val combo = item("combo", ContractType.COMBO_COUNT, target = 2, progress = 0)

        val focused = prioritizeFirstContract(listOf(merge, score, combo), enabled = true)

        assertEquals("score", focused.first().def.id)
        assertTrue(focused.first().recommended)
        assertEquals(1, focused.count { it.recommended })
    }

    @Test
    fun `closest simple core contract wins when none are complete`() {
        val merge = item("merge", ContractType.MERGE_COUNT, target = 55, progress = 8)
        val runs = item("runs", ContractType.PLAY_RUNS, target = 2, progress = 1)
        val score = item("score", ContractType.SCORE, target = 3_000, progress = 2_900)

        val focused = prioritizeFirstContract(listOf(score, merge, runs), enabled = true)

        assertEquals("runs", focused.first().def.id)
        assertTrue(focused.first().recommended)
        assertEquals(listOf("runs", "score", "merge"), focused.map { it.def.id })
    }

    @Test
    fun `simple core concept is preferred over closer advanced task`() {
        val moves = item("moves", ContractType.SURVIVE_MOVES, target = 120, progress = 40)
        val score = item("score", ContractType.SCORE, target = 3_000, progress = 2_900)
        val combo = item("combo", ContractType.COMBO_COUNT, target = 2, progress = 1)

        val focused = prioritizeFirstContract(listOf(combo, score, moves), enabled = true)

        assertEquals("moves", focused.first().def.id)
        assertTrue(focused.first().recommended)
    }

    @Test
    fun `type priority breaks ties inside the same simple tier`() {
        val merge = item("merge", ContractType.MERGE_COUNT, target = 20, progress = 10)
        val moves = item("moves", ContractType.SURVIVE_MOVES, target = 100, progress = 50)

        val focused = prioritizeFirstContract(listOf(moves, merge), enabled = true)

        assertEquals("merge", focused.first().def.id)
    }

    @Test
    fun `normal contracts preserve original order and have no recommendation`() {
        val items = listOf(
            item("score", ContractType.SCORE),
            item("merge", ContractType.MERGE_COUNT),
            item("runs", ContractType.PLAY_RUNS),
        )

        val focused = prioritizeFirstContract(items, enabled = false)

        assertEquals(items, focused)
        assertFalse(focused.any { it.recommended })
    }

    private fun item(
        id: String,
        type: ContractType,
        target: Int = 10,
        progress: Int = 0,
    ) = ContractItemUi(
        def = ContractDef(
            id = id,
            type = type,
            target = target,
            reward = ContractReward.WorkshopParts(10),
            title = id,
            description = id,
        ),
        progress = progress,
        claimed = false,
    )
}
