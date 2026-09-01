package com.steamforge.game.progression

import com.steamforge.game.core.GameEngine
import com.steamforge.game.core.GameStatus
import com.steamforge.game.core.Move
import com.steamforge.game.core.ReplayableRandom

data class WeeklyChallenge(
    val id: String,
    val seed: Long,
    val startEpochDay: Long,
    val endEpochDayExclusive: Long,
    val rewardGems: Int = 30,
) {
    val durationDays: Int get() = (endEpochDayExclusive - startEpochDay).toInt()
}

data class WeeklyRecord(
    val challengeId: String = "",
    val bestScore: Int = 0,
    val bestMoves: String = "",
    val rewardClaimed: Boolean = false,
)

data class WeeklyVerifiedResult(
    val challengeId: String,
    val score: Int,
    val maxTileLevel: Int,
    val moves: Int,
    val replay: String,
)

object WeeklyChallenges {
    private const val DAYS_PER_WEEK = 7L

    fun forEpochDay(epochDay: Long): WeeklyChallenge {
        val start = epochDay - Math.floorMod(epochDay + 3L, DAYS_PER_WEEK)
        val seed = start * 2_862_933_555_777_941_757L + 3_037_000_493L
        return WeeklyChallenge(
            id = "weekly-$start",
            seed = seed,
            startEpochDay = start,
            endEpochDayExclusive = start + DAYS_PER_WEEK,
        )
    }

    fun daysRemaining(challenge: WeeklyChallenge, epochDay: Long): Int =
        (challenge.endEpochDayExclusive - epochDay).coerceIn(0L, DAYS_PER_WEEK).toInt()
}

object WeeklyProgression {
    fun normalized(record: WeeklyRecord, challenge: WeeklyChallenge): WeeklyRecord =
        if (record.challengeId == challenge.id) record else WeeklyRecord(challengeId = challenge.id)

    fun recordVerified(
        record: WeeklyRecord,
        challenge: WeeklyChallenge,
        result: WeeklyVerifiedResult,
    ): WeeklyRecord {
        require(result.challengeId == challenge.id)
        val current = normalized(record, challenge)
        return if (result.score > current.bestScore) {
            current.copy(bestScore = result.score, bestMoves = result.replay)
        } else {
            current
        }
    }

    fun canClaimReward(record: WeeklyRecord, challenge: WeeklyChallenge): Boolean {
        val current = normalized(record, challenge)
        return current.bestScore > 0 && !current.rewardClaimed
    }

    fun markRewardClaimed(record: WeeklyRecord, challenge: WeeklyChallenge): WeeklyRecord? {
        val current = normalized(record, challenge)
        if (!canClaimReward(current, challenge)) return null
        return current.copy(rewardClaimed = true)
    }
}

object WeeklyReplayCodec {
    fun encode(moves: List<Move>): String = buildString(moves.size) {
        moves.forEach { move ->
            append(
                when (move) {
                    Move.UP -> 'U'
                    Move.DOWN -> 'D'
                    Move.LEFT -> 'L'
                    Move.RIGHT -> 'R'
                },
            )
        }
    }

    fun decode(raw: String): List<Move>? = runCatching {
        raw.map { char ->
            when (char) {
                'U' -> Move.UP
                'D' -> Move.DOWN
                'L' -> Move.LEFT
                'R' -> Move.RIGHT
                else -> error("Unknown weekly move: $char")
            }
        }
    }.getOrNull()
}

/**
 * Weekly result не доверяет переданному score. Он заново проигрывает seed + move sequence и возвращает
 * вычисленный результат только если каждый ход валиден и sequence действительно заканчивается Game Over.
 * Undo и Wrench в weekly UI отключены, поэтому replay состоит только из чистых Move-команд.
 */
object WeeklyChallengeVerifier {
    private const val MAX_REPLAY_MOVES = 100_000

    fun verify(
        challenge: WeeklyChallenge,
        moves: List<Move>,
        cfg: ProgressionConfig = ProgressionConfig(),
    ): WeeklyVerifiedResult? {
        if (moves.isEmpty() || moves.size > MAX_REPLAY_MOVES) return null

        val engine = GameEngine()
        val rng = ReplayableRandom(challenge.seed)
        var state = engine.newGame(rng = rng)
        var pressure = 0
        var overdriveRemaining = 0

        moves.forEach { move ->
            if (state.status == GameStatus.GAME_OVER) return null
            val multiplier = if (overdriveRemaining > 0) cfg.overdriveMultiplier else 1
            val result = engine.applyMove(state, move, rng, multiplier)
            if (!result.moved) return null

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
        }

        if (state.status != GameStatus.GAME_OVER) return null
        return WeeklyVerifiedResult(
            challengeId = challenge.id,
            score = state.score,
            maxTileLevel = state.maxLevel,
            moves = state.moves,
            replay = WeeklyReplayCodec.encode(moves),
        )
    }
}
