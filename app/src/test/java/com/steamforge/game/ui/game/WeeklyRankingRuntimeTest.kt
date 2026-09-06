package com.steamforge.game.ui.game

import com.steamforge.game.progression.WeeklyRankingProvider
import com.steamforge.game.progression.WeeklyRankingResult
import com.steamforge.game.progression.WeeklyRankingSnapshot
import com.steamforge.game.progression.WeeklyRankingStatus
import com.steamforge.game.progression.WeeklyRunReplay
import com.steamforge.game.progression.WeeklyRunSubmission
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyRankingRuntimeTest {
    private val submission = WeeklyRunSubmission(
        protocolVersion = WeeklyRunReplay.PROTOCOL_VERSION,
        challengeId = "weekly-20000",
        seed = -4_709_770_106_441_314_684L,
        moveSequence = "LURD",
        finalScore = 128,
        finalMaxTileLevel = 5,
    )

    @Test
    fun `matching ranked result is published`() = runTest {
        val runtime = WeeklyRankingRuntime(
            scope = this,
            provider = providerReturning(
                WeeklyRankingResult.ranked(
                    WeeklyRankingSnapshot(
                        challengeId = submission.challengeId,
                        score = submission.finalScore,
                        percentile = 87.5,
                        rank = 25,
                        participantCount = 200,
                    ),
                ),
            ),
        )

        runtime.submit(submission)
        assertTrue(runtime.state.value.submitting)
        advanceUntilIdle()

        assertFalse(runtime.state.value.submitting)
        assertEquals(WeeklyRankingStatus.RANKED, runtime.state.value.result?.status)
        assertEquals(87.5, runtime.state.value.result?.ranking?.percentile ?: -1.0, 0.0)
    }

    @Test
    fun `mismatched ranked result fails closed to rejected`() = runTest {
        val runtime = WeeklyRankingRuntime(
            scope = this,
            provider = providerReturning(
                WeeklyRankingResult.ranked(
                    WeeklyRankingSnapshot(
                        challengeId = submission.challengeId,
                        score = submission.finalScore + 4,
                        percentile = 99.0,
                    ),
                ),
            ),
        )

        runtime.submit(submission)
        advanceUntilIdle()

        assertEquals(WeeklyRankingStatus.REJECTED, runtime.state.value.result?.status)
        assertNull(runtime.state.value.result?.ranking)
    }

    @Test
    fun `provider exception becomes unavailable`() = runTest {
        val runtime = WeeklyRankingRuntime(
            scope = this,
            provider = object : WeeklyRankingProvider {
                override suspend fun submit(submission: WeeklyRunSubmission): WeeklyRankingResult {
                    error("network failed")
                }
            },
        )

        runtime.submit(submission)
        advanceUntilIdle()

        assertEquals(WeeklyRankingStatus.UNAVAILABLE, runtime.state.value.result?.status)
    }

    @Test
    fun `reset clears state and blocks stale non cooperative response`() = runTest {
        val started = CompletableDeferred<Unit>()
        val runtime = WeeklyRankingRuntime(
            scope = this,
            provider = object : WeeklyRankingProvider {
                override suspend fun submit(submission: WeeklyRunSubmission): WeeklyRankingResult = try {
                    started.complete(Unit)
                    awaitCancellation()
                } catch (_: kotlinx.coroutines.CancellationException) {
                    WeeklyRankingResult.ranked(
                        WeeklyRankingSnapshot(
                            challengeId = submission.challengeId,
                            score = submission.finalScore,
                            percentile = 100.0,
                        ),
                    )
                }
            },
        )

        runtime.submit(submission)
        advanceUntilIdle()
        assertTrue(started.isCompleted)
        assertTrue(runtime.state.value.submitting)

        runtime.reset()
        advanceUntilIdle()

        assertFalse(runtime.state.value.submitting)
        assertNull(runtime.state.value.result)
    }

    private fun providerReturning(result: WeeklyRankingResult): WeeklyRankingProvider =
        object : WeeklyRankingProvider {
            override suspend fun submit(submission: WeeklyRunSubmission): WeeklyRankingResult = result
        }
}
