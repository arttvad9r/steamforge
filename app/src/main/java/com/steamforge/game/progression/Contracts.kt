package com.steamforge.game.progression

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

enum class ContractType {
    MAKE_TILE,
    REACH_TILE,
    MERGE_COUNT,
    SCORE,
    TOTAL_SCORE,
    COMBO_COUNT,
    PLAY_RUNS,
    SURVIVE_MOVES,
}

/**
 * Typed gameplay facts consumed by meta systems. Contracts reduce these events into persisted counters;
 * gameplay does not need a contract-specific branch.
 */
sealed interface GameEvent {
    data class ScoreAdded(val amount: Int) : GameEvent
    data class ScoreReached(val score: Int) : GameEvent
    data class TilesMerged(val count: Int) : GameEvent
    data class TileCreated(val level: Int, val count: Int = 1) : GameEvent
    data class MovesSurvived(val count: Int) : GameEvent
    data class TileReached(val level: Int) : GameEvent
    data class ComboReached(val merges: Int) : GameEvent
    data class OverdriveActivated(val count: Int) : GameEvent
    data object RunFinished : GameEvent
}

sealed interface ContractReward {
    data class WorkshopParts(val amount: Int) : ContractReward
    data class BlueprintPiece(
        val collectionId: String,
        val fallbackParts: Int,
    ) : ContractReward
}

fun ContractReward.scaledWorkshopParts(multiplier: Double): ContractReward {
    val safeMultiplier = if (multiplier.isFinite() && multiplier in 0.25..4.0) multiplier else 1.0
    fun scaled(amount: Int): Int =
        (amount.coerceAtLeast(1).toDouble() * safeMultiplier)
            .roundToInt()
            .coerceAtLeast(1)

    return when (this) {
        is ContractReward.WorkshopParts -> copy(amount = scaled(amount))
        is ContractReward.BlueprintPiece -> copy(fallbackParts = scaled(fallbackParts))
    }
}

