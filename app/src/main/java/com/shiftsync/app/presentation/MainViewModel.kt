package com.shiftsync.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shiftsync.app.domain.model.Employee
import com.shiftsync.app.domain.model.ScheduleInsight
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.repository.ScheduleRepository
import com.shiftsync.app.domain.usecase.ClaimOpenShiftUseCase
import com.shiftsync.app.domain.usecase.GetScheduleInsightsUseCase
import com.shiftsync.app.domain.usecase.RequestShiftSwapUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    repository: ScheduleRepository,
    private val requestShiftSwap: RequestShiftSwapUseCase,
    private val claimOpenShift: ClaimOpenShiftUseCase,
    private val getScheduleInsights: GetScheduleInsightsUseCase,
    private val employeeId: String
) : ViewModel() {
    private val mutableState = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeMyShifts(employeeId),
                repository.observeOpenShifts(),
                repository.observeTeam()
            ) { myShifts, openShifts, team ->
                ScheduleUiState(myShifts = myShifts, openShifts = openShifts, team = team)
            }.collect { nextState ->
                mutableState.update {
                    nextState.copy(
                        selectedShift = it.selectedShift,
                        swapReason = it.swapReason,
                        selectedRecipientId = it.selectedRecipientId,
                        insight = it.insight,
                        snackbarMessage = it.snackbarMessage
                    )
                }
            }
        }
        refreshInsight()
    }

    fun openSwapDialog(shift: Shift) {
        mutableState.update { it.copy(selectedShift = shift, swapReason = "", selectedRecipientId = null) }
    }

    fun dismissSwapDialog() {
        mutableState.update { it.copy(selectedShift = null, swapReason = "", selectedRecipientId = null) }
    }

    fun updateSwapReason(reason: String) {
        mutableState.update { it.copy(swapReason = reason) }
    }

    fun selectRecipient(employeeId: String?) {
        mutableState.update { it.copy(selectedRecipientId = employeeId) }
    }

    fun submitSwap(useSmartNote: Boolean) {
        val snapshot = state.value
        val shift = snapshot.selectedShift ?: return
        viewModelScope.launch {
            requestShiftSwap(
                shiftId = shift.id,
                recipientId = snapshot.selectedRecipientId,
                reason = snapshot.swapReason,
                useSmartNote = useSmartNote
            )
            mutableState.update {
                it.copy(
                    selectedShift = null,
                    swapReason = "",
                    selectedRecipientId = null,
                    snackbarMessage = "Swap request sent"
                )
            }
        }
    }

    fun claimShift(shift: Shift) {
        viewModelScope.launch {
            val result = claimOpenShift(employeeId, shift)
            mutableState.update {
                it.copy(snackbarMessage = result.fold(
                    onSuccess = { "Shift claimed" },
                    onFailure = { error -> error.message ?: "Unable to claim shift" }
                ))
            }
        }
    }

    fun refreshInsight() {
        viewModelScope.launch {
            val insight = getScheduleInsights(employeeId)
            mutableState.update { it.copy(insight = insight) }
        }
    }

    fun clearSnackbar() {
        mutableState.update { it.copy(snackbarMessage = null) }
    }
}

data class ScheduleUiState(
    val myShifts: List<Shift> = emptyList(),
    val openShifts: List<Shift> = emptyList(),
    val team: List<Employee> = emptyList(),
    val selectedShift: Shift? = null,
    val selectedRecipientId: String? = null,
    val swapReason: String = "",
    val insight: ScheduleInsight? = null,
    val snackbarMessage: String? = null
)

class MainViewModelFactory(
    private val repository: ScheduleRepository,
    private val requestShiftSwap: RequestShiftSwapUseCase,
    private val claimOpenShift: ClaimOpenShiftUseCase,
    private val getScheduleInsights: GetScheduleInsightsUseCase,
    private val employeeId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository, requestShiftSwap, claimOpenShift, getScheduleInsights, employeeId) as T
    }
}
