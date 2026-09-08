package com.steamforge.game.ui.settings

import com.steamforge.game.data.DataRepo
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.PlayerProgress
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
class SettingsPersistenceIoTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun CoroutineScope.subscribe(ui: StateFlow<*>) = launch { ui.collect {} }

    private class FailingSettingsRepo(
        val delegate: FakeDataRepo,
    ) : DataRepo by delegate {
        override suspend fun updateProgress(block: (PlayerProgress) -> PlayerProgress) {
            throw IOException("ENOSPC update")
        }

        override suspend fun resetGameProgress() {
            throw IOException("ENOSPC reset")
        }
    }

    @Test
    fun `settings toggle keeps current state when persistence write fails`() = runTest(dispatcher) {
        val repo = FailingSettingsRepo(FakeDataRepo(PlayerProgress(soundEnabled = true)))
        val vm = SettingsViewModel(repo)
        backgroundScope.subscribe(vm.ui)
        advanceUntilIdle()

        vm.setSound(false)
        advanceUntilIdle()

        assertTrue(vm.ui.value.soundEnabled)
    }

    @Test
    fun `reset keeps current progress visible when persistence write fails`() = runTest(dispatcher) {
        val initial = PlayerProgress(
            gems = 37,
            soundEnabled = false,
            hapticsEnabled = false,
            animationsEnabled = true,
        )
        val repo = FailingSettingsRepo(FakeDataRepo(initial))
        val vm = SettingsViewModel(repo)
        backgroundScope.subscribe(vm.ui)
        advanceUntilIdle()

        assertFalse(vm.ui.value.soundEnabled)
        vm.resetProgress()
        advanceUntilIdle()

        assertEquals(37, repo.delegate.currentProgress.gems)
        assertFalse(vm.ui.value.soundEnabled)
    }
}