data class ContractDef(
    val id: String,
    val type: ContractType,
    /** Progress required for completion: count, score, merges, runs or moves. */
    val target: Int,
    val reward: ContractReward,
    val title: String,
    val description: String,
    /** Tile level qualifier for MAKE_TILE / REACH_TILE. Level 1 == tile value 2. */
    val tileLevel: Int? = null,
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
    /** Сумма очков за день; TOTAL_SCORE использует именно этот счётчик. */
    val score: Int = 0,
    /** Лучший результат одной партии за день; SCORE использует этот high-water. */
    val bestRunScore: Int = 0,
    val merges: Int = 0,
    val moves: Int = 0,
    val runs: Int = 0,
    val maxTileLevel: Int = 0,
    /** Максимальное число merge за один ход. */
    val maxCombo: Int = 0,
    val overdrives: Int = 0,
    /** Число реально созданных merge-плиток по level; spawn 2/4 сюда не входит. */
    val madeTilesByLevel: Map<Int, Int> = emptyMap(),
) {
    fun highWater(other: ContractCounters): ContractCounters = ContractCounters(
        score = max(score, other.score),
        bestRunScore = max(bestRunScore, other.bestRunScore),
        merges = max(merges, other.merges),
        moves = max(moves, other.moves),
        runs = max(runs, other.runs),
        maxTileLevel = max(maxTileLevel, other.maxTileLevel),
        maxCombo = max(maxCombo, other.maxCombo),
        overdrives = max(overdrives, other.overdrives),
        madeTilesByLevel = mapHighWater(madeTilesByLevel, other.madeTilesByLevel),
    )

    fun positiveDelta(previous: ContractCounters): ContractCounters = ContractCounters(
        score = (score - previous.score).coerceAtLeast(0),
        bestRunScore = bestRunScore,
        merges = (merges - previous.merges).coerceAtLeast(0),
        moves = (moves - previous.moves).coerceAtLeast(0),
        runs = (runs - previous.runs).coerceAtLeast(0),
        maxTileLevel = maxTileLevel,
        maxCombo = maxCombo,
        overdrives = (overdrives - previous.overdrives).coerceAtLeast(0),
        madeTilesByLevel = mapPositiveDelta(madeTilesByLevel, previous.madeTilesByLevel),
    )

    fun plus(delta: ContractCounters): ContractCounters = ContractCounters(
        score = saturatingAdd(score, delta.score),
        bestRunScore = max(bestRunScore, delta.bestRunScore),
        merges = saturatingAdd(merges, delta.merges),
        moves = saturatingAdd(moves, delta.moves),
        runs = saturatingAdd(runs, delta.runs),
        maxTileLevel = max(maxTileLevel, delta.maxTileLevel),
        maxCombo = max(maxCombo, delta.maxCombo),
        overdrives = saturatingAdd(overdrives, delta.overdrives),
        madeTilesByLevel = mapSum(madeTilesByLevel, delta.madeTilesByLevel),
    )

    fun madeTileCount(level: Int): Int = madeTilesByLevel[level] ?: 0

    /** Single reducer used by Contracts for all gameplay progress. */
    fun record(event: GameEvent): ContractCounters = when (event) {
        is GameEvent.ScoreAdded -> copy(score = saturatingAdd(score, event.amount))
        is GameEvent.ScoreReached -> copy(bestRunScore = max(bestRunScore, event.score.coerceAtLeast(0)))
        is GameEvent.TilesMerged -> copy(merges = saturatingAdd(merges, event.count))
        is GameEvent.TileCreated -> {
            if (event.level !in 1..30 || event.count <= 0) this else copy(
                madeTilesByLevel = madeTilesByLevel +
                    (event.level to saturatingAdd(madeTileCount(event.level), event.count)),
            )
        }
        is GameEvent.MovesSurvived -> copy(moves = saturatingAdd(moves, event.count))
        is GameEvent.TileReached -> copy(maxTileLevel = max(maxTileLevel, event.level.coerceAtLeast(0)))
        is GameEvent.ComboReached -> copy(maxCombo = max(maxCombo, event.merges.coerceAtLeast(0)))
        is GameEvent.OverdriveActivated -> copy(overdrives = saturatingAdd(overdrives, event.count))
        GameEvent.RunFinished -> copy(runs = saturatingAdd(runs, 1))
    }

    fun record(events: Iterable<GameEvent>): ContractCounters = events.fold(this) { current, event ->
        current.record(event)
    }

    companion object {
        private const val MAX_COUNTER = 10_000_000

        private fun saturatingAdd(value: Int, amount: Int): Int =
            (value.toLong().coerceAtLeast(0L) + amount.toLong().coerceAtLeast(0L))
                .coerceAtMost(MAX_COUNTER.toLong())
                .toInt()

        private fun mapHighWater(first: Map<Int, Int>, second: Map<Int, Int>): Map<Int, Int> =
            (first.keys + second.keys).associateWith { level ->
                max(first[level] ?: 0, second[level] ?: 0).coerceAtMost(MAX_COUNTER)
            }.filterValues { it > 0 }

        private fun mapPositiveDelta(current: Map<Int, Int>, previous: Map<Int, Int>): Map<Int, Int> =
            current.mapNotNull { (level, count) ->
                val delta = (count - (previous[level] ?: 0)).coerceAtLeast(0)
                if (delta > 0) level to delta else null
            }.toMap()

        private fun mapSum(first: Map<Int, Int>, second: Map<Int, Int>): Map<Int, Int> =
            (first.keys + second.keys).associateWith { level ->
                saturatingAdd(first[level] ?: 0, second[level] ?: 0)
            }.filterValues { it > 0 }

        fun fromSummary(summary: GameSummary): ContractCounters = ContractCounters(
            score = summary.score,
            bestRunScore = summary.score,
            merges = summary.merges,
            moves = summary.moves,
            maxTileLevel = summary.maxTileLevel,
            maxCombo = summary.maxMergesInOneMove,
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
    private const val BLUEPRINT_REWARD_PERIOD_DAYS = 3L

    fun forEpochDay(epochDay: Long, blueprintAvailable: Boolean = true): List<ContractDef> {
        val rng = Random(epochDay * 6364136223846793005L + 1442695040888963407L)
        return ContractType.entries
            .shuffled(rng)
            .take(CONTRACTS_PER_DAY)
            .mapIndexed { index, type -> definition(epochDay, index, type, rng, blueprintAvailable) }
    }

    fun progress(def: ContractDef, ledger: ContractLedger): Int {
        val raw = when (def.type) {
            ContractType.MAKE_TILE -> ledger.totals.madeTileCount(def.tileLevel ?: 0)
            ContractType.REACH_TILE -> if (ledger.totals.maxTileLevel >= (def.tileLevel ?: Int.MAX_VALUE)) 1 else 0
            ContractType.MERGE_COUNT -> ledger.totals.merges
            ContractType.SCORE -> ledger.totals.bestRunScore
            ContractType.TOTAL_SCORE -> ledger.totals.score
            ContractType.COMBO_COUNT -> ledger.totals.maxCombo
            ContractType.PLAY_RUNS -> ledger.totals.runs
            ContractType.SURVIVE_MOVES -> ledger.totals.moves
        }
        return raw.coerceIn(0, def.target)
    }

    fun isComplete(def: ContractDef, ledger: ContractLedger): Boolean = progress(def, ledger) >= def.target

    /**
     * Атомарно вызывается внутри DataRepo.updateProgress. Награда всегда проходит через RewardSystem,
     * а claimedIds делает повторный tap безопасным и идемпотентным.
     */
    fun claim(
        progress: PlayerProgress,
        day: Long,
        contractId: String,
        workshopPartsMultiplier: Double = 1.0,
    ): PlayerProgress {
        val ledger = normalized(progress.contracts, day)
        val blueprintAvailable = !BlueprintCollections.isSteamEngineComplete(progress.blueprintPieces)
        val contract = forEpochDay(day, blueprintAvailable).firstOrNull { it.id == contractId } ?: return progress
        if (contract.id in ledger.claimedIds || !isComplete(contract, ledger)) return progress

        val effectiveReward = contract.reward.scaledWorkshopParts(workshopPartsMultiplier)
        val reward = when (val value = effectiveReward) {
            is ContractReward.WorkshopParts -> Reward.WorkshopParts(value.amount)
            is ContractReward.BlueprintPiece -> {
                val piece = BlueprintCollections.nextMissingPiece(value.collectionId, progress.blueprintPieces)
                if (piece != null) Reward.BlueprintPiece(piece.id) else Reward.WorkshopParts(value.fallbackParts)
            }
        }
        val (rewarded, _) = RewardSystem.apply(progress, reward)
        return rewarded.copy(
            contracts = ledger.copy(claimedIds = ledger.claimedIds + contract.id),
        )
    }

    /**
     * Snapshot is only an idempotency adapter for autosave/undo. Persisted contract totals are updated
     * exclusively by typed GameEvent values produced from the positive high-water delta.
     */
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
        val events = eventsForDelta(
            delta = delta,
            reachedScore = highWater.bestRunScore.takeIf { it > ledger.totals.bestRunScore },
            reachedLevel = highWater.maxTileLevel.takeIf { it > ledger.totals.maxTileLevel },
            reachedCombo = highWater.maxCombo.takeIf { it > ledger.totals.maxCombo },
        )
        val totals = ledger.totals.record(events)
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
    ): PlayerProgress = recordFinishedRun(
        progress = progress,
        day = day,
        runSeed = runSeed,
        snapshot = ContractCounters.fromSummary(summary),
    )

    /** Final snapshot contributes only the missing delta, then emits exactly one RunFinished event. */
    fun recordFinishedRun(
        progress: PlayerProgress,
        day: Long,
        runSeed: Long,
        snapshot: ContractCounters,
    ): PlayerProgress {
        val ledger = normalized(progress.contracts, day)
        val sameAsActiveRun = ledger.activeRunSeed == runSeed
        val previous = if (sameAsActiveRun) ledger.activeRun else ContractCounters()
        val highWater = previous.highWater(snapshot)
        val delta = highWater.positiveDelta(previous)
        val events = eventsForDelta(
            delta = delta,
            reachedScore = highWater.bestRunScore.takeIf { it > ledger.totals.bestRunScore },
            reachedLevel = highWater.maxTileLevel.takeIf { it > ledger.totals.maxTileLevel },
            reachedCombo = highWater.maxCombo.takeIf { it > ledger.totals.maxCombo },
            runFinished = true,
        )
        val totals = ledger.totals.record(events)

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

    private fun eventsForDelta(
        delta: ContractCounters,
        reachedScore: Int?,
        reachedLevel: Int?,
        reachedCombo: Int?,
        runFinished: Boolean = false,
    ): List<GameEvent> = buildList {
        if (delta.score > 0) add(GameEvent.ScoreAdded(delta.score))
        if (reachedScore != null && reachedScore > 0) add(GameEvent.ScoreReached(reachedScore))
        if (delta.merges > 0) add(GameEvent.TilesMerged(delta.merges))
        delta.madeTilesByLevel.forEach { (level, count) ->
            if (count > 0) add(GameEvent.TileCreated(level, count))
        }
        if (delta.moves > 0) add(GameEvent.MovesSurvived(delta.moves))
        if (reachedLevel != null && reachedLevel > 0) add(GameEvent.TileReached(reachedLevel))
        if (reachedCombo != null && reachedCombo > 0) add(GameEvent.ComboReached(reachedCombo))
        if (delta.overdrives > 0) add(GameEvent.OverdriveActivated(delta.overdrives))
        if (runFinished) add(GameEvent.RunFinished)
    }

    private fun definition(
        epochDay: Long,
        slot: Int,
        type: ContractType,
        rng: Random,
        blueprintAvailable: Boolean,
    ): ContractDef {
        val tileLevel = when (type) {
            ContractType.MAKE_TILE -> intArrayOf(6, 7, 7, 8)[rng.nextInt(4)]
            ContractType.REACH_TILE -> intArrayOf(7, 8, 8, 9)[rng.nextInt(4)]
            else -> null
        }
        val target = when (type) {
            ContractType.MAKE_TILE -> 2 + rng.nextInt(3)
            ContractType.REACH_TILE -> 1
            ContractType.MERGE_COUNT -> 25 + rng.nextInt(4) * 10
            ContractType.SCORE -> 3_000 + rng.nextInt(4) * 1_000
            ContractType.TOTAL_SCORE -> 8_000 + rng.nextInt(5) * 2_000
            ContractType.COMBO_COUNT -> 2 + rng.nextInt(3)
            ContractType.PLAY_RUNS -> 1 + rng.nextInt(3)
            ContractType.SURVIVE_MOVES -> 80 + rng.nextInt(5) * 20
        }
        val tile = tileLevel?.let(::tileValue)
        val rewardParts = when (type) {
            ContractType.MAKE_TILE -> 10 + (tileLevel ?: 0) + target * 2
            ContractType.REACH_TILE -> 10 + (tileLevel ?: 0) * 2
            ContractType.MERGE_COUNT -> 10 + target / 10
            ContractType.SCORE -> 10 + target / 500
            ContractType.TOTAL_SCORE -> 10 + target / 1_500
            ContractType.COMBO_COUNT -> 10 + target * 3
            ContractType.PLAY_RUNS -> 8 + target * 3
            ContractType.SURVIVE_MOVES -> 10 + target / 20
        }
        val title = when (type) {
            ContractType.MAKE_TILE -> "Создать деталь $tile ×$target"
            ContractType.REACH_TILE -> "Достичь детали $tile"
            ContractType.MERGE_COUNT -> "Выполнить $target объединений"
            ContractType.SCORE -> "Набрать $target очков за партию"
            ContractType.TOTAL_SCORE -> "Набрать $target очков суммарно"
            ContractType.COMBO_COUNT -> "Сделать комбо ×$target"
            ContractType.PLAY_RUNS -> "Завершить $target ${runWord(target)}"
            ContractType.SURVIVE_MOVES -> "Сделать $target ходов"
        }
        val description = when (type) {
            ContractType.MAKE_TILE -> "Считаются только детали, созданные объединением"
            ContractType.REACH_TILE -> "Лучший достигнутый номинал за сегодня"
            ContractType.MERGE_COUNT -> "Объединения суммируются между партиями"
            ContractType.SCORE -> "Лучший результат одной партии"
            ContractType.TOTAL_SCORE -> "Очки суммируются между партиями"
            ContractType.COMBO_COUNT -> "Максимум объединений за один ход"
            ContractType.PLAY_RUNS -> "Засчитываются завершённые партии"
            ContractType.SURVIVE_MOVES -> "Ходы суммируются между партиями"
        }
        val blueprintReward = blueprintAvailable &&
            slot == 0 &&
            Math.floorMod(epochDay, BLUEPRINT_REWARD_PERIOD_DAYS) == 0L
        val reward = if (blueprintReward) {
            ContractReward.BlueprintPiece(
                collectionId = BlueprintCollections.STEAM_ENGINE_ID,
                fallbackParts = rewardParts,
            )
        } else {
            ContractReward.WorkshopParts(rewardParts)
        }
        return ContractDef(
            id = "$epochDay-$slot-${type.name}",
            type = type,
            target = target,
            reward = reward,
            title = title,
            description = description,
            tileLevel = tileLevel,
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
