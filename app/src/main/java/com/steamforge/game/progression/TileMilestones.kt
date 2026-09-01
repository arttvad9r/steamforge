package com.steamforge.game.progression

/** Rare first-discovery moments for the upper half of the 2048 ladder. */
data class TileMilestone(
    val level: Int,
    val value: Int,
    val title: String,
    val subtitle: String,
)

object TileMilestones {
    val all: List<TileMilestone> = listOf(
        TileMilestone(6, 64, "PRESSURE VALVE", "Контур выдерживает рабочее давление."),
        TileMilestone(7, 128, "BRASS REGULATOR", "Точная регулировка ядра разблокирована."),
        TileMilestone(8, 256, "TURBINE ASSEMBLY", "Паровой привод выходит на проектную мощность."),
        TileMilestone(9, 512, "FORGE REACTOR", "Мастерская получила новый источник энергии."),
        TileMilestone(10, 1024, "MASTER GEAR", "Главная передача механизма собрана."),
        TileMilestone(11, 2048, "MECHANICAL CORE", "Ядро Steamforge введено в строй."),
    )

    fun byLevel(level: Int): TileMilestone? = all.firstOrNull { it.level == level }

    /**
     * Returns the highest milestone crossed by a move. Lower milestones crossed in the same move are
     * considered discovered too, so the player never gets a queue of modal interruptions.
     */
    fun newlyReached(previousMaxLevel: Int, newMaxLevel: Int): TileMilestone? =
        all.lastOrNull { it.level > previousMaxLevel && it.level <= newMaxLevel }
}
