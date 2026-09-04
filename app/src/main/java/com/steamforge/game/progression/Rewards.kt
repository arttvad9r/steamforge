package com.steamforge.game.progression

/**
 * Единое описание наград. Gems оставлены как legacy-ресурс текущей версии игры;
 * новые meta-системы должны предпочитать WorkshopParts / BlueprintPiece / CosmeticUnlock.
 */
sealed interface Reward {
    data class WorkshopParts(val amount: Int) : Reward
    data class BlueprintPiece(val id: String) : Reward
    data class CosmeticUnlock(val id: String) : Reward
    data class Gems(val amount: Int) : Reward
}

/** Фактически применённая награда после валидации, дедупликации и saturation. */
data class RewardReceipt(
    val workshopParts: Int = 0,
    val gems: Int = 0,
    val blueprintPieces: Set<String> = emptySet(),
    val cosmetics: Set<String> = emptySet(),
) {
    val isEmpty: Boolean
        get() = workshopParts == 0 && gems == 0 && blueprintPieces.isEmpty() && cosmetics.isEmpty()
}

/**
 * Единственная точка применения положительных игровых наград к PlayerProgress.
 * Идемпотентность источника (day/id/claim key) остаётся обязанностью атомарной repository-транзакции;
 * здесь выполняются валидация payload, saturation числовых ресурсов и дедупликация коллекционных unlock.
 */
object RewardSystem {
    fun apply(progress: PlayerProgress, rewards: Iterable<Reward>): Pair<PlayerProgress, RewardReceipt> {
        var current = progress
        var receipt = RewardReceipt()

        rewards.forEach { reward ->
            when (reward) {
                is Reward.WorkshopParts -> {
                    val amount = reward.amount.coerceAtLeast(0)
                    if (amount == 0) return@forEach
                    val before = current.workshopParts.coerceAtLeast(0)
                    val updated = saturatingAdd(before, amount)
                    val applied = updated - before
                    current = current.copy(workshopParts = updated)
                    receipt = receipt.copy(workshopParts = saturatingAdd(receipt.workshopParts, applied))
                }

                is Reward.Gems -> {
                    val amount = reward.amount.coerceAtLeast(0)
                    if (amount == 0) return@forEach
                    val before = current.gems.coerceAtLeast(0)
                    val updated = saturatingAdd(before, amount)
                    val applied = updated - before
                    current = current.copy(
                        gems = updated,
                        stats = current.stats.copy(
                            gemsEarned = saturatingAddLong(current.stats.gemsEarned, applied.toLong()),
                        ),
                    )
                    receipt = receipt.copy(gems = saturatingAdd(receipt.gems, applied))
                }

                is Reward.BlueprintPiece -> {
                    val id = reward.id.trim()
                    if (id.isEmpty() || id in current.blueprintPieces) return@forEach
                    current = current.copy(blueprintPieces = current.blueprintPieces + id)
                    receipt = receipt.copy(blueprintPieces = receipt.blueprintPieces + id)
                }

                is Reward.CosmeticUnlock -> {
                    val id = reward.id.trim()
                    if (id.isEmpty() || id in current.unlockedCosmetics) return@forEach
                    current = current.copy(unlockedCosmetics = current.unlockedCosmetics + id)
                    receipt = receipt.copy(cosmetics = receipt.cosmetics + id)
                }
            }
        }

        return current to receipt
    }

    fun apply(progress: PlayerProgress, vararg rewards: Reward): Pair<PlayerProgress, RewardReceipt> =
        apply(progress, rewards.asIterable())

    private fun saturatingAdd(value: Int, amount: Int): Int =
        (value.toLong().coerceAtLeast(0L) + amount.toLong().coerceAtLeast(0L))
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

    private fun saturatingAddLong(value: Long, amount: Long): Long {
        val safeValue = value.coerceAtLeast(0L)
        if (amount <= 0L) return safeValue
        return if (safeValue >= Long.MAX_VALUE - amount) Long.MAX_VALUE else safeValue + amount
    }
}
