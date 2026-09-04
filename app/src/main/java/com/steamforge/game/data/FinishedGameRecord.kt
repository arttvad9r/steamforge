package com.steamforge.game.data

import com.steamforge.game.progression.FinishEffects
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Запись о завершённой партии. Персистится до выхода с экрана игры и служит
 * фундаментом идемпотентности rewarded-награды: [id] уникален, [rewardedClaimed]
 * изменяется только атомарно в репозитории.
 */
@Serializable
data class FinishedGameRecord(
    val id: String,
    /** epochDay завершения: overlay восстанавливается только в тот же день. */
    val day: Long,
    val daily: Boolean,
    val score: Int,
    val maxTileLevel: Int,
    val newBest: Boolean = false,
    val xpGained: Int = 0,
    val gemsGained: Int = 0,
    val workshopPartsGained: Int = 0,
    val levelUps: List<Int> = emptyList(),
    val newAchievementIds: List<String> = emptyList(),
    /** Финальная доска (GameSaveCodec) для восстановления overlay после process death. */
    val state: String,
    val rewardedClaimed: Boolean = false,
)

/** Заполняет поля эффектов записи результатом атомарной транзакции завершения. */
internal fun FinishedGameRecord.withEffects(effects: FinishEffects): FinishedGameRecord = copy(
    newBest = effects.newBest,
    xpGained = effects.xpGained,
    gemsGained = effects.gemsGained,
    workshopPartsGained = effects.workshopPartsGained,
    levelUps = effects.levelUps,
    newAchievementIds = effects.newAchievements.map { it.id },
)

/** Json-кодирование записи для Preferences DataStore. */
object FinishedGameCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(record: FinishedGameRecord): String = json.encodeToString(record)

    fun decode(raw: String): FinishedGameRecord? =
        runCatching { json.decodeFromString<FinishedGameRecord>(raw) }.getOrNull()
}
