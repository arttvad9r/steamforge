package com.steamforge.game.ui.game

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.core.GameState
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.data.FinishedGameRecord
import com.steamforge.game.data.GameSaveCodec
import com.steamforge.game.data.SavedGame
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.ProgressionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RewardIdempotencyTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class NoAnalytics : Analytics {
        override fun logEvent(name: String, params: Map<String, Any?>) = Unit
    }

    private fun record(
        id: String,
        gems: Int,
        claimed: Boolean = false,
        day: Long = LocalDay.todayEpochDay(),
        daily: Boolean = false,
    ) = FinishedGameRecord(
        id = id,
        day = day,
        daily = daily,
        score = 1500,
        maxTileLevel = 7,
        gemsGained = gems,
        state = GameSaveCodec.encode(
            SavedGame(GameState(), seed = 1L, pressure = 0, overdriveRemaining = 0, freeUndosLeft = 0),
        ),
        rewardedClaimed = claimed,
    )

    private fun vm(repo: FakeDataRepo): GameViewModel = GameViewModel(
        repo = repo,
        analytics = NoAnalytics(),
        cfg = ProgressionConfig(),
        seedProvider = { 42L },
        savedGameProvider = { repo.currentGame },
    )

    private fun gems(repo: FakeDataRepo) = repo.currentProgress.gems

    @Test
    fun `repeated rewarded callback grants doubled gems only once`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialFinished = record("fg-1", gems = 12))
        val model = vm(repo)
        advanceUntilIdle()
        assertTrue(model.ui.value.finished)
        assertEquals("fg-1", model.ui.value.gameResultId)

        model.grantDoubleReward()
        advanceUntilIdle()
        assertEquals(12, gems(repo))
        assertTrue(model.ui.value.rewardDoubled)

        model.grantDoubleReward()
        advanceUntilIdle()
        assertEquals(12, gems(repo))
    }

    @Test
    fun `process recreation does not regrant claimed reward`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialFinished = record("fg-1", gems = 12))
        val first = vm(repo)
        advanceUntilIdle()
        first.grantDoubleReward()
        advanceUntilIdle()

        val second = vm(repo)
        advanceUntilIdle()
        assertTrue(second.ui.value.finished)
        assertTrue(second.ui.value.rewardDoubled)
        second.grantDoubleReward()
        advanceUntilIdle()
        assertEquals(12, gems(repo))
    }

    @Test
    fun `two different finished games each reward exactly once`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialFinished = record("fg-1", gems = 10))
        val first = vm(repo)
        advanceUntilIdle()
        first.grantDoubleReward()
        advanceUntilIdle()
        assertEquals(10, gems(repo))

        repo.currentFinished = record("fg-2", gems = 25)
        val second = vm(repo)
        advanceUntilIdle()
        second.grantDoubleReward()
        advanceUntilIdle()
        assertEquals(35, gems(repo))
        second.grantDoubleReward()
        advanceUntilIdle()
        assertEquals(35, gems(repo))
    }

    @Test
    fun `claim is refused for foreign id or claimed record`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialFinished = record("fg-1", gems = 10, claimed = true))
        val model = vm(repo)
        advanceUntilIdle()
        assertTrue(model.ui.value.rewardDoubled)
        model.grantDoubleReward()
        advanceUntilIdle()
        assertEquals(0, gems(repo))

        repo.currentFinished = record("fg-2", gems = 10)
        assertFalse(repo.claimDoubleReward("fg-OTHER", 10))
        assertTrue(repo.claimDoubleReward("fg-2", 10))
        assertFalse(repo.claimDoubleReward("fg-2", 10))
        assertEquals(10, gems(repo))
    }

    @Test
    fun `exit after finish discards result record`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialFinished = record("fg-1", gems = 12))
        val model = vm(repo)
        advanceUntilIdle()
        model.exit()
        advanceUntilIdle()
        assertEquals(null, repo.currentFinished)
    }
}
