package com.steamforge.game.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.progression.WeeklyRankingProvider
import com.steamforge.game.progression.WeeklyRunSubmission

class WeeklyRankingViewModel(
    provider: WeeklyRankingProvider,
) : ViewModel() {
    private val runtime = WeeklyRankingRuntime(viewModelScope, provider)
    private var lastSubmission: WeeklyRunSubmission? = null

    val state = runtime.state

    fun onSubmission(submission: WeeklyRunSubmission?) {
        if (submission == null) {
            lastSubmission = null
            runtime.reset()
            return
        }
        if (submission == lastSubmission) return

        lastSubmission = submission
        runtime.submit(submission)
    }
}
