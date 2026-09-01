package com.steamforge.game.progression

/**
 * Onboarding не является отдельным tutorial-mode core: этапы только управляют тем, какой уже существующий
 * экран показывается новому игроку. Обычная партия остаётся настоящей и сохраняемой.
 */
object Onboarding {
    const val CORE = 0
    const val WORKSHOP = 1
    const val COMPLETE = 2

    fun normalize(step: Int): Int = step.coerceIn(CORE, COMPLETE)

    /**
     * Старые установки не должны внезапно попадать в onboarding после обновления. Если ключ ещё не
     * существовал, любой реальный след предыдущей игры считается достаточным для COMPLETE.
     */
    fun resolveInitialStep(storedStep: Int?, hasLegacyProgress: Boolean): Int = when {
        storedStep != null -> normalize(storedStep)
        hasLegacyProgress -> COMPLETE
        else -> CORE
    }
}
