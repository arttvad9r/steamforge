package com.steamforge.game.data

import com.steamforge.game.progression.FinishEffects
import com.steamforge.game.progression.GameSummary
import com.steamforge.game.progression.PlayerProgress
import kotlinx.coroutines.flow.Flow

/** Контракт хранилища: позволяет тестировать ViewModel с in-memory фейком. */
interface DataRepo {
    val progress: Flow<PlayerProgress>
    val savedGame: Flow<SavedGame?>
    val finishedGame: Flow<FinishedGameRecord?>
    suspend fun saveGame(state: SavedGame)
    suspend fun clearGame()
    suspend fun updateProgress(block: (PlayerProgress) -> PlayerProgress)

    /**
     * Сохраняет обычную партию и, если реализация поддерживает contracts, атомарно учитывает
     * high-water прогресс этой партии без второй записи DataStore.
     */
    suspend fun saveGameWithContractProgress(state: SavedGame, day: Long) {
        saveGame(state)
    }

    /**
     * Атомарное завершение партии: награды считаются от свежего прогресса,
     * запись результата (с эффектами) и обновлённый прогресс пишутся одной транзакцией.
     */
    suspend fun applyGameFinish(
        record: FinishedGameRecord,
        finisher: (PlayerProgress) -> Pair<PlayerProgress, FinishEffects>,
    )

    /**
     * Вариант finish-транзакции, который перед finisher учитывает финальный snapshot Contracts.
     * Default оставляет старое поведение для in-memory test repositories.
     */
    suspend fun applyGameFinishWithContractProgress(
        record: FinishedGameRecord,
        summary: GameSummary,
        day: Long,
        runSeed: Long,
        finisher: (PlayerProgress) -> Pair<PlayerProgress, FinishEffects>,
    ) {
        applyGameFinish(record, finisher)
    }

    /** Атомарная идемпотентная выдача x2-гемов: true только один раз для данного gameResultId. */
    suspend fun claimDoubleReward(gameResultId: String, gems: Int): Boolean

    /** Атомарная награда за daily challenge: true только один раз для epochDay. */
    suspend fun claimDailyChallenge(day: Long, rewardGems: Int, bonusXp: Int): Boolean

    /** Атомарная награда за daily contract. */
    suspend fun claimContract(day: Long, contractId: String): Boolean = false

    /** Overlay результата обработан (выход/новая партия) — запись больше не нужна. */
    suspend fun clearFinishedGame()

    /** Сброс только игрового прогресса; privacy/settings сохраняются. */
    suspend fun resetGameProgress()
}
