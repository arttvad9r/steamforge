package com.steamforge.game.ui.blueprints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.BlueprintPieceDef
import com.steamforge.game.progression.Blueprints
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class BlueprintPieceUi(
    val def: BlueprintPieceDef,
    val collected: Boolean,
)

data class BlueprintsUiState(
    val title: String = Blueprints.steamEngine.title,
    val description: String = Blueprints.steamEngine.description,
    val pieces: List<BlueprintPieceUi> = Blueprints.steamEngine.pieces.map { BlueprintPieceUi(it, false) },
    val completed: Boolean = false,
    val workshopUnlocked: Boolean = false,
) {
    val collected: Int get() = pieces.count { it.collected }
    val total: Int get() = pieces.size
    val fraction: Float get() = (collected.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
}

class BlueprintsViewModel(repo: DataRepo) : ViewModel() {
    val ui: StateFlow<BlueprintsUiState> = repo.progress.map { progress ->
        val set = Blueprints.steamEngine
        BlueprintsUiState(
            title = set.title,
            description = set.description,
            pieces = set.pieces.map { piece -> BlueprintPieceUi(piece, piece.id in progress.blueprintPieces) },
            completed = Blueprints.isComplete(set, progress.blueprintPieces),
            workshopUnlocked = set.workshopUnlockId in progress.unlockedCosmetics,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlueprintsUiState())
}
