package com.steamforge.game.progression

import kotlin.math.max
import kotlin.random.Random

enum class ContractType {
    MAKE_TILE,
    MERGE_COUNT,
    SCORE,
    PLAY_RUNS,
    SURVIVE_MOVES,
    OVERDRIVE,
}

sealed interface ContractReward {
    data class WorkshopParts(val amount: Int) : ContractReward
    data class BlueprintPiece(val id: String) : ContractReward

    fun toReward(): Reward = when (this) {
        is WorkshopParts -> Reward.WorkshopParts(amount)
        is BlueprintPiece -> Reward.BlueprintPiece(id)
    }
}

data class ContractDef(
    val id: String,
    val type: ContractType,
    val target: Int,
    val reward: ContractReward,
    val title: String,
    val description: String,
) {
    /**
     * Совместимость со старым repository claim-path. Новый UI этот путь не использует:
     * DailyContracts.claim применяет типизированную reward через RewardSystem.
     */
    @Deprecated("Use reward")
    val rewardGems: Int
        get() = when (val value = reward) {
            is ContractReward.WorkshopParts -> value.amount
            is ContractReward.BlueprintPiece -> 0
        }
}

data class ContractCounters(
    val score: Int = 0,
    val merges: Int = 0,
    val moves: Int = 0,
    val runs: Int = 0,
    val maxTileLevel: Int = 0,
    val overdrives: Int = 0,
) {
    fun highWater(other: ContractCounters): ContractCounters = ContractCounters(
        score = max(score, other.score),
        merges = max(merges, other.merges),
        moves = max(moves, other.moves),
        runs = max(runs, other.runs),
        maxTileLevel = max(maxTileLevel, other.maxTileLevel),
        overdrives = max(overdrives, other.overdrives),
    )

    fun positiveDelta(previous: ContractCounters): ContractCounters = ContractCounters(
        score = (score - previous.score).coerceAtLeast(0),
        merges = (merges - previous.merges).coerceAtLeast(0),
        moves = (moves - previous.moves).coerceAtLeast(0),
        runs = (runs - previous.runs).coerceAtLeast(0),
        maxTileLevel = maxTileLevel,
        overdrives = (overdrives - previous.overdrives).coerceAtLeast(0),
    )

    fun plus(delta: ContractCounters): ContractCounters = ContractCounters(
        score = (score + delta.score).coerceAtMost(MAX_COUNTER),
        merges = (merges + delta.merges).coerceAtMost(MAX_COUNTER),
        moves = (moves + delta.moves).coerceAtMost(MAX_COUNTER),
        runs = (runs + delta.runs).coerceAtMost(MAX_COUNTER),
        maxTileLevel = max(maxTileLevel, delta.maxTileLevel),
        overdrives = (overdrives + delta.overdrives).coerceAtMost(MAX_COUNTER),
    )

    companion object {
        private const val MAX_COUNTER = 10_000_000

        fun fromSummary(summary: GameSummary): ContractCounters = ContractCounters(
            score = summary.score,
            merges = summary.merges,
            moves = summary.moves,
            maxTileLevel = summary.maxTileLevel,
            overdrives = summary.overdrives,
        )
    }
}

data class ContractLedger(
    val day: Long = -1L,
    val totals: ContractCounters = ContractCounters(),
    val claimedIds: Set<String> = emptySet(),
    val activeRunSeed: Long? = null,
    val activeRun: ContractCounters = ContractCounters(),
)

object DailyContracts {
    private const val CONTRACTS_PER_DAY = 3

    fun forEpochDay(epochDay: Long): List<ContractDef> {
        val rng = Random(epochDay * 6364136223846793005L + 1442695040888963407L)
        return ContractType.entries
            .shuffled(rng)
            .take(CONTRACTS_PER_DAY)
            .mapIndexed { index, type -> definition(epochDay, index, type, rng) }
    }

    fun progress(def: ContractDef, ledger: ContractLedger): Int {
        val raw = when (def.type) {
            ContractType.MAKE_TILE -> tileValue(ledger.totals.maxTileLevel)
            ContractType.MERGE_COUNT -> ledger.totals.merges
            ContractType.SCORE -> ledger.totals.score
            ContractType.PLAY_RUNS -> ledger.totals.runs
            ContractType.SURVIVE_MOVES -> ledger.totals.moves
            ContractType.OVERDRIVE -> ledger.totals.overdrives
        }
        return raw.coerceIn(0, def.target)
    }

    fun isComplete(def: ContractDef, ledger: ContractLedger): Boolean = progress(def, ledger) >= def.target

