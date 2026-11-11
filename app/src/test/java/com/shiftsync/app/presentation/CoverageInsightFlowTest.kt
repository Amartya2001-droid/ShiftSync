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
    fun `external schedule changes refresh coverage through empty and reopened board`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = InMemoryScheduleRepository()
        val viewModel = MainViewModel(
            repository, RequestShiftSwapUseCase(repository),
            ClaimOpenShiftUseCase(repository, ValidateTradeEligibilityUseCase(repository)),
            GetScheduleInsightsUseCase(repository), SeedData.currentEmployeeId
        )
        try {
            runCurrent()
            val initialHours = viewModel.state.value.insight!!.totalHours
            val openShifts = viewModel.state.value.openShifts
            assertEquals(2, openShifts.size)

            // Simulate changes outside this ViewModel: the repository flow must trigger refresh.
            openShifts.forEach { repository.claimOpenShift(it.id, "emp-sam") }
            runCurrent()
            assertTrue(viewModel.state.value.openShifts.isEmpty())
            assertTrue(viewModel.state.value.insight!!.coverageGaps.isEmpty())
            assertEquals(initialHours, viewModel.state.value.insight!!.totalHours)

            val reopened = openShifts.first()
            repository.postOpenShift(reopened.id, "Available again")
            runCurrent()
            assertEquals(listOf(reopened.id), viewModel.state.value.openShifts.map { it.id })
            assertEquals(1, viewModel.state.value.insight!!.coverageGaps.size)
            assertTrue(viewModel.state.value.insight!!.coverageGaps.single().contains("Host"))
            assertEquals(initialHours, viewModel.state.value.insight!!.totalHours)

            repository.claimOpenShift(reopened.id, SeedData.currentEmployeeId)
            runCurrent()
            assertTrue(viewModel.state.value.openShifts.isEmpty())
            assertTrue(viewModel.state.value.insight!!.coverageGaps.isEmpty())
            assertEquals(initialHours + reopened.hours, viewModel.state.value.insight!!.totalHours)
            assertTrue(viewModel.state.value.myShifts.any { it.id == reopened.id })
            assertNull(viewModel.state.value.snackbarMessage)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

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
            val initialHours = viewModel.state.value.insight!!.totalHours
            viewModel.claimShift(open)
            runCurrent()
            assertEquals("Shift claimed", viewModel.state.value.snackbarMessage)
            assertEquals(1, viewModel.state.value.insight!!.coverageGaps.size)
            assertEquals(initialHours + open.hours, viewModel.state.value.insight!!.totalHours)
            assertTrue(viewModel.state.value.insight!!.coverageGaps.none { it.contains("Host") })
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
