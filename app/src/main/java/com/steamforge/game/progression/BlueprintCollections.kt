package com.steamforge.game.progression

/** A finite collectible part that belongs to one blueprint collection. */
data class BlueprintPieceDef(
    val id: String,
    val title: String,
)

data class BlueprintCollectionDef(
    val id: String,
    val title: String,
    val pieces: List<BlueprintPieceDef>,
) {
    val pieceIds: Set<String> = pieces.map { it.id }.toSet()
}

/**
 * Finite blueprint catalog. Persisted ownership remains PlayerProgress.blueprintPieces;
 * completion is derived from stable piece ids, so no extra DataStore schema is required.
 */
object BlueprintCollections {
    const val STEAM_ENGINE_ID = "steam_engine"

    val steamEngine = BlueprintCollectionDef(
        id = STEAM_ENGINE_ID,
        title = "STEAM ENGINE",
        pieces = listOf(
            BlueprintPieceDef("steam_engine_boiler", "Котёл"),
            BlueprintPieceDef("steam_engine_piston", "Поршень"),
            BlueprintPieceDef("steam_engine_valve", "Клапан"),
            BlueprintPieceDef("steam_engine_flywheel", "Маховик"),
            BlueprintPieceDef("steam_engine_regulator", "Регулятор"),
            BlueprintPieceDef("steam_engine_pressure_gauge", "Манометр"),
        ),
    )

    val all: List<BlueprintCollectionDef> = listOf(steamEngine)

    fun collection(id: String): BlueprintCollectionDef? = all.firstOrNull { it.id == id }

    fun ownedCount(collection: BlueprintCollectionDef, ownedIds: Set<String>): Int =
        collection.pieces.count { it.id in ownedIds }

    fun isComplete(collection: BlueprintCollectionDef, ownedIds: Set<String>): Boolean =
        collection.pieces.isNotEmpty() && ownedCount(collection, ownedIds) == collection.pieces.size

    fun isSteamEngineComplete(ownedIds: Set<String>): Boolean = isComplete(steamEngine, ownedIds)

    /**
     * Chooses the next not-yet-owned piece in catalog order. This avoids duplicate rewards while the
     * collection is incomplete and keeps reward resolution deterministic for a given profile state.
     */
    fun nextMissingPiece(collectionId: String, ownedIds: Set<String>): BlueprintPieceDef? =
        collection(collectionId)?.pieces?.firstOrNull { it.id !in ownedIds }
}
