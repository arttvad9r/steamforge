package com.steamforge.game.ui.workshop

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.analytics.AnalyticsEvents
import com.steamforge.game.config.RemoteConfigProvider
import com.steamforge.game.config.RemoteConfigRefreshResult
import com.steamforge.game.config.RemoteConfigSnapshot
import com.steamforge.game.config.RemoteConfigSource
import com.steamforge.game.config.RemoteGameConfig
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.PlayerProgress
import com.steamforge.game.progression.ProgressionConfig
import com.steamforge.game.progression.WorkshopMechanism
import com.steamforge.game.progression.WorkshopProgression
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkshopAnalyticsTest {
    private val dispatcher = StandardTestDispatcher()

    private class RecordingAnalytics : Analytics {
        val events = mutableListOf<Pair<String, Map<String, Any?>>>()
        override fun logEvent(name: String, params: Map<String, Any?>) {
            events += name to params
        }
    }

    private class MutableRemoteConfigProvider(config: RemoteGameConfig) : RemoteConfigProvider {
        private val state = MutableStateFlow(snapshotFor(config, "test-1"))
        override val snapshot: StateFlow<RemoteConfigSnapshot> = state

        fun update(config: RemoteGameConfig) {
            state.value = snapshotFor(config, "test-2")
        }

        override suspend fun refresh(): RemoteConfigRefreshResult = RemoteConfigRefreshResult.UPDATED

        private fun snapshotFor(config: RemoteGameConfig, revision: String) = RemoteConfigSnapshot(
            config = config.sanitized(),
            source = RemoteConfigSource.REMOTE,
            revision = revision,
        )
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `successful persisted upgrade emits workshop and economy events`() = runTest(dispatcher) {
        val cfg = ProgressionConfig()
        val cost = WorkshopProgression.mechanismUpgradeCost(0, cfg)!!
        val remaining = 100
        val repo = FakeDataRepo(initialProgress = PlayerProgress(workshopParts = cost + remaining))
        val analytics = RecordingAnalytics()
        val vm = WorkshopViewModel(repo = repo, cfg = cfg, analytics = analytics)

        vm.upgradeMechanism(WorkshopMechanism.CORE)
        advanceUntilIdle()

        assertEquals(1, repo.currentProgress.workshopCoreStage)
        val upgradeEvents = analytics.events.filter { it.first == AnalyticsEvents.WORKSHOP_UPGRADE }
        assertEquals(1, upgradeEvents.size)
        val upgradeParams = upgradeEvents.single().second
        assertEquals("CORE", upgradeParams["mechanism"])
        assertEquals(0, upgradeParams["from_stage"])
        assertEquals(1, upgradeParams["to_stage"])
        assertEquals(cost, upgradeParams["parts_spent"])

        val economyEvents = analytics.events.filter { it.first == AnalyticsEvents.RESOURCE_SPENT }
        assertEquals(1, economyEvents.size)
        val economyParams = economyEvents.single().second
        assertEquals("workshop_parts", economyParams["resource_type"])
        assertEquals("workshop_upgrade", economyParams["source"])
        assertEquals(cost, economyParams["amount"])
        assertEquals(remaining, economyParams["balance_after"])
    }

    @Test
    fun `runtime remote config cost is used by persisted upgrade and analytics`() = runTest(dispatcher) {
        val remoteCost = 7
        val remaining = 11
        val provider = MutableRemoteConfigProvider(
            RemoteGameConfig(workshopUpgradeCosts = listOf(remoteCost, 15, 30, 60)),
        )
        val repo = FakeDataRepo(
            initialProgress = PlayerProgress(workshopParts = remoteCost + remaining),
        )
        val analytics = RecordingAnalytics()
        val vm = WorkshopViewModel(
            repo = repo,
            remoteConfigProvider = provider,
            analytics = analytics,
        )

        vm.upgradeMechanism(WorkshopMechanism.CORE)
        advanceUntilIdle()

        assertEquals(1, repo.currentProgress.workshopCoreStage)
        assertEquals(remaining, repo.currentProgress.workshopParts)

        val upgradeParams = analytics.events
            .single { it.first == AnalyticsEvents.WORKSHOP_UPGRADE }
            .second
        assertEquals(remoteCost, upgradeParams["parts_spent"])

        val economyParams = analytics.events
            .single { it.first == AnalyticsEvents.RESOURCE_SPENT }
            .second
        assertEquals(remoteCost, economyParams["amount"])
        assertEquals(remaining, economyParams["balance_after"])
    }

    @Test
    fun `workshop ui reacts to remote config snapshot updates`() = runTest(dispatcher) {
        val provider = MutableRemoteConfigProvider(
            RemoteGameConfig(workshopUpgradeCosts = listOf(7, 15, 30, 60)),
        )
        val repo = FakeDataRepo(initialProgress = PlayerProgress(workshopParts = 100))
        val vm = WorkshopViewModel(repo = repo, remoteConfigProvider = provider)
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()

        assertEquals(
            7,
            vm.ui.value.mechanisms.first { it.mechanism == WorkshopMechanism.CORE }.nextCost,
        )

        provider.update(RemoteGameConfig(workshopUpgradeCosts = listOf(9, 18, 36, 72)))
        advanceUntilIdle()

        assertEquals(
            9,
            vm.ui.value.mechanisms.first { it.mechanism == WorkshopMechanism.CORE }.nextCost,
        )
    }

    @Test
    fun `daily reward emits workshop parts economy event once`() = runTest(dispatcher) {
        val day = 1000L
        val initialParts = 9
        val repo = FakeDataRepo(initialProgress = PlayerProgress(workshopParts = initialParts))
        val analytics = RecordingAnalytics()
        val vm = WorkshopViewModel(repo = repo, today = { day }, analytics = analytics)
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()
        val expectedParts = vm.ui.value.dailyRewardWorkshopParts

        vm.claimDailyReward()
        advanceUntilIdle()
        vm.claimDailyReward()
        advanceUntilIdle()

        assertEquals(initialParts + expectedParts, repo.currentProgress.workshopParts)
        val events = analytics.events.filter { it.first == AnalyticsEvents.RESOURCE_EARNED }
        assertEquals(1, events.size)
        val params = events.single().second
        assertEquals("workshop_parts", params["resource_type"])
        assertEquals("daily_reward", params["source"])
        assertEquals(expectedParts, params["amount"])
        assertEquals(initialParts + expectedParts, params["balance_after"])
    }

    @Test
    fun `rejected upgrade does not emit workshop or economy event`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialProgress = PlayerProgress(workshopParts = 0))
        val analytics = RecordingAnalytics()
        val vm = WorkshopViewModel(repo = repo, analytics = analytics)

        vm.upgradeMechanism(WorkshopMechanism.CORE)
        advanceUntilIdle()

        assertEquals(0, repo.currentProgress.workshopCoreStage)
        assertTrue(analytics.events.none { it.first == AnalyticsEvents.WORKSHOP_UPGRADE })
        assertTrue(analytics.events.none { it.first == AnalyticsEvents.RESOURCE_SPENT })
    }
}
