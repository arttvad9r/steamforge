package com.steamforge.game.core

/** Цепочка механических элементов: level 1..11 = значения 2..2048. */
object Elements {
    data class Element(val level: Int, val name: String) {
        val value: Int get() = 1 shl level
    }

    val chain: List<Element> = listOf(
        Element(1, "Уголь"),
        Element(2, "Медная шестерня"),
        Element(3, "Клапан"),
        Element(4, "Поршень"),
        Element(5, "Паровой котёл"),
        Element(6, "Динамо"),
        Element(7, "Двигатель"),
        Element(8, "Автоматон"),
        Element(9, "Турбина"),
        Element(10, "Реактор"),
        Element(11, "Механическое ядро"),
    )

    private val byLevel = chain.associateBy { it.level }

    fun name(level: Int): String = byLevel[level]?.name ?: "Деталь"
}
