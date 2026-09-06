package com.steamforge.game.weekly.server.postgres

import com.steamforge.game.progression.WeeklyRankingSnapshot
import com.steamforge.game.weekly.server.AcceptedWeeklyRun
import com.steamforge.game.weekly.server.WeeklyAuthenticatedPrincipal
import com.steamforge.game.weekly.server.WeeklyPopulationRecordResult
import com.steamforge.game.weekly.server.WeeklyRankingPopulationStore
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PostgreSQL-backed accepted-population store for Weekly ranking V1.
 *
 * ADR 0004 defines first accepted attempt wins. The primary key on challenge + authenticated subject
 * therefore acts as the persistence-level deduplication boundary. A duplicate key is a normal product
 * outcome, while any other SQL failure is surfaced to [WeeklyRankingPopulationStore]'s caller.
 */
class PostgresWeeklyRankingPopulationStore(
    private val dataSource: DataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WeeklyRankingPopulationStore {

    override suspend fun recordAndRank(
        principal: WeeklyAuthenticatedPrincipal,
        run: AcceptedWeeklyRun,
    ): WeeklyPopulationRecordResult = withContext(ioDispatcher) {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            connection.transactionIsolation = Connection.TRANSACTION_SERIALIZABLE

            try {
                val inserted = tryInsert(connection, principal, run)
                if (!inserted) {
                    connection.rollback()
                    return@use WeeklyPopulationRecordResult.Duplicate
                }

                val participantCount = count(
                    connection = connection,
                    sql = "SELECT COUNT(*) FROM weekly_ranked_attempts WHERE challenge_id = ?",
                    challengeId = run.challengeId,
                )
                check(participantCount > 0) { "accepted population must include the inserted participant" }

                val higherScores = count(
                    connection = connection,
                    sql = "SELECT COUNT(*) FROM weekly_ranked_attempts WHERE challenge_id = ? AND score > ?",
                    challengeId = run.challengeId,
                    score = run.score,
                )
                val lowerOrEqualScores = count(
                    connection = connection,
                    sql = "SELECT COUNT(*) FROM weekly_ranked_attempts WHERE challenge_id = ? AND score <= ?",
                    challengeId = run.challengeId,
                    score = run.score,
                )

                val snapshot = WeeklyRankingSnapshot(
                    challengeId = run.challengeId,
                    score = run.score,
                    percentile = 100.0 * lowerOrEqualScores.toDouble() / participantCount.toDouble(),
                    rank = higherScores + 1,
                    participantCount = participantCount,
                )
                connection.commit()
                WeeklyPopulationRecordResult.Ranked(snapshot)
            } catch (failure: Throwable) {
                runCatching { connection.rollback() }
                throw failure
            }
        }
    }

    private fun tryInsert(
        connection: Connection,
        principal: WeeklyAuthenticatedPrincipal,
        run: AcceptedWeeklyRun,
    ): Boolean {
        return try {
            connection.prepareStatement(
                """
                INSERT INTO weekly_ranked_attempts (
                    challenge_id,
                    subject_key,
                    score,
                    max_tile_level,
                    protocol_version
                ) VALUES (?, ?, ?, ?, ?)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, run.challengeId)
                statement.setString(2, principal.subject)
                statement.setInt(3, run.score)
                statement.setInt(4, run.maxTileLevel)
                statement.setInt(5, run.protocolVersion)
                check(statement.executeUpdate() == 1) { "weekly accepted attempt insert did not affect one row" }
            }
            true
        } catch (failure: SQLException) {
            if (failure.sqlState == SQL_STATE_UNIQUE_VIOLATION) false else throw failure
        }
    }

    private fun count(
        connection: Connection,
        sql: String,
        challengeId: String,
        score: Int? = null,
    ): Int {
        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, challengeId)
            if (score != null) statement.setInt(2, score)
            statement.executeQuery().use { result ->
                check(result.next()) { "weekly ranking count query returned no row" }
                return result.getInt(1)
            }
        }
    }

    private companion object {
        const val SQL_STATE_UNIQUE_VIOLATION = "23505"
    }
}
