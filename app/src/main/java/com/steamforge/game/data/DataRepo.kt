package com.steamforge.game.data

import com.steamforge.game.core.Move
import com.steamforge.game.progression.FinishEffects
import com.steamforge.game.progression.GameSummary
import com.steamforge.game.progression.PlayerProgress
import com.steamforge.game.progression.WeeklyChallenge
import com.steamforge.game.progression.WeeklyVerifiedResult
import kotlinx.coroutines.flow.Flow

/** Контракт хранилища: позволяет тестировать ViewModel с in-memory фейком. */
interface DataRepo {
    val progress: Flow<PlayerProgress>
    val savedGame: Flow<SavedGame?>
    val finishedGame: Flow<FinishedGameRecord?>
    suspend fun saveGame(state: SavedGame)
    suspend fun clearGame()
    suspend fun updateProgress(block: (PlayerProgress) -> PlayerProgress)

    suspend fun saveGameWithContractProgress(state: SavedGame, day: Long) {
        saveGame(state)
    }

    suspend fun applyGameFinish(
        record: FinishedGameRecord,
        finisher: (PlayerProgress) -> Pair<PlayerProgress, FinishEffects>,
    )

    suspend fun applyGameFinishWithContractProgress(
        record: FinishedGameRecord,
        summary: GameSummary,
        day: Long,
        runSeed: Long,
        finisher: (PlayerProgress) -> Pair<PlayerProgress, FinishEffects>,
    ) {
        applyGameFinish(record, finisher)
    }

    suspend fun claimDoubleReward(gameResultId: String, gems: Int): Boolean
    suspend fun claimDailyChallenge(day: Long, rewardGems: Int, bonusXp: Int): Boolean
    suspend fun claimContract(day: Long, contractId: String): Boolean = false

    /**
     * Не принимает client score. Реализация должна пересчитать seed + move sequence и сохранить только
     * verified result.
     */
    suspend fun submitWeeklyChallenge(
        challenge: WeeklyChallenge,
        moves: List<Move>,
    ): WeeklyVerifiedResult? = null

    /** Недельная награда выдаётся только после verified завершённой попытки и только один раз. */
    suspend fun claimWeeklyReward(challenge: WeeklyChallenge): Boolean = false

    suspend fun clearFinishedGame()
    suspend fun resetGameProgress()
}
