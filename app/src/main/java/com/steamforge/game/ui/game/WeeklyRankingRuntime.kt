package com.steamforge.game.ui.game

import com.steamforge.game.progression.WeeklyRankingProvider
import com.steamforge.game.progression.WeeklyRankingResult
import com.steamforge.game.progression.WeeklyRunSubmission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WeeklyRankingRuntimeState(
    val submitting: Boolean = false,
    val result: WeeklyRankingResult? = null,
)

/**
 * Owns the asynchronous Weekly ranking request independently of gameplay state.
 *
 * A generation guard complements Job cancellation so even a non-cooperative provider that swallows
 * cancellation cannot publish a late response into a restarted/new Weekly attempt.
 */
internal class WeeklyRankingRuntime(
    private val scope: CoroutineScope,
    private val provider: WeeklyRankingProvider,
) {
    private var job: Job? = null
    private var generation: Long = 0L

    private val _state = MutableStateFlow(WeeklyRankingRuntimeState())
    val state: StateFlow<WeeklyRankingRuntimeState> = _state.asStateFlow()

    fun reset() {
        generation++
        job?.cancel()
        job = null
        _state.value = WeeklyRankingRuntimeState()
    }

    fun submit(submission: WeeklyRunSubmission) {
        job?.cancel()
        val requestGeneration = ++generation
        _state.value = WeeklyRankingRuntimeState(submitting = true)
        job = scope.launch {
            val result = try {
                provider.submit(submission).verifiedFor(submission)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                WeeklyRankingResult.unavailable()
            }

            if (requestGeneration == generation) {
                _state.value = WeeklyRankingRuntimeState(result = result)
            }
        }
    }
}
