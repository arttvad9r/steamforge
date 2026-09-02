package com.steamforge.game.ui.game

internal enum class MergeFeedbackTier { LOW, MID, HIGH }

internal data class MergeFeedbackProfile(
    val tier: MergeFeedbackTier,
    val playbackRate: Float,
)

internal fun mergeFeedbackProfile(maxLevel: Int, mergeCount: Int): MergeFeedbackProfile {
    val tier = when {
        maxLevel >= 8 -> MergeFeedbackTier.HIGH
        maxLevel >= 5 -> MergeFeedbackTier.MID
        else -> MergeFeedbackTier.LOW
    }
    val playbackRate = when {
        mergeCount <= 1 -> 1.00f
        mergeCount == 2 -> 1.04f
        mergeCount == 3 -> 1.08f
        else -> 1.12f
    }
    return MergeFeedbackProfile(tier, playbackRate)
}

internal fun mergePopScale(level: Int, mergeCount: Int): Float {
    val base = when {
        level >= 11 -> 1.18f
        level >= 8 -> 1.15f
        level >= 5 -> 1.12f
        else -> 1.09f
    }
    val comboBonus = (mergeCount - 1).coerceIn(0, 2) * 0.01f
    return (base + comboBonus).coerceAtMost(1.20f)
}
