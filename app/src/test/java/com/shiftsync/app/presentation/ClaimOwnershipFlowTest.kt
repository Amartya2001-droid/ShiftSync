package com.shiftsync.app.presentation

import androidx.lifecycle.viewModelScope
import com.shiftsync.app.data.repository.InMemoryScheduleRepository
import com.shiftsync.app.data.repository.SeedData
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
class ClaimOwnershipFlowTest {
    @Test
    fun `posting keeps ownership until a coworker claims then both rosters and insights refresh`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = InMemoryScheduleRepository()
        val maya = viewModel(repository, SeedData.currentEmployeeId)
        val sam = viewModel(repository, "emp-sam")
        try {
            runCurrent()
            val offered = maya.state.value.myShifts.first()
            val initialHours = maya.state.value.insight!!.totalHours
            maya.openSwapDialog(offered)
            maya.submitSwap(false)
            runCurrent()
            assertTrue(maya.state.value.myShifts.any { it.id == offered.id })
            assertEquals(SeedData.currentEmployeeId, sam.state.value.openShifts.first { it.id == offered.id }.employeeId)
            maya.clearSnackbar()
            maya.claimShift(sam.state.value.openShifts.first { it.id == offered.id })
            runCurrent()
            assertNull(maya.state.value.snackbarMessage)
            sam.claimShift(sam.state.value.openShifts.first { it.id == offered.id })
            runCurrent()
            assertEquals("Shift claimed", sam.state.value.snackbarMessage)
            assertFalse(maya.state.value.myShifts.any { it.id == offered.id })
            assertTrue(sam.state.value.myShifts.any { it.id == offered.id })
            assertFalse(sam.state.value.openShifts.any { it.id == offered.id })
            assertEquals(initialHours - offered.hours, maya.state.value.insight!!.totalHours)
            assertEquals(offered.hours, sam.state.value.insight!!.totalHours)
            maya.switchEmployee("emp-sam")
            runCurrent()
            assertEquals("emp-sam", maya.state.value.currentEmployeeId)
            assertTrue(maya.state.value.myShifts.any { it.id == offered.id })
            assertEquals(offered.hours, maya.state.value.insight!!.totalHours)
        } finally {
            maya.viewModelScope.cancel()
            sam.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `in flight claim blocks duplicate taps and employee changes`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val backing = InMemoryScheduleRepository()
        val repository = spyk(backing)
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.claimOpenShift("shift-4", "emp-sam") } coAnswers {
            gate.await()
            backing.claimOpenShift("shift-4", "emp-sam")
        }
        val viewModel = viewModel(repository, "emp-sam")
        try {
            runCurrent()
            val shift = viewModel.state.value.openShifts.first { it.id == "shift-4" }
            viewModel.claimShift(shift)
            viewModel.claimShift(shift)
            viewModel.switchEmployee("emp-jon")
            runCurrent()
            assertEquals(shift.id, viewModel.state.value.claimingShiftId)
            assertEquals("emp-sam", viewModel.state.value.currentEmployeeId)
            coVerify(exactly = 1) { repository.claimOpenShift("shift-4", "emp-sam") }
            gate.complete(Unit)
            runCurrent()
            assertFalse(viewModel.state.value.isBusy)
            assertEquals("Shift claimed", viewModel.state.value.snackbarMessage)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(repository: InMemoryScheduleRepository, employeeId: String) = MainViewModel(
        repository, RequestShiftSwapUseCase(repository), ClaimOpenShiftUseCase(repository),
        GetScheduleInsightsUseCase(repository), employeeId
    )
}
