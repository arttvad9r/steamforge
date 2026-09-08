package com.steamforge.game.ui.game

import com.steamforge.game.core.Move
import com.steamforge.game.data.DataRepo
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.DailyChallenge
import com.steamforge.game.progression.DailyGoalType
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.PlayerProgress
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DailyChallengeClaimRetryTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FlakyDailyRepo(
        private val delegate: FakeDataRepo,
        private val commitBeforeFailure: Boolean,
    ) : DataRepo by delegate {
        var attempts = 0
        var progressAfterAmbiguousCommit: PlayerProgress? = null

        val currentProgress: PlayerProgress
            get() = delegate.currentProgress

        override suspend fun claimDailyChallenge(day: Long, rewardGems: Int, bonusXp: Int): Boolean {
            attempts++
            if (attempts == 1) {
                if (commitBeforeFailure) {
                    delegate.claimDailyChallenge(day, rewardGems, bonusXp)
                    progressAfterAmbiguousCommit = delegate.currentProgress
                }
                throw IOException("ENOSPC")
            }
            return delegate.claimDailyChallenge(day, rewardGems, bonusXp)
        }
    }

    private fun challenge(): DailyChallenge = DailyChallenge(
        epochDay = LocalDay.todayEpochDay(),
        type = DailyGoalType.REACH_SCORE,
        target = 0,
        mergeLevel = 6,
        seed = 44_221L,
        rewardGems = 15,
        bonusXp = 60,
    )

    private fun model(repo: DataRepo, challenge: DailyChallenge) = GameViewModel(
        repo = repo,
        dailyMode = true,
        dailyProvider = { challenge },
        seedProvider = { challenge.seed },
    )

    private suspend fun TestScope.triggerSatisfiedMove(model: GameViewModel, repo: FlakyDailyRepo) {
        for (move in listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)) {
            model.onMove(move)
            advanceUntilIdle()
            if (repo.attempts > 0) return
        }
        error("No accepted move triggered the daily challenge")
    }

    @Test
    fun `transient daily claim io retries without another move`() = runTest(dispatcher) {
        val challenge = challenge()
        val repo = FlakyDailyRepo(FakeDataRepo(), commitBeforeFailure = false)
        val model = model(repo, challenge)
        advanceUntilIdle()

        triggerSatisfiedMove(model, repo)

        assertEquals(2, repo.attempts)
        assertTrue(model.ui.value.dailySatisfied)
        assertEquals(challenge.epochDay, repo.currentProgress.dailyChallengeDay)
        assertTrue(repo.currentProgress.dailyChallengeDone)
        assertEquals(1, repo.currentProgress.stats.dailyCompleted)
        assertEquals(challenge.bonusXp, repo.currentProgress.totalXp)
        assertTrue(repo.currentProgress.gems >= challenge.rewardGems)
    }

    @Test
    fun `ambiguous committed daily claim retry stays idempotent`() = runTest(dispatcher) {
        val challenge = challenge()
        val repo = FlakyDailyRepo(FakeDataRepo(), commitBeforeFailure = true)
        val model = model(repo, challenge)
        advanceUntilIdle()

        triggerSatisfiedMove(model, repo)

        assertEquals(2, repo.attempts)
        assertTrue(model.ui.value.dailySatisfied)
        assertNotNull(repo.progressAfterAmbiguousCommit)
        assertEquals(repo.progressAfterAmbiguousCommit, repo.currentProgress)
        assertEquals(1, repo.currentProgress.stats.dailyCompleted)
    }
}
