package com.steamforge.game.ui.game

import com.steamforge.game.progression.WeeklyRankingProvider
import com.steamforge.game.progression.WeeklyRankingResult
import com.steamforge.game.progression.WeeklyRunReplay
import com.steamforge.game.progression.WeeklyRunSubmission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyRankingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `same submission is not resent until reset`() = runTest(dispatcher) {
        var calls = 0
        val provider = object : WeeklyRankingProvider {
            override suspend fun submit(submission: WeeklyRunSubmission): WeeklyRankingResult {
                calls++
                return WeeklyRankingResult.unavailable()
            }
        }
        val model = WeeklyRankingViewModel(provider)
        val submission = WeeklyRunSubmission(
            protocolVersion = WeeklyRunReplay.PROTOCOL_VERSION,
            challengeId = "weekly-20000",
            seed = 42L,
            moveSequence = "LURD",
            finalScore = 128,
            finalMaxTileLevel = 5,
        )

        model.onSubmission(submission)
        model.onSubmission(submission)
        advanceUntilIdle()
        assertEquals(1, calls)

        model.onSubmission(null)
        model.onSubmission(submission)
        advanceUntilIdle()
        assertEquals(2, calls)
    }
}
