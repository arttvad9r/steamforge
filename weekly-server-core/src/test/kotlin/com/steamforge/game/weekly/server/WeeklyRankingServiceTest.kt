package com.steamforge.game.weekly.server

import com.steamforge.game.core.GameEngine
import com.steamforge.game.core.GameStatus
import com.steamforge.game.core.Move
import com.steamforge.game.core.ReplayableRandom
import com.steamforge.game.progression.WeeklyChallenges
import com.steamforge.game.progression.WeeklyRankingSnapshot
import com.steamforge.game.progression.WeeklyRankingStatus
import com.steamforge.game.progression.WeeklyRankingWire
import com.steamforge.game.progression.WeeklyRunReplay
import com.steamforge.game.progression.WeeklyRunSubmission
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyRankingServiceTest {
    private val validator = WeeklySubmissionValidator { challengeId ->
        CHALLENGE.takeIf { it.challengeId == challengeId }
    }

    @Test
    fun `authenticated accepted run is ranked from server population`() = runTest {
        val principal = WeeklyAuthenticatedPrincipal("provider-subject-123")
        var capturedPrincipal: WeeklyAuthenticatedPrincipal? = null
        var capturedRun: AcceptedWeeklyRun? = null
        val store = object : WeeklyRankingPopulationStore {
            override suspend fun recordAndRank(
                principal: WeeklyAuthenticatedPrincipal,
                run: AcceptedWeeklyRun,
            ): WeeklyRankingSnapshot {
                capturedPrincipal = principal
                capturedRun = run
                return WeeklyRankingSnapshot(
                    challengeId = run.challengeId,
                    score = run.score,
                    percentile = 87.5,
                    rank = 13,
                    participantCount = 100,
                )
            }
        }

        val result = WeeklyRankingService(validator, store).submit(principal, VALID_PAYLOAD)

        assertEquals(WeeklyRankingStatus.RANKED, result.status)
        assertEquals(principal, capturedPrincipal)
        assertEquals(VALID_SUBMISSION.challengeId, capturedRun?.challengeId)
        assertEquals(VALID_SUBMISSION.seed, capturedRun?.seed)
        assertEquals(VALID_SUBMISSION.finalScore, capturedRun?.score)
        assertEquals(87.5, result.ranking?.percentile ?: -1.0, 0.0)
        assertEquals(13, result.ranking?.rank)
        assertEquals(100, result.ranking?.participantCount)
    }

    @Test
    fun `forged submission is rejected before population storage`() = runTest {
        var storeCalled = false
        val store = object : WeeklyRankingPopulationStore {
            override suspend fun recordAndRank(
                principal: WeeklyAuthenticatedPrincipal,
                run: AcceptedWeeklyRun,
            ): WeeklyRankingSnapshot {
                storeCalled = true
                return matchingRanking(run)
            }
        }
        val forged = VALID_SUBMISSION.copy(finalScore = VALID_SUBMISSION.finalScore + 10_000)
        val payload = WeeklyRankingWire.encodeSubmission(forged)

        val result = WeeklyRankingService(validator, store).submit(PRINCIPAL, payload)

        assertEquals(WeeklyRankingStatus.REJECTED, result.status)
        assertFalse(storeCalled)
    }

    @Test
    fun `population result for another score fails closed to unavailable`() = runTest {
        val store = object : WeeklyRankingPopulationStore {
            override suspend fun recordAndRank(
                principal: WeeklyAuthenticatedPrincipal,
                run: AcceptedWeeklyRun,
            ): WeeklyRankingSnapshot = WeeklyRankingSnapshot(
                challengeId = run.challengeId,
                score = run.score + 4,
                percentile = 99.0,
                rank = 1,
                participantCount = 100,
            )
        }

        val result = WeeklyRankingService(validator, store).submit(PRINCIPAL, VALID_PAYLOAD)

        assertEquals(WeeklyRankingStatus.UNAVAILABLE, result.status)
    }

    @Test
    fun `population storage exception becomes unavailable`() = runTest {
        val store = object : WeeklyRankingPopulationStore {
            override suspend fun recordAndRank(
                principal: WeeklyAuthenticatedPrincipal,
                run: AcceptedWeeklyRun,
            ): WeeklyRankingSnapshot = error("database unavailable")
        }

        val result = WeeklyRankingService(validator, store).submit(PRINCIPAL, VALID_PAYLOAD)

        assertEquals(WeeklyRankingStatus.UNAVAILABLE, result.status)
    }

    @Test
    fun `population storage cancellation propagates`() = runTest {
        val store = object : WeeklyRankingPopulationStore {
            override suspend fun recordAndRank(
                principal: WeeklyAuthenticatedPrincipal,
                run: AcceptedWeeklyRun,
            ): WeeklyRankingSnapshot = throw CancellationException("request cancelled")
        }

        val failure = runCatching {
            WeeklyRankingService(validator, store).submit(PRINCIPAL, VALID_PAYLOAD)
        }.exceptionOrNull()

        assertTrue(failure is CancellationException)
    }

    @Test
    fun `authenticated principal rejects blank and oversized subjects`() {
        assertTrue(runCatching { WeeklyAuthenticatedPrincipal("   ") }.isFailure)
        assertTrue(runCatching { WeeklyAuthenticatedPrincipal("x".repeat(257)) }.isFailure)
        assertEquals("subject", WeeklyAuthenticatedPrincipal("subject").subject)
    }

    private fun matchingRanking(run: AcceptedWeeklyRun): WeeklyRankingSnapshot =
        WeeklyRankingSnapshot(
            challengeId = run.challengeId,
            score = run.score,
            percentile = 50.0,
            rank = 1,
            participantCount = 1,
        )

    private companion object {
        val CHALLENGE = WeeklyChallenges.forEpochDay(20_000L)
        val PRINCIPAL = WeeklyAuthenticatedPrincipal("provider-subject")
        val VALID_SUBMISSION: WeeklyRunSubmission by lazy {
            WeeklyRunReplay.submission(CHALLENGE, playToGameOver())
        }
        val VALID_PAYLOAD: String by lazy {
            WeeklyRankingWire.encodeSubmission(VALID_SUBMISSION)
        }

        fun playToGameOver(): List<Move> {
            val engine = GameEngine()
            val rng = ReplayableRandom(CHALLENGE.seed)
            var state = engine.newGame(rng = rng)
            val accepted = ArrayList<Move>()

            while (state.status == GameStatus.PLAYING && accepted.size < WeeklyRunReplay.MAX_INPUT_MOVES) {
                val order = MOVE_ORDERS[accepted.size % MOVE_ORDERS.size]
                val move = order.firstOrNull { candidate ->
                    engine.applyMove(state, candidate, Random(0)).moved
                } ?: break
                val result = engine.applyMove(state, move, rng)
                require(result.moved)
                accepted += move
                state = result.state
            }

            check(state.status == GameStatus.GAME_OVER)
            check(accepted.size < WeeklyRunReplay.MAX_INPUT_MOVES)
            return accepted
        }

        val MOVE_ORDERS = listOf(
            listOf(Move.DOWN, Move.LEFT, Move.RIGHT, Move.UP),
            listOf(Move.LEFT, Move.DOWN, Move.RIGHT, Move.UP),
            listOf(Move.DOWN, Move.RIGHT, Move.LEFT, Move.UP),
            listOf(Move.RIGHT, Move.DOWN, Move.LEFT, Move.UP),
        )
    }
}
