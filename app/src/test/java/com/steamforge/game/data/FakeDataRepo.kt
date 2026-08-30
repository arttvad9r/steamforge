package com.steamforge.game.data

import com.steamforge.game.progression.PlayerProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory фейк для тестов ViewModel. */
class FakeDataRepo(
    initialProgress: PlayerProgress = PlayerProgress(),
    initialGame: SavedGame? = null,
    initialFinished: FinishedGameRecord? = null,
) : DataRepo {

    private val progressFlow = MutableStateFlow(initialProgress)
    private val gameFlow = MutableStateFlow(initialGame)
    private val finishedFlow = MutableStateFlow(initialFinished)

    var currentProgress: PlayerProgress
        get() = progressFlow.value
        set(value) { progressFlow.value = value }

    var currentGame: SavedGame?
        get() = gameFlow.value
        set(value) { gameFlow.value = value }

    var currentFinished: FinishedGameRecord?
        get() = finishedFlow.value
        set(value) { finishedFlow.value = value }

    override val progress: Flow<PlayerProgress> = progressFlow
    override val savedGame: Flow<SavedGame?> = gameFlow
    override val finishedGame: Flow<FinishedGameRecord?> = finishedFlow

    override suspend fun saveGame(state: SavedGame) {
        gameFlow.value = state
    }

    override suspend fun clearGame() {
        gameFlow.value = null
    }

    override suspend fun updateProgress(block: (PlayerProgress) -> PlayerProgress) {
        progressFlow.value = block(progressFlow.value)
    }

    override suspend fun applyGameFinish(
        record: FinishedGameRecord,
        finisher: (PlayerProgress) -> Pair<PlayerProgress, com.steamforge.game.progression.FinishEffects>,
    ) {
        val (updated, effects) = finisher(progressFlow.value)
        finishedFlow.value = record.withEffects(effects)
        gameFlow.value = null
        progressFlow.value = updated
    }

    override suspend fun claimDoubleReward(gameResultId: String, gems: Int): Boolean {
        val record = finishedFlow.value ?: return false
        if (record.id != gameResultId || record.rewardedClaimed || gems <= 0) return false
        finishedFlow.value = record.copy(rewardedClaimed = true)
        progressFlow.value = progressFlow.value.let { p ->
            p.copy(gems = p.gems + gems, stats = p.stats.copy(gemsEarned = p.stats.gemsEarned + gems))
        }
        return true
    }

    override suspend fun clearFinishedGame() {
        finishedFlow.value = null
    }

    override suspend fun resetAll() {
        progressFlow.value = PlayerProgress()
        gameFlow.value = null
        finishedFlow.value = null
    }
}
