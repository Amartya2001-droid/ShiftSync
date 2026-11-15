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
class RequestCancellationFlowTest {
    @Test
    fun `sender withdrawal updates both histories and leaves the shift on the sender roster`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = InMemoryScheduleRepository()
        val maya = viewModel(repository, SeedData.currentEmployeeId)
        val sam = viewModel(repository, "emp-sam")
        try {
            runCurrent()
            val shift = maya.state.value.myShifts.first()
            maya.openSwapDialog(shift)
            maya.selectRecipient("emp-sam")
            maya.submitSwap(false)
            runCurrent()
            val request = sam.state.value.requestHistory.single().request
            sam.cancelRequest(request.id)
            runCurrent()
            assertEquals(TradeRequestStatus.Pending, sam.state.value.requestHistory.single().request.status)
            assertFalse(sam.state.value.isBusy)
            assertTrue(sam.state.value.snackbarMessage!!.contains("Only the current sender"))
            maya.cancelRequest(request.id)
            runCurrent()
            assertEquals("Offer withdrawn", maya.state.value.snackbarMessage)
            assertEquals(TradeRequestStatus.Cancelled, maya.state.value.requestHistory.single().request.status)
            assertEquals(TradeRequestStatus.Cancelled, sam.state.value.requestHistory.single().request.status)
            assertTrue(maya.state.value.myShifts.any { it.id == shift.id })
            assertTrue(sam.state.value.myShifts.isEmpty())
            sam.respondToRequest(request.id, true)
            runCurrent()
            assertTrue(sam.state.value.snackbarMessage!!.contains("already been resolved"))
        } finally {
            maya.viewModelScope.cancel()
            sam.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `open board withdrawal refreshes coverage without changing hours`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = InMemoryScheduleRepository()
        val maya = viewModel(repository, SeedData.currentEmployeeId)
        try {
            runCurrent()
            val initial = maya.state.value.insight!!
            val shift = maya.state.value.myShifts.first()
            maya.openSwapDialog(shift)
            maya.submitSwap(false)
            runCurrent()
            assertEquals(initial.coverageGaps.size + 1, maya.state.value.insight!!.coverageGaps.size)
            maya.cancelRequest(maya.state.value.requestHistory.single().request.id)
            runCurrent()
            assertFalse(maya.state.value.openShifts.any { it.id == shift.id })
            assertEquals(initial.coverageGaps, maya.state.value.insight!!.coverageGaps)
            assertEquals(initial.totalHours, maya.state.value.insight!!.totalHours)
        } finally {
            maya.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `withdrawal blocks duplicate taps and profile changes until the write completes`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val backing = InMemoryScheduleRepository()
        val request = backing.postOpenShift("shift-1", "Coverage", SeedData.currentEmployeeId)
        val repository = spyk(backing)
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.cancelRequest(request.id, SeedData.currentEmployeeId) } coAnswers {
            gate.await()
            backing.cancelRequest(request.id, SeedData.currentEmployeeId)
        }
        val maya = viewModel(repository, SeedData.currentEmployeeId)
        try {
            runCurrent()
            maya.cancelRequest(request.id)
            maya.cancelRequest(request.id)
            maya.switchEmployee("emp-sam")
            runCurrent()
            assertEquals(SeedData.currentEmployeeId, maya.state.value.currentEmployeeId)
            assertTrue(maya.state.value.isBusy)
            coVerify(exactly = 1) { repository.cancelRequest(request.id, SeedData.currentEmployeeId) }
            gate.complete(Unit)
            runCurrent()
            assertFalse(maya.state.value.isBusy)
            assertEquals(TradeRequestStatus.Cancelled, maya.state.value.requestHistory.single().request.status)
        } finally {
            maya.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(repository: InMemoryScheduleRepository, employeeId: String) = MainViewModel(
        repository, RequestShiftSwapUseCase(repository), ClaimOpenShiftUseCase(repository),
        GetScheduleInsightsUseCase(repository), employeeId
    )
}
