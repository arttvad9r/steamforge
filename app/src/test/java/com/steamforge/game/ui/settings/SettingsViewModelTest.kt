package com.steamforge.game.ui.settings

import android.app.Activity
import android.content.Intent
import com.steamforge.game.billing.BillingProvider
import com.steamforge.game.billing.CosmeticProduct
import com.steamforge.game.billing.CosmeticsBillingState
import com.steamforge.game.billing.RemoveAdsState
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.PlayerProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private class FakeBilling(initial: RemoveAdsState) : BillingProvider {
        override val removeAds = MutableStateFlow(initial)
        override val cosmetics = MutableStateFlow(CosmeticsBillingState())
        var refreshes = 0
        override fun refresh() { refreshes++ }
        override fun purchaseRemoveAds(activity: Activity) = Unit
        override fun purchaseCosmetic(activity: Activity, product: CosmeticProduct) = Unit
        override fun proceedIntent(intent: Intent?) = Unit
    }

    @Test
    fun `remove ads state comes from billing provider`() = runTest(dispatcher) {
        val billing = FakeBilling(
            RemoveAdsState(
                configured = true,
                productAvailable = true,
                priceLabel = "149 ₽",
            ),
        )
        val model = SettingsViewModel(FakeDataRepo(), billing)
        advanceUntilIdle()

        assertTrue(model.ui.value.removeAdsConfigured)
        assertTrue(model.ui.value.removeAdsProductAvailable)
        assertEquals("149 ₽", model.ui.value.removeAdsPriceLabel)

        billing.removeAds.value = billing.removeAds.value.copy(owned = true)
        advanceUntilIdle()
        assertTrue(model.ui.value.removeAdsOwned)
    }

    @Test
    fun `reset progress does not revoke billing entitlement`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialProgress = PlayerProgress(gems = 99, totalXp = 500))
        val billing = FakeBilling(RemoveAdsState(configured = true, owned = true))
        val model = SettingsViewModel(repo, billing)
        advanceUntilIdle()

        model.resetProgress()
        advanceUntilIdle()

        assertTrue(model.ui.value.removeAdsOwned)
        assertEquals(0, repo.currentProgress.gems)
        assertEquals(0, repo.currentProgress.totalXp)
        assertFalse(model.ui.value.removeAdsPurchaseInProgress)
    }
}
