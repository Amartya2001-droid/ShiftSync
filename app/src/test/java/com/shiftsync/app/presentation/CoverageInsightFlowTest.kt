package com.shiftsync.app.presentation

import com.shiftsync.app.data.repository.InMemoryScheduleRepository
import com.shiftsync.app.data.repository.SeedData
import com.shiftsync.app.domain.usecase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.*
import androidx.lifecycle.viewModelScope
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CoverageInsightFlowTest {
    @Test
    fun `coverage updates after claiming and posting without manual refresh`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = InMemoryScheduleRepository()
        val viewModel = MainViewModel(
            repository, RequestShiftSwapUseCase(repository),
            ClaimOpenShiftUseCase(repository, ValidateTradeEligibilityUseCase(repository)),
            GetScheduleInsightsUseCase(repository), SeedData.currentEmployeeId
        )
        try {
            runCurrent()
            assertEquals(2, viewModel.state.value.insight!!.coverageGaps.size)
            val open = viewModel.state.value.openShifts.first()
            viewModel.claimShift(open)
            runCurrent()
            assertEquals("Shift claimed", viewModel.state.value.snackbarMessage)
            assertEquals(1, viewModel.state.value.insight!!.coverageGaps.size)
            viewModel.openSwapDialog(open)
            viewModel.submitSwap(false)
            runCurrent()
            assertEquals(2, viewModel.state.value.insight!!.coverageGaps.size)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }
}
