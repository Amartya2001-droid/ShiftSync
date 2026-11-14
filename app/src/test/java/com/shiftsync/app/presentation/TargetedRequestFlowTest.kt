package com.shiftsync.app.presentation

import androidx.lifecycle.viewModelScope
import com.shiftsync.app.data.repository.InMemoryScheduleRepository
import com.shiftsync.app.data.repository.SeedData
import com.shiftsync.app.domain.model.TradeRequestStatus
import com.shiftsync.app.domain.usecase.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TargetedRequestFlowTest {
    @Test
    fun `recipient acceptance updates both histories rosters and hours without manual refresh`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = InMemoryScheduleRepository()
        val maya = viewModel(repository, SeedData.currentEmployeeId)
        val sam = viewModel(repository, "emp-sam")
        try {
            runCurrent()
            val shift = maya.state.value.myShifts.first()
            val hours = maya.state.value.insight!!.totalHours
            maya.openSwapDialog(shift)
            maya.selectRecipient("emp-sam")
            maya.updateSwapReason("Exam")
            maya.submitSwap(false)
            runCurrent()
            val request = sam.state.value.requestHistory.single().request
            assertEquals(TradeRequestStatus.Pending, request.status)
            sam.respondToRequest(request.id, true)
            runCurrent()
            assertEquals("Request accepted", sam.state.value.snackbarMessage)
            assertEquals(TradeRequestStatus.Accepted, maya.state.value.requestHistory.single().request.status)
            assertEquals(TradeRequestStatus.Accepted, sam.state.value.requestHistory.single().request.status)
            assertFalse(maya.state.value.myShifts.any { it.id == shift.id })
            assertTrue(sam.state.value.myShifts.any { it.id == shift.id })
            assertEquals(hours - shift.hours, maya.state.value.insight!!.totalHours)
            assertEquals(shift.hours, sam.state.value.insight!!.totalHours)
        } finally {
            maya.viewModelScope.cancel()
            sam.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `pending response blocks duplicate taps and profile switches then clears busy state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val backing = InMemoryScheduleRepository()
        val request = backing.requestSwap("shift-1", "emp-sam", "Coverage", SeedData.currentEmployeeId)
        val repository = spyk(backing)
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.respondToRequest(request.id, "emp-sam", false) } coAnswers {
            gate.await()
            backing.respondToRequest(request.id, "emp-sam", false)
        }
        val sam = viewModel(repository, "emp-sam")
        try {
            runCurrent()
            sam.respondToRequest(request.id, false)
            sam.respondToRequest(request.id, false)
            sam.switchEmployee("emp-jon")
            runCurrent()
            assertEquals("emp-sam", sam.state.value.currentEmployeeId)
            assertTrue(sam.state.value.isBusy)
            coVerify(exactly = 1) { repository.respondToRequest(request.id, "emp-sam", false) }
            gate.complete(Unit)
            runCurrent()
            assertFalse(sam.state.value.isBusy)
            assertEquals("Request declined", sam.state.value.snackbarMessage)
            assertEquals(TradeRequestStatus.Rejected, sam.state.value.requestHistory.single().request.status)
            assertTrue(sam.state.value.myShifts.isEmpty())
        } finally {
            sam.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(repository: InMemoryScheduleRepository, employeeId: String) = MainViewModel(
        repository, RequestShiftSwapUseCase(repository), ClaimOpenShiftUseCase(repository),
        GetScheduleInsightsUseCase(repository), employeeId
    )
}
