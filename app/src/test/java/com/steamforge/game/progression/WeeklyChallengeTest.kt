package com.steamforge.game.progression

import com.steamforge.game.core.GameEngine
import com.steamforge.game.core.GameStatus
import com.steamforge.game.core.Move
import com.steamforge.game.core.ReplayableRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyChallengeTest {

    private val cfg = ProgressionConfig()

    @Test
    fun `all days in a weekly window share challenge and next window rotates`() {
        val challenge = WeeklyChallenges.forEpochDay(20_000L)

        for (day in challenge.startEpochDay until challenge.endEpochDayExclusive) {
            assertEquals(challenge, WeeklyChallenges.forEpochDay(day))
        }

        val next = WeeklyChallenges.forEpochDay(challenge.endEpochDayExclusive)
        assertNotEquals(challenge.id, next.id)
        assertNotEquals(challenge.seed, next.seed)
        assertEquals(7, challenge.durationDays)
    }

    @Test
    fun `replay codec is reversible and rejects unknown commands`() {
        val moves = listOf(Move.UP, Move.LEFT, Move.DOWN, Move.RIGHT, Move.RIGHT)
        val encoded = WeeklyReplayCodec.encode(moves)

        assertEquals(moves, WeeklyReplayCodec.decode(encoded))
        assertNull(WeeklyReplayCodec.decode(encoded + "X"))
    }

    @Test
    fun `full deterministic run is replay verified`() {
        val challenge = WeeklyChallenges.forEpochDay(20_000L)
        val generated = generateFinishedReplay(challenge)

        val verified = WeeklyChallengeVerifier.verify(challenge, generated.moves)

        assertNotNull(verified)
        assertEquals(generated.score, verified?.score)
        assertEquals(generated.maxTileLevel, verified?.maxTileLevel)
        assertEquals(generated.moves.size, verified?.moves)
        assertEquals(WeeklyReplayCodec.encode(generated.moves), verified?.replay)
    }

    @Test
    fun `partial or post game tampered replay is rejected`() {
        val challenge = WeeklyChallenges.forEpochDay(20_001L)
        val generated = generateFinishedReplay(challenge)

        assertNull(WeeklyChallengeVerifier.verify(challenge, generated.moves.dropLast(1)))
        assertNull(WeeklyChallengeVerifier.verify(challenge, generated.moves + Move.LEFT))
    }

    @Test
    fun `weekly reward resets on rotation and becomes single claim after verified result`() {
        val current = WeeklyChallenges.forEpochDay(20_000L)
        val previous = WeeklyChallenges.forEpochDay(current.startEpochDay - 1L)
        val oldRecord = WeeklyRecord(
            challengeId = previous.id,
            bestScore = 9_999,
            bestMoves = "LR",
            rewardClaimed = true,
        )

        val normalized = WeeklyProgression.normalized(oldRecord, current)
        assertEquals(current.id, normalized.challengeId)
        assertEquals(0, normalized.bestScore)
        assertFalse(normalized.rewardClaimed)
        assertFalse(WeeklyProgression.canClaimReward(normalized, current))

        val verified = WeeklyVerifiedResult(
            challengeId = current.id,
            score = 1_500,
            maxTileLevel = 8,
            moves = 120,
            replay = "LDRU",
        )
        val recorded = WeeklyProgression.recordVerified(normalized, current, verified)
        assertTrue(WeeklyProgression.canClaimReward(recorded, current))

        val claimed = WeeklyProgression.markRewardClaimed(recorded, current)
        assertNotNull(claimed)
        assertTrue(claimed?.rewardClaimed == true)
        assertFalse(WeeklyProgression.canClaimReward(claimed!!, current))
        assertNull(WeeklyProgression.markRewardClaimed(claimed, current))
    }

    private data class GeneratedRun(
        val moves: List<Move>,
        val score: Int,
        val maxTileLevel: Int,
    )

    private fun generateFinishedReplay(challenge: WeeklyChallenge): GeneratedRun {
        val engine = GameEngine()
        val rng = ReplayableRandom(challenge.seed)
        var state = engine.newGame(rng = rng)
        var pressure = 0
        var overdriveRemaining = 0
        val replay = mutableListOf<Move>()
        val directions = listOf(Move.LEFT, Move.DOWN, Move.RIGHT, Move.UP)
        var cursor = 0
        var guard = 0

        while (state.status != GameStatus.GAME_OVER && guard++ < 20_000) {
            var accepted = false
            for (offset in directions.indices) {
                val move = directions[(cursor + offset) % directions.size]
                val multiplier = if (overdriveRemaining > 0) cfg.overdriveMultiplier else 1
                val result = engine.applyMove(state, move, rng, multiplier)
                if (!result.moved) continue

                replay += move
                cursor = (cursor + offset + 1) % directions.size
                if (overdriveRemaining > 0) {
                    overdriveRemaining = (overdriveRemaining - result.merges.size).coerceAtLeast(0)
                } else {
                    pressure += result.merges.sumOf { cfg.pressureGainForMerge(it.tile.level) }
                    if (pressure >= cfg.pressureMax) {
                        pressure = 0
                        overdriveRemaining = cfg.overdriveMerges
                    }
                }
                state = result.state
                accepted = true
                break
            }
            if (!accepted) break
        }

        assertEquals(GameStatus.GAME_OVER, state.status)
        assertTrue(replay.isNotEmpty())
        return GeneratedRun(replay, state.score, state.maxLevel)
    }
}
