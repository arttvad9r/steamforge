package com.steamforge.game.ui.game

internal enum class MergeFeedbackTier { LOW, MID, HIGH }

internal data class MergeFeedbackProfile(
    val tier: MergeFeedbackTier,
    val playbackRate: Float,
)

/**
 * Feedback is deliberately restrained. Repeated merges gain a little urgency without drifting into arcade-pitch
 * squeaks, while the material tier remains the main signal of progression.
 */
internal fun mergeFeedbackProfile(maxLevel: Int, mergeCount: Int): MergeFeedbackProfile {
    val tier = when {
        maxLevel >= 8 -> MergeFeedbackTier.HIGH
        maxLevel >= 5 -> MergeFeedbackTier.MID
        else -> MergeFeedbackTier.LOW
    }
    val playbackRate = when {
        mergeCount <= 1 -> 1.00f
        mergeCount == 2 -> 1.025f
        mergeCount == 3 -> 1.05f
        else -> 1.075f
    }
    return MergeFeedbackProfile(tier, playbackRate)
}

/**
 * Premium feedback uses a short mechanical "settle" rather than a large cartoon bounce. Higher-value parts still
 * get progressively more presence, but even a multi-merge keeps the board readable.
 */
internal fun mergePopScale(level: Int, mergeCount: Int): Float {
    val base = when {
        level >= 11 -> 1.145f
        level >= 8 -> 1.115f
        level >= 5 -> 1.085f
        else -> 1.055f
    }
    val comboBonus = (mergeCount - 1).coerceIn(0, 2) * 0.0075f
    return (base + comboBonus).coerceAtMost(1.16f)
}
