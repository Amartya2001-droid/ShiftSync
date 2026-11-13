package com.shiftsync.app.presentation

import androidx.lifecycle.viewModelScope
import com.shiftsync.app.data.repository.InMemoryScheduleRepository
import com.shiftsync.app.data.repository.SeedData
import com.shiftsync.app.domain.usecase.*
import io.mockk.coEvery
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ScheduleStorageStateTest {
    @Test
    fun `startup failure offers retry without replacing the repository`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = spyk(InMemoryScheduleRepository())
        var attempts = 0
        coEvery { repository.initialize() } coAnswers {
            if (attempts++ == 0) throw IOException("Storage unavailable")
        }
        val viewModel = viewModel(repository)
        try {
            assertTrue(viewModel.state.value.isLoading)
            runCurrent()
            assertFalse(viewModel.state.value.isLoading)
            assertNotNull(viewModel.state.value.loadError)
            viewModel.loadSchedule()
            runCurrent()
            assertNull(viewModel.state.value.loadError)
            assertFalse(viewModel.state.value.isLoading)
            assertEquals(3, viewModel.state.value.myShifts.size)
            assertNotNull(viewModel.state.value.insight)
            assertEquals(2, attempts)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `failed save preserves swap input for retry`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = spyk(InMemoryScheduleRepository())
        coEvery { repository.postOpenShift(any(), any()) } throws IOException("Disk full")
        val viewModel = viewModel(repository)
        try {
            runCurrent()
            val shift = viewModel.state.value.myShifts.first()
            viewModel.openSwapDialog(shift)
            viewModel.updateSwapReason("Exam tomorrow")
            viewModel.submitSwap(false)
            runCurrent()
            assertEquals(shift, viewModel.state.value.selectedShift)
            assertEquals("Exam tomorrow", viewModel.state.value.swapReason)
            assertEquals("Unable to save the swap request. Please retry.", viewModel.state.value.snackbarMessage)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(repository: InMemoryScheduleRepository) = MainViewModel(
        repository, RequestShiftSwapUseCase(repository),
        ClaimOpenShiftUseCase(repository, ValidateTradeEligibilityUseCase(repository)),
        GetScheduleInsightsUseCase(repository), SeedData.currentEmployeeId
    )
}
