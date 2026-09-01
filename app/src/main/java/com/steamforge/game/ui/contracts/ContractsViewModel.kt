package com.steamforge.game.ui.contracts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.data.DataRepo
import com.steamforge.game.progression.ContractDef
import com.steamforge.game.progression.DailyContracts
import com.steamforge.game.progression.LocalDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContractItemUi(
    val def: ContractDef,
    val progress: Int,
    val claimed: Boolean,
) {
    val complete: Boolean get() = progress >= def.target
    val fraction: Float get() = (progress.toFloat() / def.target.coerceAtLeast(1)).coerceIn(0f, 1f)
}

data class ContractsUiState(
    val day: Long = -1L,
    val gems: Int = 0,
    val items: List<ContractItemUi> = emptyList(),
) {
    val completed: Int get() = items.count { it.complete }
    val claimed: Int get() = items.count { it.claimed }
}

class ContractsViewModel(
    private val repo: DataRepo,
    private val today: () -> Long = { LocalDay.todayEpochDay() },
) : ViewModel() {

    val ui: StateFlow<ContractsUiState> = repo.progress.map { progress ->
        val day = today()
        val ledger = DailyContracts.normalized(progress.contracts, day)
        val items = DailyContracts.forEpochDay(day).map { def ->
            ContractItemUi(
                def = def,
                progress = DailyContracts.progress(def, ledger),
                claimed = def.id in ledger.claimedIds,
            )
        }
        ContractsUiState(
            day = day,
            gems = progress.gems,
            items = items,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContractsUiState())

    fun claim(contractId: String) {
        viewModelScope.launch {
            repo.claimContract(today(), contractId)
        }
    }
}
