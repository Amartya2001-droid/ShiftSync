package com.shiftsync.app.domain.usecase

import com.shiftsync.app.data.repository.SeedData
import com.shiftsync.app.domain.model.ShiftStatus
import com.shiftsync.app.domain.repository.ScheduleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ClaimOpenShiftUseCaseTest {
    private val repository = mockk<ScheduleRepository>()
    private val useCase = ClaimOpenShiftUseCase(repository)

    @Test
    fun `delegates by id so the repository validates saved ownership instead of a UI snapshot`() = runTest {
        val stale = SeedData.shifts().first().copy(status = ShiftStatus.Confirmed)
        val claimed = stale.copy(employeeId = "emp-sam")
        coEvery { repository.claimOpenShift(stale.id, "emp-sam") } returns claimed
        assertEquals(claimed, useCase("emp-sam", stale).getOrThrow())
        coVerify(exactly = 1) { repository.claimOpenShift(stale.id, "emp-sam") }
        coVerify(exactly = 0) { repository.shiftsForEmployee(any()) }
    }

    @Test
    fun `returns the authoritative rejection without reporting success`() = runTest {
        val shift = SeedData.shifts().first()
        coEvery { repository.claimOpenShift(shift.id, "emp-sam") } throws IllegalStateException("No longer available")
        val result = useCase("emp-sam", shift)
        assertTrue(result.isFailure)
        assertEquals("No longer available", result.exceptionOrNull()?.message)
    }

    @Test
    fun `does not swallow coroutine cancellation`() = runTest {
        val shift = SeedData.shifts().first()
        coEvery { repository.claimOpenShift(shift.id, "emp-sam") } throws CancellationException()
        try {
            useCase("emp-sam", shift)
            fail<Unit>("Expected cancellation")
        } catch (_: CancellationException) {
            // Cancellation must propagate to the ViewModel job.
        }
    }
}
