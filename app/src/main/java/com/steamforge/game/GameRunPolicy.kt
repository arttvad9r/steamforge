package com.steamforge.game

import com.steamforge.game.progression.WeeklyRules

/**
 * Product-level session behavior derived from [GameRunMode]. Keeping these switches together prevents
 * a competitive WEEKLY run from accidentally falling through NORMAL save/economy/tooling paths.
 */
data class GameRunPolicy(
    val mode: GameRunMode,
    val persistActiveRun: Boolean,
    val persistFinishedResult: Boolean,
    val restoreFinishedResult: Boolean,
    val grantProgressionOnFinish: Boolean,
    val allowUndo: Boolean,
    val allowWrench: Boolean,
    val allowOverdrive: Boolean,
) {
    val isDaily: Boolean
        get() = mode == GameRunMode.DAILY

    val isWeekly: Boolean
        get() = mode == GameRunMode.WEEKLY
}

object GameRunPolicies {
    fun resolve(
        mode: GameRunMode,
        weeklyRules: WeeklyRules = WeeklyRules(),
    ): GameRunPolicy = when (mode) {
        GameRunMode.NORMAL -> GameRunPolicy(
            mode = mode,
            persistActiveRun = true,
            persistFinishedResult = true,
            restoreFinishedResult = true,
            grantProgressionOnFinish = true,
            allowUndo = true,
            allowWrench = true,
            allowOverdrive = true,
        )

        GameRunMode.DAILY -> GameRunPolicy(
            mode = mode,
            persistActiveRun = false,
            persistFinishedResult = true,
            restoreFinishedResult = true,
            grantProgressionOnFinish = true,
            allowUndo = true,
            allowWrench = true,
            allowOverdrive = true,
        )

        GameRunMode.WEEKLY -> GameRunPolicy(
            mode = mode,
            persistActiveRun = false,
            persistFinishedResult = false,
            restoreFinishedResult = false,
            grantProgressionOnFinish = false,
            allowUndo = weeklyRules.allowUndo,
            allowWrench = weeklyRules.allowWrench,
            allowOverdrive = weeklyRules.allowOverdrive,
        )
    }
}
