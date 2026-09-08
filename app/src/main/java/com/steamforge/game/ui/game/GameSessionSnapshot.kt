package com.steamforge.game.ui.game

import com.steamforge.game.data.SavedGame

/**
 * Single persisted-session projection shared by active autosave and terminal result storage.
 * Keep this mapping in one place when SavedGame/session fields evolve.
 */
internal fun buildSavedGameSnapshot(
    ui: GameUiState,
    sessionSeed: Long?,
    rngDraws: Long,
): SavedGame = SavedGame(
    state = ui.state,
    seed = sessionSeed,
    pressure = ui.pressure,
    overdriveRemaining = ui.overdriveRemaining,
    freeUndosLeft = ui.freeUndosLeft,
    rngDraws = rngDraws,
    mergesTotal = ui.mergesTotal,
    maxMergesInOneMove = ui.maxMergesInOneMove,
    overdrivesSession = ui.overdrivesSession,
    undosSession = ui.undosSession,
    highMergesSession = ui.highMergesSession,
)
