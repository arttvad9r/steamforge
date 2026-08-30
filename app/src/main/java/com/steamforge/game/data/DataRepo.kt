package com.steamforge.game.data

import com.steamforge.game.progression.FinishEffects
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
     * Атомарное завершение партии: награды считаются от свежего прогресса,
     * запись результата (с эффектами) и обновлённый прогресс пишутся одной транзакцией.
     */
    suspend fun applyGameFinish(
        record: FinishedGameRecord,
        finisher: (PlayerProgress) -> Pair<PlayerProgress, FinishEffects>,
    )

    /** Атомарная идемпотентная выдача x2-гемов: true только один раз для данного gameResultId. */
    suspend fun claimDoubleReward(gameResultId: String, gems: Int): Boolean

    /** Атомарная награда за daily challenge: true только один раз для epochDay. */
    suspend fun claimDailyChallenge(day: Long, rewardGems: Int, bonusXp: Int): Boolean

    /** Overlay результата обработан (выход/новая партия) — запись больше не нужна. */
    suspend fun clearFinishedGame()

    /** Сброс только игрового прогресса; privacy/settings сохраняются. */
    suspend fun resetGameProgress()
}
