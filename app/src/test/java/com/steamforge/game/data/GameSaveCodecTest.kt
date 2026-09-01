package com.steamforge.game.data

import com.steamforge.game.core.GameState
import com.steamforge.game.core.Tile
import com.steamforge.game.progression.RewardedWorkshopBonus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameSaveCodecTest {

    @Test
    fun `v4 roundtrip preserves board rng and session counters`() {
        val original = SavedGame(
            state = GameState(
                tiles = listOf(Tile(1, 1, 0, 0), Tile(7, 5, 3, 2)),
                score = 1234,
                nextTileId = 8,
                won = true,
                moves = 42,
            ),
            seed = 987654321L,
            pressure = 73,
            overdriveRemaining = 3,
            freeUndosLeft = 1,
            rngDraws = 37L,
            mergesTotal = 29,
            maxMergesInOneMove = 3,
            overdrivesSession = 2,
            undosSession = 4,
            highMergesSession = 5,
        )
        assertEquals(original, GameSaveCodec.decode(GameSaveCodec.encode(original)))
    }

    @Test
    fun `finished game codec preserves rewarded workshop bonus for process recreation`() {
        val bonus = RewardedWorkshopBonus(xpGained = 180, gemsGained = 30, levelUps = listOf(2, 3))
        val original = FinishedGameRecord(
            id = "run-42",
            day = 20_698L,
            daily = false,
            score = 4_096,
            maxTileLevel = 11,
            xpGained = 180,
            state = "final-state",
        ).withRewardedBonus(bonus)

        val restored = FinishedGameCodec.decode(FinishedGameCodec.encode(original))!!

        assertEquals(true, restored.rewardedClaimed)
        assertEquals(bonus, restored.rewardedBonus())
        assertEquals(original, restored)
    }

    @Test
    fun `legacy finished game without rewarded details remains readable`() {
        val raw = """{"id":"old-run","day":20698,"daily":false,"score":100,"maxTileLevel":4,"state":"legacy","rewardedClaimed":true}"""
        val restored = FinishedGameCodec.decode(raw)!!

        assertEquals(true, restored.rewardedClaimed)
        assertEquals(RewardedWorkshopBonus(), restored.rewardedBonus())
    }

    @Test
    fun `v3 remains readable with zero session counters`() {
        val raw = "v3|4|100|3|0|5|42|70|2|1|17|1,1,0,0;2,2,1,1"
        val decoded = GameSaveCodec.decode(raw)!!
        assertEquals(42L, decoded.seed)
        assertEquals(70, decoded.pressure)
        assertEquals(2, decoded.overdriveRemaining)
        assertEquals(1, decoded.freeUndosLeft)
        assertEquals(17L, decoded.rngDraws)
        assertEquals(0, decoded.mergesTotal)
        assertEquals(0, decoded.maxMergesInOneMove)
        assertEquals(0, decoded.overdrivesSession)
        assertEquals(0, decoded.undosSession)
        assertEquals(0, decoded.highMergesSession)
    }

    @Test
    fun `v2 remains readable with zero rng position`() {
        val raw = "v2|4|100|3|0|5|42|70|2|1|1,1,0,0;2,2,1,1"
        val decoded = GameSaveCodec.decode(raw)!!
        assertEquals(42L, decoded.seed)
        assertEquals(70, decoded.pressure)
        assertEquals(2, decoded.overdriveRemaining)
        assertEquals(1, decoded.freeUndosLeft)
        assertEquals(0L, decoded.rngDraws)
    }

    @Test
    fun `v1 remains readable with safe defaults`() {
        val raw = "v1|4|100|3|0|5|1,1,0,0;2,2,1,1"
        val decoded = GameSaveCodec.decode(raw)!!
        assertEquals(null, decoded.seed)
        assertEquals(0, decoded.pressure)
        assertEquals(0, decoded.overdriveRemaining)
        assertEquals(0, decoded.freeUndosLeft)
        assertEquals(0L, decoded.rngDraws)
    }

    @Test
    fun `broken or structurally invalid input returns null`() {
        assertNull(GameSaveCodec.decode("garbage"))
        assertNull(GameSaveCodec.decode("v4|bad"))
        assertNull(GameSaveCodec.decode("v3|bad"))
        assertNull(GameSaveCodec.decode("v3|4|0|1|0|0|1|0|0|0|0|bad,tile"))
        assertNull(GameSaveCodec.decode("v3|4|0|3|0|0|1|0|0|0|0|1,1,0,0;2,2,0,0"))
        assertNull(GameSaveCodec.decode("v3|4|0|2|0|0|1|0|0|0|0|2,1,0,0"))
        assertNull(GameSaveCodec.decode("v3|4|0|3|0|0|1|0|0|0|0|1,1,4,0;2,2,1,1"))
    }
}
