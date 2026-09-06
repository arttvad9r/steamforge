package com.steamforge.game.weekly.server.postgres

import com.steamforge.game.weekly.server.AcceptedWeeklyRun
import com.steamforge.game.weekly.server.WeeklyAuthenticatedPrincipal
import com.steamforge.game.weekly.server.WeeklyPopulationRecordResult
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.h2.jdbcx.JdbcDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostgresWeeklyRankingPopulationStoreTest {
    private lateinit var dataSource: JdbcDataSource
    private lateinit var store: PostgresWeeklyRankingPopulationStore

    @Before
    fun setUp() {
        dataSource = JdbcDataSource().apply {
            setURL("jdbc:h2:mem:weekly-${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
        }
        applyMigration()
        store = PostgresWeeklyRankingPopulationStore(dataSource)
    }

    @Test
    fun `first accepted attempt is ranked and duplicate does not replace it`() = runTest {
        val principal = WeeklyAuthenticatedPrincipal("vkid:player-1")
        val firstRun = run(score = 320)
        val laterRun = run(score = 640)

        val first = store.recordAndRank(principal, firstRun)
        val duplicate = store.recordAndRank(principal, laterRun)
        val secondParticipant = store.recordAndRank(
            WeeklyAuthenticatedPrincipal("vkid:player-2"),
            run(score = 160),
        )

        val ranked = first as WeeklyPopulationRecordResult.Ranked
        assertEquals(1, ranked.ranking.rank)
        assertEquals(1, ranked.ranking.participantCount)
        assertEquals(100.0, ranked.ranking.percentile, 0.0)
        assertEquals(WeeklyPopulationRecordResult.Duplicate, duplicate)
        assertEquals(320, storedScore(CHALLENGE_ID, principal.subject))
        assertEquals(2, (secondParticipant as WeeklyPopulationRecordResult.Ranked).ranking.participantCount)
    }

    @Test
    fun `rank and percentile use deduplicated accepted population`() = runTest {
        val high = ranked("high", score = 300)
        val low = ranked("low", score = 100)
        val middle = ranked("middle", score = 200)
        val tiedMiddle = ranked("middle-tie", score = 200)

        assertEquals(1, high.rank)
        assertEquals(1, high.participantCount)
        assertEquals(100.0, high.percentile, 0.0)

        assertEquals(2, low.rank)
        assertEquals(2, low.participantCount)
        assertEquals(50.0, low.percentile, 0.0)

        assertEquals(2, middle.rank)
        assertEquals(3, middle.participantCount)
        assertEquals(100.0 * 2.0 / 3.0, middle.percentile, 0.000001)

        assertEquals(2, tiedMiddle.rank)
        assertEquals(4, tiedMiddle.participantCount)
        assertEquals(75.0, tiedMiddle.percentile, 0.0)
    }

    @Test
    fun `population is isolated by challenge`() = runTest {
        ranked("player-a", score = 500, challengeId = CHALLENGE_ID)
        ranked("player-b", score = 100, challengeId = CHALLENGE_ID)

        val other = ranked("player-c", score = 50, challengeId = "weekly-other")

        assertEquals(1, other.rank)
        assertEquals(1, other.participantCount)
        assertEquals(100.0, other.percentile, 0.0)
    }

    @Test
    fun `same subject can participate once in each challenge`() = runTest {
        val principal = WeeklyAuthenticatedPrincipal("vkid:same-player")

        val first = store.recordAndRank(principal, run(score = 100, challengeId = CHALLENGE_ID))
        val second = store.recordAndRank(principal, run(score = 200, challengeId = "weekly-next"))

        assertTrue(first is WeeklyPopulationRecordResult.Ranked)
        assertTrue(second is WeeklyPopulationRecordResult.Ranked)
    }

    private suspend fun ranked(
        subject: String,
        score: Int,
        challengeId: String = CHALLENGE_ID,
    ) = (store.recordAndRank(
        WeeklyAuthenticatedPrincipal("vkid:$subject"),
        run(score = score, challengeId = challengeId),
    ) as WeeklyPopulationRecordResult.Ranked).ranking

    private fun run(
        score: Int,
        challengeId: String = CHALLENGE_ID,
    ) = AcceptedWeeklyRun(
        protocolVersion = 1,
        challengeId = challengeId,
        seed = 42L,
        score = score,
        maxTileLevel = 8,
    )

    private fun storedScore(challengeId: String, subject: String): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT score FROM weekly_ranked_attempts WHERE challenge_id = ? AND subject_key = ?",
            ).use { statement ->
                statement.setString(1, challengeId)
                statement.setString(2, subject)
                statement.executeQuery().use { result ->
                    check(result.next())
                    result.getInt(1)
                }
            }
        }

    private fun applyMigration() {
        val migration = requireNotNull(
            javaClass.getResource("/db/migration/V1__weekly_ranked_attempts.sql"),
        ).readText()

        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                migration.split(';')
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .forEach(statement::execute)
            }
        }
    }

    private companion object {
        const val CHALLENGE_ID = "weekly-20000"
    }
}
