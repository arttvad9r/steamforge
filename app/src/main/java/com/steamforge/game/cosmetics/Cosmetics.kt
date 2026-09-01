package com.steamforge.game.cosmetics

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object CosmeticCatalog {
    const val TILE_CLASSIC = "tile_classic"
    const val TILE_PATINA = "tile_patina"
    const val WORKSHOP_CLASSIC = "workshop_classic"
    const val WORKSHOP_FOUNDRY = "workshop_foundry"
}

data class CosmeticLoadout(
    val tileSet: String = CosmeticCatalog.TILE_CLASSIC,
    val workshopTheme: String = CosmeticCatalog.WORKSHOP_CLASSIC,
)

private val Context.cosmeticDataStore by preferencesDataStore(name = "steamforge_cosmetics")

/** Equipped cosmetic choices are presentation preferences, independent from game progress and billing ownership. */
class CosmeticLoadoutStore(context: Context) {
    private val appContext = context.applicationContext

    val loadout: Flow<CosmeticLoadout> = appContext.cosmeticDataStore.data
        .map { prefs ->
            CosmeticLoadout(
                tileSet = prefs[TILE_SET] ?: CosmeticCatalog.TILE_CLASSIC,
                workshopTheme = prefs[WORKSHOP_THEME] ?: CosmeticCatalog.WORKSHOP_CLASSIC,
            )
        }
        .distinctUntilChanged()

    suspend fun equipTileSet(id: String) {
        require(id == CosmeticCatalog.TILE_CLASSIC || id == CosmeticCatalog.TILE_PATINA)
        appContext.cosmeticDataStore.edit { it[TILE_SET] = id }
    }

    suspend fun equipWorkshopTheme(id: String) {
        require(id == CosmeticCatalog.WORKSHOP_CLASSIC || id == CosmeticCatalog.WORKSHOP_FOUNDRY)
        appContext.cosmeticDataStore.edit { it[WORKSHOP_THEME] = id }
    }

    private companion object {
        val TILE_SET = stringPreferencesKey("equipped_tile_set")
        val WORKSHOP_THEME = stringPreferencesKey("equipped_workshop_theme")
    }
}
