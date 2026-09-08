package com.steamforge.game.ui.blueprints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.BlueprintCollections
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class BlueprintsUiState(
    val loaded: Boolean = false,
    val ownedPieceIds: Set<String> = emptySet(),
    val steamEngineOwned: Int = 0,
    val steamEngineTotal: Int = BlueprintCollections.steamEngine.pieces.size,
    val steamEngineComplete: Boolean = false,
    val animationsEnabled: Boolean = true,
)

class BlueprintsViewModel(repo: DataRepo) : ViewModel() {
    val ui: StateFlow<BlueprintsUiState> = repo.progress
        .map { progress ->
            val collection = BlueprintCollections.steamEngine
            val owned = progress.blueprintPieces.intersect(collection.pieceIds)
            BlueprintsUiState(
                loaded = true,
                ownedPieceIds = owned,
                steamEngineOwned = owned.size,
                steamEngineTotal = collection.pieces.size,
                steamEngineComplete = BlueprintCollections.isComplete(collection, progress.blueprintPieces),
                animationsEnabled = progress.animationsEnabled,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BlueprintsUiState(),
        )
}
