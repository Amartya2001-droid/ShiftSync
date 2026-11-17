package com.shiftsync.app.presentation

import androidx.lifecycle.viewModelScope
import com.shiftsync.app.data.repository.InMemoryScheduleRepository
import com.shiftsync.app.data.repository.SeedData
import com.shiftsync.app.domain.model.TradeRequestStatus
import com.shiftsync.app.domain.usecase.*
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RequestResponseConfirmationTest {
    @Test
    fun `opening and dismissing either response prompt performs no write`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = spyk(InMemoryScheduleRepository())
        val request = repository.requestSwap("shift-1", "emp-sam", "Exam tomorrow", SeedData.currentEmployeeId)
        val sam = viewModel(repository, "emp-sam")
        try {
            runCurrent()
            for (accept in listOf(true, false)) {
                sam.requestResponseConfirmation(request.id, accept)
                assertEquals(accept, sam.state.value.responseConfirmation!!.accept)
                assertEquals("Maya Chen", sam.state.value.responseConfirmation!!.item.requesterName)
                assertEquals("Patio", sam.state.value.responseConfirmation!!.item.shift!!.section)
                assertEquals("Exam tomorrow", sam.state.value.responseConfirmation!!.item.request.note)
                sam.dismissResponseConfirmation()
                runCurrent()
                assertNull(sam.state.value.responseConfirmation)
            }
            coVerify(exactly = 0) { repository.respondToRequest(any(), any(), any()) }
            assertEquals(TradeRequestStatus.Pending, sam.state.value.requestHistory.single().request.status)
            assertTrue(repository.shiftsForEmployee(SeedData.currentEmployeeId).any { it.id == "shift-1" })
        } finally {
            sam.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `confirming acceptance writes once and transfers ownership`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = spyk(InMemoryScheduleRepository())
        val request = repository.requestSwap("shift-1", "emp-sam", "Coverage", SeedData.currentEmployeeId)
        val sam = viewModel(repository, "emp-sam")
        try {
            runCurrent()
            sam.requestResponseConfirmation(request.id, true)
            coVerify(exactly = 0) { repository.respondToRequest(any(), any(), any()) }
            sam.confirmRequestResponse()
            sam.confirmRequestResponse()
            runCurrent()
            coVerify(exactly = 1) { repository.respondToRequest(request.id, "emp-sam", true) }
            assertNull(sam.state.value.responseConfirmation)
            assertEquals(TradeRequestStatus.Accepted, sam.state.value.requestHistory.single().request.status)
            assertTrue(sam.state.value.myShifts.any { it.id == "shift-1" })
        } finally {
            sam.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `confirmed decline retains sender ownership`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = spyk(InMemoryScheduleRepository())
        val request = repository.requestSwap("shift-1", "emp-sam", "Coverage", SeedData.currentEmployeeId)
        val sam = viewModel(repository, "emp-sam")
        try {
            runCurrent()
            sam.requestResponseConfirmation(request.id, false)
            sam.confirmRequestResponse()
            runCurrent()
            coVerify(exactly = 1) { repository.respondToRequest(request.id, "emp-sam", false) }
            assertEquals(TradeRequestStatus.Rejected, sam.state.value.requestHistory.single().request.status)
            assertTrue(sam.state.value.myShifts.isEmpty())
            assertTrue(repository.shiftsForEmployee(SeedData.currentEmployeeId).any { it.id == "shift-1" })
        } finally {
            sam.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `a withdrawal while the prompt is open clears it without a response write`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = spyk(InMemoryScheduleRepository())
        val request = repository.requestSwap("shift-1", "emp-sam", "Coverage", SeedData.currentEmployeeId)
        val sam = viewModel(repository, "emp-sam")
        try {
            runCurrent()
            sam.requestResponseConfirmation(request.id, true)
            repository.cancelRequest(request.id, SeedData.currentEmployeeId)
            runCurrent()
            assertNull(sam.state.value.responseConfirmation)
            sam.confirmRequestResponse()
            runCurrent()
            coVerify(exactly = 0) { repository.respondToRequest(any(), any(), any()) }
            assertEquals(TradeRequestStatus.Cancelled, sam.state.value.requestHistory.single().request.status)
        } finally {
            sam.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `changing demo employee dismisses the prompt and sender cannot reopen it`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = spyk(InMemoryScheduleRepository())
        val request = repository.requestSwap("shift-1", "emp-sam", "Coverage", SeedData.currentEmployeeId)
        val sam = viewModel(repository, "emp-sam")
        try {
            runCurrent()
            sam.requestResponseConfirmation(request.id, false)
            sam.switchEmployee(SeedData.currentEmployeeId)
            runCurrent()
            assertNull(sam.state.value.responseConfirmation)
            sam.confirmRequestResponse()
            sam.requestResponseConfirmation(request.id, true)
            runCurrent()
            assertNull(sam.state.value.responseConfirmation)
            coVerify(exactly = 0) { repository.respondToRequest(any(), any(), any()) }
            assertEquals(TradeRequestStatus.Pending, sam.state.value.requestHistory.single().request.status)
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
