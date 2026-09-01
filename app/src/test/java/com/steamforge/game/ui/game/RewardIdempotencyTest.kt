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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RewardIdempotencyTest {
    private val dispatcher = StandardTestDispatcher()
    private val cfg = ProgressionConfig()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private class NoAnalytics : Analytics {
        override fun logEvent(name: String, params: Map<String, Any?>) = Unit
    }

    private fun record(id: String, xp: Int, claimed: Boolean = false) = FinishedGameRecord(
        id = id,
        day = LocalDay.todayEpochDay(),
        daily = false,
        score = 1500,
        maxTileLevel = 7,
        xpGained = xp,
        state = GameSaveCodec.encode(
            SavedGame(GameState(), seed = 1L, pressure = 0, overdriveRemaining = 0, freeUndosLeft = 0),
        ),
        rewardedClaimed = claimed,
    )

    private fun vm(repo: FakeDataRepo) = GameViewModel(
        repo = repo,
        analytics = NoAnalytics(),
        cfg = cfg,
        seedProvider = { 42L },
        savedGameProvider = { repo.currentGame },
    )

    @Test
    fun `repeated rewarded callback grants workshop xp only once`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialFinished = record("fg-1", 60))
        val model = vm(repo)
        advanceUntilIdle()

        model.grantDoubleReward()
        advanceUntilIdle()
        assertEquals(60, repo.currentProgress.totalXp)
        assertEquals(60, model.ui.value.rewardedBonus?.xpGained)

        model.grantDoubleReward()
        advanceUntilIdle()
        assertEquals(60, repo.currentProgress.totalXp)
    }

    @Test
    fun `process recreation restores rewarded bonus without regrant`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialFinished = record("fg-1", 60))
        val first = vm(repo)
        advanceUntilIdle()
        first.grantDoubleReward()
        advanceUntilIdle()

        val restored = vm(repo)
        advanceUntilIdle()
        assertTrue(restored.ui.value.rewardDoubled)
        assertEquals(60, restored.ui.value.rewardedBonus?.xpGained)
        restored.grantDoubleReward()
        advanceUntilIdle()
        assertEquals(60, repo.currentProgress.totalXp)
    }

    @Test
    fun `repository derives rewarded amount from persisted run xp`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialFinished = record("fg-1", 200))
        val bonus = repo.claimDoubleReward("fg-1", cfg)

        assertEquals(200, bonus?.xpGained)
        assertEquals(200, repo.currentProgress.totalXp)
        assertEquals(listOf(2), bonus?.levelUps)
        assertEquals(cfg.levelUpGems(2), repo.currentProgress.gems)
        assertNull(repo.claimDoubleReward("fg-1", cfg))
    }

    @Test
    fun `claim rejects mismatched id and already claimed record`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialFinished = record("fg-1", 100))
        assertNull(repo.claimDoubleReward("other", cfg))
        assertEquals(100, repo.claimDoubleReward("fg-1", cfg)?.xpGained)
        assertNull(repo.claimDoubleReward("fg-1", cfg))

        repo.currentFinished = record("fg-2", 100, claimed = true)
        assertNull(repo.claimDoubleReward("fg-2", cfg))
    }

    @Test
    fun `exit after finish discards result record`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialFinished = record("fg-1", 60))
        val model = vm(repo)
        advanceUntilIdle()
        model.exit()
        advanceUntilIdle()
        assertEquals(null, repo.currentFinished)
    }
}
