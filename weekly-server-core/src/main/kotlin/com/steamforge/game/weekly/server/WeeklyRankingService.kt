package com.steamforge.game.weekly.server

import com.steamforge.game.progression.WeeklyRankingResult
import com.steamforge.game.progression.WeeklyRankingSnapshot
import kotlin.coroutines.cancellation.CancellationException

/**
 * Server-recognized identity established by the deployment authentication layer.
 *
 * This value must never be constructed directly from an identifier in the ranking request body. The
 * transport/auth adapter is responsible for authenticating a request first and only then constructing
 * this principal from the verified provider subject/session identity.
 */
data class WeeklyAuthenticatedPrincipal(
    val subject: String,
) {
    init {
        require(subject.isNotBlank()) { "authenticated subject must not be blank" }
        require(subject.length <= MAX_SUBJECT_LENGTH) { "authenticated subject is too long" }
    }

    private companion object {
        const val MAX_SUBJECT_LENGTH = 256
    }
}

/** Persistence outcome for the first-accepted-attempt policy defined by ADR 0004. */
sealed interface WeeklyPopulationRecordResult {
    data class Ranked(val ranking: WeeklyRankingSnapshot) : WeeklyPopulationRecordResult

    data object Duplicate : WeeklyPopulationRecordResult
}

/**
 * Persistence/population boundary for competitive Weekly ranking.
 *
 * Implementations must derive ranking only from server-accepted runs and deduplicate the population by
 * authenticated participant for a challenge. ADR 0004 defines V1 as first accepted attempt wins:
 * a successful first insert returns [WeeklyPopulationRecordResult.Ranked], while a later accepted run
 * for the same principal/challenge returns [WeeklyPopulationRecordResult.Duplicate] without changing
 * the accepted population.
 *
 * The ranked snapshot must describe [run] exactly. Storage adapters should perform the successful first
 * insert plus population measurement atomically enough that rank/participantCount describe one coherent
 * accepted-population snapshot.
 */
interface WeeklyRankingPopulationStore {
    suspend fun recordAndRank(
        principal: WeeklyAuthenticatedPrincipal,
        run: AcceptedWeeklyRun,
    ): WeeklyPopulationRecordResult
}

/**
 * Server-side application service between authenticated transport and ranking persistence.
 *
 * Replay validation remains the security boundary for the score itself. Authentication supplies the
 * participant identity used by the population store. Duplicate accepted attempts are REJECTED under
 * ADR 0004. Persistence failures are UNAVAILABLE; malformed/forged submissions are REJECTED.
 * Structured-concurrency cancellation always propagates.
 */
class WeeklyRankingService(
    private val validator: WeeklySubmissionValidator,
    private val populationStore: WeeklyRankingPopulationStore,
) {
    suspend fun submit(
        principal: WeeklyAuthenticatedPrincipal,
        payload: String,
    ): WeeklyRankingResult {
        return when (val validation = validator.validate(payload)) {
            is WeeklySubmissionValidation.Rejected -> WeeklyRankingResult.rejected()
            is WeeklySubmissionValidation.Accepted -> rankAccepted(principal, validation.run)
        }
    }

    private suspend fun rankAccepted(
        principal: WeeklyAuthenticatedPrincipal,
        run: AcceptedWeeklyRun,
    ): WeeklyRankingResult {
        val populationResult = try {
            populationStore.recordAndRank(principal, run)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return WeeklyRankingResult.unavailable()
        }

        val ranking = when (populationResult) {
            WeeklyPopulationRecordResult.Duplicate -> return WeeklyRankingResult.rejected()
            is WeeklyPopulationRecordResult.Ranked -> populationResult.ranking
        }

        // A storage/adapter bug must never let ranking data for another run cross the trust boundary.
        if (ranking.challengeId != run.challengeId || ranking.score != run.score) {
            return WeeklyRankingResult.unavailable()
        }

        return WeeklyRankingResult.ranked(ranking)
    }
}
