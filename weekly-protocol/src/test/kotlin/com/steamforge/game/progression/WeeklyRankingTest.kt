package com.steamforge.game.progression

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WeeklyRankingTest {
    private val submission = WeeklyRunSubmission(
        protocolVersion = WeeklyRunReplay.PROTOCOL_VERSION,
        challengeId = "weekly-2026-36",
        seed = 123L,
        moveSequence = "LURD",
        finalScore = 4_096,
        finalMaxTileLevel = 9,
    )

    @Test
    fun `ranked result accepts matching score percentile and optional leaderboard metadata`() {
        val snapshot = WeeklyRankingSnapshot(
            challengeId = submission.challengeId,
            score = submission.finalScore,
            percentile = 93.4,
            rank = 66,
            participantCount = 1_000,
        )
        val result = WeeklyRankingResult.ranked(snapshot).verifiedFor(submission)

        assertEquals(WeeklyRankingStatus.RANKED, result.status)
        assertEquals(snapshot, result.ranking)
        assertTrue(snapshot.matches(submission))
    }

    @Test
    fun `ranking response fails closed when challenge or score does not match submission`() {
        val wrongChallenge = WeeklyRankingResult.ranked(
            WeeklyRankingSnapshot(
                challengeId = "weekly-other",
                score = submission.finalScore,
                percentile = 99.0,
            ),
        ).verifiedFor(submission)
        val wrongScore = WeeklyRankingResult.ranked(
            WeeklyRankingSnapshot(
                challengeId = submission.challengeId,
                score = submission.finalScore + 1,
                percentile = 99.0,
            ),
        ).verifiedFor(submission)

        assertEquals(WeeklyRankingStatus.REJECTED, wrongChallenge.status)
        assertNull(wrongChallenge.ranking)
        assertEquals(WeeklyRankingStatus.REJECTED, wrongScore.status)
        assertNull(wrongScore.ranking)
    }

    @Test
    fun `offline provider never fabricates percentile`() = runTest {
        val result = UnavailableWeeklyRankingProvider.submit(submission)

        assertEquals(WeeklyRankingStatus.UNAVAILABLE, result.status)
        assertNull(result.ranking)
    }

    @Test
    fun `ranking snapshot rejects invalid percentile and partial leaderboard metadata`() {
        assertIllegalArgument {
            WeeklyRankingSnapshot(
                challengeId = submission.challengeId,
                score = submission.finalScore,
                percentile = 100.1,
            )
        }
        assertIllegalArgument {
            WeeklyRankingSnapshot(
                challengeId = submission.challengeId,
                score = submission.finalScore,
                percentile = 50.0,
                rank = 1,
                participantCount = null,
            )
        }
        assertIllegalArgument {
            WeeklyRankingSnapshot(
                challengeId = submission.challengeId,
                score = submission.finalScore,
                percentile = 50.0,
                rank = 11,
                participantCount = 10,
            )
        }
    }

    private fun assertIllegalArgument(block: () -> Unit) {
        try {
            block()
            fail("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
}
