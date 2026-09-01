package com.steamforge.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.steamforge.game.progression.Onboarding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "steamforge_onboarding")

/**
 * App-shell state intentionally lives outside PlayerProgress/economy. Resetting game progress therefore
 * does not force an experienced player through onboarding again.
 */
class OnboardingStore(
    context: Context,
    private val repo: DataRepo,
) {
    private object Keys {
        val step = intPreferencesKey("onboarding_step")
    }

    private val store = context.applicationContext.onboardingDataStore

    /** null means the one-time fresh-vs-legacy decision has not been persisted yet. */
    val step: Flow<Int?> = store.data.map { prefs -> prefs[Keys.step]?.let(Onboarding::normalize) }

    suspend fun ensureInitialized() {
        if (store.data.first()[Keys.step] != null) return
        val progress = repo.progress.first()
        val hasSavedGame = repo.savedGame.first() != null
        val initial = Onboarding.resolveInitialStep(
            storedStep = null,
            hasLegacyProgress = Onboarding.hasLegacyProgress(progress, hasSavedGame),
        )
        store.edit { prefs ->
            if (prefs[Keys.step] == null) prefs[Keys.step] = initial
        }
    }

    suspend fun setStep(step: Int) {
        store.edit { it[Keys.step] = Onboarding.normalize(step) }
    }
}
