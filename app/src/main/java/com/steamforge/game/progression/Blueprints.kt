package com.steamforge.game.progression

data class BlueprintPieceDef(
    val id: String,
    val title: String,
    val technicalLabel: String,
)

data class BlueprintSetDef(
    val id: String,
    val title: String,
    val description: String,
    val pieces: List<BlueprintPieceDef>,
    val workshopUnlockId: String,
)

object Blueprints {
    const val STEAM_ENGINE_UNLOCK = "steam_engine"

    val steamEngine = BlueprintSetDef(
        id = "steam_engine_v1",
        title = "STEAM ENGINE",
        description = "Базовый силовой модуль мастерской",
        workshopUnlockId = STEAM_ENGINE_UNLOCK,
        pieces = listOf(
            BlueprintPieceDef("steam_engine_boiler", "Boiler", "PRESSURE VESSEL A-01"),
            BlueprintPieceDef("steam_engine_piston", "Piston", "DRIVE PISTON B-04"),
            BlueprintPieceDef("steam_engine_valve", "Valve", "FLOW VALVE C-02"),
            BlueprintPieceDef("steam_engine_flywheel", "Flywheel", "FLYWHEEL D-07"),
            BlueprintPieceDef("steam_engine_regulator", "Regulator", "REGULATOR E-03"),
            BlueprintPieceDef("steam_engine_gauge", "Pressure Gauge", "GAUGE F-11"),
        ),
    )

    val sets: List<BlueprintSetDef> = listOf(steamEngine)

    fun collectedCount(set: BlueprintSetDef, owned: Set<String>): Int = set.pieces.count { it.id in owned }

    fun isComplete(set: BlueprintSetDef, owned: Set<String>): Boolean = collectedCount(set, owned) == set.pieces.size

    /** Выдаёт отсутствующую деталь детерминированно, но без дублей. */
    fun nextMissingPiece(set: BlueprintSetDef, owned: Set<String>, seed: Long): BlueprintPieceDef? {
        val missing = set.pieces.filterNot { it.id in owned }
        if (missing.isEmpty()) return null
        val mixed = (seed xor (seed ushr 32)).toInt()
        return missing[Math.floorMod(mixed, missing.size)]
    }

    fun workshopUnlocks(owned: Set<String>): Set<String> = buildSet {
        sets.forEach { set ->
            if (isComplete(set, owned)) add(set.workshopUnlockId)
        }
    }
}