    /**
     * Атомарно вызывается внутри DataRepo.updateProgress. Награда всегда проходит через RewardSystem,
     * а claimedIds делает повторный tap безопасным и идемпотентным.
     */
    fun claim(progress: PlayerProgress, day: Long, contractId: String): PlayerProgress {
        val ledger = normalized(progress.contracts, day)
        val contract = forEpochDay(day).firstOrNull { it.id == contractId } ?: return progress
        if (contract.id in ledger.claimedIds || !isComplete(contract, ledger)) return progress

        val (rewarded, _) = RewardSystem.apply(progress, contract.reward.toReward())
        return rewarded.copy(
            contracts = ledger.copy(claimedIds = ledger.claimedIds + contract.id),
        )
    }

    fun recordLiveSnapshot(
        progress: PlayerProgress,
        day: Long,
        runSeed: Long,
        snapshot: ContractCounters,
    ): PlayerProgress {
        val ledger = normalized(progress.contracts, day)
        val previous = if (ledger.activeRunSeed == runSeed) ledger.activeRun else ContractCounters()
        val highWater = previous.highWater(snapshot)
        val delta = highWater.positiveDelta(previous)
        val totals = ledger.totals.plus(delta).copy(
            maxTileLevel = max(ledger.totals.maxTileLevel, highWater.maxTileLevel),
        )
        return progress.copy(
            contracts = ledger.copy(
                totals = totals,
                activeRunSeed = runSeed,
                activeRun = highWater,
            ),
        )
    }

    fun recordFinishedRun(
        progress: PlayerProgress,
        day: Long,
        runSeed: Long,
        summary: GameSummary,
    ): PlayerProgress {
        val ledger = normalized(progress.contracts, day)
        val snapshot = ContractCounters.fromSummary(summary)
        val sameAsActiveRun = ledger.activeRunSeed == runSeed
        val previous = if (sameAsActiveRun) ledger.activeRun else ContractCounters()
        val highWater = previous.highWater(snapshot)
        val delta = highWater.positiveDelta(previous)
        val totals = ledger.totals
            .plus(delta)
            .plus(ContractCounters(runs = 1))
            .copy(maxTileLevel = max(ledger.totals.maxTileLevel, highWater.maxTileLevel))

        return progress.copy(
            contracts = ledger.copy(
                totals = totals,
                activeRunSeed = if (sameAsActiveRun) null else ledger.activeRunSeed,
                activeRun = if (sameAsActiveRun) ContractCounters() else ledger.activeRun,
            ),
        )
    }

    fun normalized(ledger: ContractLedger, day: Long): ContractLedger =
        if (ledger.day == day) ledger else ContractLedger(day = day)

    private fun definition(epochDay: Long, slot: Int, type: ContractType, rng: Random): ContractDef {
        val target = when (type) {
            ContractType.MAKE_TILE -> intArrayOf(128, 256, 256, 512)[rng.nextInt(4)]
            ContractType.MERGE_COUNT -> 20 + rng.nextInt(4) * 10
            ContractType.SCORE -> 1_500 + rng.nextInt(5) * 500
            ContractType.PLAY_RUNS -> 1 + rng.nextInt(3)
            ContractType.SURVIVE_MOVES -> 60 + rng.nextInt(5) * 20
            ContractType.OVERDRIVE -> 1 + rng.nextInt(3)
        }
        val rewardParts = when (type) {
            ContractType.MAKE_TILE -> if (target >= 512) 18 else 14
            ContractType.MERGE_COUNT -> 10 + target / 10
            ContractType.SCORE -> 10 + target / 500
            ContractType.PLAY_RUNS -> 8 + target * 3
            ContractType.SURVIVE_MOVES -> 10 + target / 20
            ContractType.OVERDRIVE -> 10 + target * 4
        }
        val title = when (type) {
            ContractType.MAKE_TILE -> "Собрать деталь $target"
            ContractType.MERGE_COUNT -> "Выполнить $target объединений"
            ContractType.SCORE -> "Набрать $target очков"
            ContractType.PLAY_RUNS -> "Завершить $target ${runWord(target)}"
            ContractType.SURVIVE_MOVES -> "Сделать $target ходов"
            ContractType.OVERDRIVE -> "Запустить Overdrive ×$target"
        }
        val description = when (type) {
            ContractType.MAKE_TILE -> "Лучший достигнутый номинал за сегодня"
            ContractType.MERGE_COUNT -> "Объединения суммируются между партиями"
            ContractType.SCORE -> "Очки суммируются между партиями"
            ContractType.PLAY_RUNS -> "Засчитываются завершённые партии"
            ContractType.SURVIVE_MOVES -> "Ходы суммируются между партиями"
            ContractType.OVERDRIVE -> "Активации давления суммируются за день"
        }
        return ContractDef(
            id = "$epochDay-$slot-${type.name}",
            type = type,
            target = target,
            reward = ContractReward.WorkshopParts(rewardParts),
            title = title,
            description = description,
        )
    }

    private fun tileValue(level: Int): Int = when {
        level <= 0 -> 0
        level >= 30 -> 1 shl 30
        else -> 1 shl level
    }

    private fun runWord(count: Int): String = when (count) {
        1 -> "партию"
        else -> "партии"
    }
}
