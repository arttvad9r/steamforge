package com.steamforge.game.progression

/**
 * Определения достижений. Логика не связана с UI: evaluate — чистая функция над статистикой.
 */
data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val gemReward: Int,
    val maxProgress: Int = 1,
    val hidden: Boolean = false,
    val progressOf: (PlayerStats) -> Int,
)

object Achievements {

    private fun tile(level: Int, gems: Int) = AchievementDef(
        id = "tile_${1 shl level}",
        title = "Собран ${1 shl level}",
        description = "Собери «${com.steamforge.game.core.Elements.name(level)}» (${1 shl level})",
        gemReward = gems,
        progressOf = { if (it.maxTileLevel >= level) 1 else 0 },
    )

    val all: List<AchievementDef> = listOf(
        AchievementDef("merge_1", "Первый стык", "Выполни первое объединение", 3) { minOf(it.totalMerges, 1) },
        tile(6, 3),
        tile(7, 5),
        tile(8, 5),
        tile(9, 8),
        tile(10, 12),
        tile(11, 25),
        AchievementDef("score_1000", "Разгон", "Набери 1000 очков за партию", 3) { if (it.bestScore >= 1000) 1 else 0 },
        AchievementDef("score_5000", "Инженер", "Набери 5000 очков за партию", 8) { if (it.bestScore >= 5000) 1 else 0 },
        AchievementDef("score_20000", "Главный механик", "Набери 20000 очков за партию", 20) { if (it.bestScore >= 20000) 1 else 0 },
        AchievementDef("games_10", "Смена начата", "Сыграй 10 партий", 5, maxProgress = 10) { minOf(it.gamesPlayed, 10) },
        AchievementDef("games_50", "Ветеран цеха", "Сыграй 50 партий", 15, maxProgress = 50) { minOf(it.gamesPlayed, 50) },
        AchievementDef("overdrive_5", "Перегрузка", "Активируй Overdrive 5 раз", 8, maxProgress = 5) { minOf(it.overdrives, 5) },
        AchievementDef("combo_3", "Двойной удар", "Сделай 3 объединения за один ход", 5) { if (it.maxMergesInOneMove >= 3) 1 else 0 },
        AchievementDef("undo_10", "Мастер на все руки", "Используй отмену хода 10 раз", 5, maxProgress = 10) { minOf(it.undos, 10) },
        AchievementDef("daily_1", "Наряд на день", "Выполни ежедневное испытание", 10) { if (it.dailyCompleted >= 1) 1 else 0 },
        AchievementDef("daily_7", "Неделя у станка", "Выполни 7 ежедневных испытаний", 30, maxProgress = 7) { minOf(it.dailyCompleted, 7) },
        AchievementDef("gems_500", "Полный чулан", "Заработай 500 гемов суммарно", 25, hidden = true) { if (it.gemsEarned >= 500) 1 else 0 },
    )

    private val byId = all.associateBy { it.id }

    fun byId(id: String): AchievementDef? = byId[id]

    /** Достижения, условия которых выполнены, но ещё не разблокированы. */
    fun newlyUnlocked(stats: PlayerStats, unlockedIds: Set<String>): List<AchievementDef> =
        all.filter { it.id !in unlockedIds && it.progressOf(stats) >= it.maxProgress }
}
