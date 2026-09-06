package com.steamforge.game.progression

import com.steamforge.game.core.Move
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyRankingWireTest {
    private val challenge = WeeklyChallenges.forEpochDay(20_000L)
    private val submission = WeeklyRunReplay.submission(
        challenge,
        listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN),
    )

    @Test
    fun `submission preserves full 64 bit seed as JSON string`() {
        assertTrue(kotlin.math.abs(challenge.seed) > JS_SAFE_INTEGER_MAX)

        val encoded = WeeklyRankingWire.encodeSubmission(submission)
        val root = Json.parseToJsonElement(encoded).jsonObject
        val seed = requireNotNull(root["seed"]).jsonPrimitive

        assertTrue(seed.isString)
        assertEquals(challenge.seed.toString(), seed.content)
        assertEquals(submission, WeeklyRankingWire.decodeSubmission(encoded))
    }

    @Test
    fun `server result encoder round trips all stable statuses`() {
        val ranked = WeeklyRankingResult.ranked(
            WeeklyRankingSnapshot(
                challengeId = submission.challengeId,
                score = submission.finalScore,
                percentile = 87.5,
                rank = 13,
                participantCount = 100,
            ),
        )
        val cases = listOf(
            ranked to "ranked",
            WeeklyRankingResult.rejected() to "rejected",
            WeeklyRankingResult.unavailable() to "unavailable",
        )

        cases.forEach { (result, expectedStatus) ->
            val encoded = WeeklyRankingWire.encodeResult(result)
            val root = Json.parseToJsonElement(encoded).jsonObject

            assertEquals(
                WeeklyRunReplay.PROTOCOL_VERSION.toString(),
                requireNotNull(root["protocolVersion"]).jsonPrimitive.content,
            )
            assertEquals(expectedStatus, requireNotNull(root["status"]).jsonPrimitive.content)
            assertEquals(result, WeeklyRankingWire.decodeResult(encoded))
        }
    }

    @Test
    fun `ranked response decodes and remains bound to submitted challenge and score`() {
        val payload = """
            {
              "protocolVersion": ${WeeklyRunReplay.PROTOCOL_VERSION},
              "status": "ranked",
              "challengeId": "${submission.challengeId}",
              "score": ${submission.finalScore},
              "percentile": 87.5,
              "rank": 13,
              "participantCount": 100
            }
        """.trimIndent()

        val result = requireNotNull(WeeklyRankingWire.decodeResult(payload))

        assertEquals(WeeklyRankingStatus.RANKED, result.status)
        assertEquals(87.5, result.ranking?.percentile ?: -1.0, 0.0)
        assertEquals(13, result.ranking?.rank)
        assertEquals(100, result.ranking?.participantCount)
        assertEquals(WeeklyRankingStatus.RANKED, result.verifiedFor(submission).status)
        assertEquals(
            WeeklyRankingStatus.REJECTED,
            result.verifiedFor(submission.copy(finalScore = submission.finalScore + 1)).status,
        )
    }

    @Test
    fun `invalid protocol and ranking invariants fail closed`() {
        val wrongSubmissionProtocol = WeeklyRankingWire.encodeSubmission(submission)
            .replace(
                "\"protocolVersion\":${WeeklyRunReplay.PROTOCOL_VERSION}",
                "\"protocolVersion\":999",
            )
        val wrongResultProtocol = """
            {"protocolVersion":999,"status":"unavailable"}
        """.trimIndent()
        val badPercentile = """
            {
              "protocolVersion": ${WeeklyRunReplay.PROTOCOL_VERSION},
              "status": "ranked",
              "challengeId": "${submission.challengeId}",
              "score": ${submission.finalScore},
              "percentile": 100.1
            }
        """.trimIndent()
        val incompleteLeaderboard = """
            {
              "protocolVersion": ${WeeklyRunReplay.PROTOCOL_VERSION},
              "status": "ranked",
              "challengeId": "${submission.challengeId}",
              "score": ${submission.finalScore},
              "percentile": 50.0,
              "rank": 2
            }
        """.trimIndent()

        assertNull(WeeklyRankingWire.decodeSubmission(wrongSubmissionProtocol))
        assertNull(WeeklyRankingWire.decodeResult(wrongResultProtocol))
        assertNull(WeeklyRankingWire.decodeResult(badPercentile))
        assertNull(WeeklyRankingWire.decodeResult(incompleteLeaderboard))
    }

    @Test
    fun `rejected and unavailable responses never fabricate ranking data`() {
        val rejected = WeeklyRankingWire.decodeResult(
            """{"protocolVersion":${WeeklyRunReplay.PROTOCOL_VERSION},"status":"rejected"}""",
        )
        val unavailable = WeeklyRankingWire.decodeResult(
            """{"protocolVersion":${WeeklyRunReplay.PROTOCOL_VERSION},"status":"unavailable"}""",
        )

        assertEquals(WeeklyRankingStatus.REJECTED, rejected?.status)
        assertNull(rejected?.ranking)
        assertEquals(WeeklyRankingStatus.UNAVAILABLE, unavailable?.status)
        assertNull(unavailable?.ranking)
    }

    @Test
    fun `wire decoder rejects oversized or non decimal seed payloads`() {
        val oversizedMoves = "L".repeat(WeeklyRunReplay.MAX_INPUT_MOVES + 1)
        val oversized = """
            {
              "protocolVersion": ${WeeklyRunReplay.PROTOCOL_VERSION},
              "challengeId": "${submission.challengeId}",
              "seed": "${submission.seed}",
              "moveSequence": "$oversizedMoves",
              "finalScore": 0,
              "finalMaxTileLevel": 1
            }
        """.trimIndent()
        val badSeed = WeeklyRankingWire.encodeSubmission(submission)
            .replace("\"${submission.seed}\"", "\"not-a-long\"")

        assertNull(WeeklyRankingWire.decodeSubmission(oversized))
        assertNull(WeeklyRankingWire.decodeSubmission(badSeed))
        assertFalse(WeeklyRankingWire.encodeSubmission(submission).isBlank())
    }

    private companion object {
        const val JS_SAFE_INTEGER_MAX = 9_007_199_254_740_991L
    }
}
