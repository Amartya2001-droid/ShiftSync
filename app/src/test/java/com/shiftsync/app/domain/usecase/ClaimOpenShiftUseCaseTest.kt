package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.RestaurantRole
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import com.shiftsync.app.domain.repository.ScheduleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ClaimOpenShiftUseCaseTest {
    private val repository = mockk<ScheduleRepository>()
    private val validate = ValidateTradeEligibilityUseCase(repository)
    private val useCase = ClaimOpenShiftUseCase(repository, validate)

    @Test
    fun `claims shift when employee is eligible`() = runTest {
        val candidate = openShift()
        val claimed = candidate.copy(employeeId = "emp-1", status = ShiftStatus.Confirmed)
        coEvery { repository.shiftsForEmployee("emp-1") } returns emptyList()
        coEvery { repository.claimOpenShift("shift-open", "emp-1") } returns claimed

        val result = useCase("emp-1", candidate)

        assertTrue(result.isSuccess)
        assertEquals(claimed, result.getOrThrow())
        coVerify { repository.claimOpenShift("shift-open", "emp-1") }
    }

    @Test
    fun `does not claim shift when employee has a conflict`() = runTest {
        val candidate = openShift()
        coEvery { repository.shiftsForEmployee("emp-1") } returns listOf(
            candidate.copy(id = "existing", employeeId = "emp-1", status = ShiftStatus.Confirmed)
        )

        val result = useCase("emp-1", candidate)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.claimOpenShift(any(), any()) }
    }

    @Test
    fun `rejects a shift that is no longer up for grabs`() = runTest {
        val result = useCase("emp-1", openShift().copy(status = ShiftStatus.PendingTrade))

        assertTrue(result.isFailure)
        assertEquals("This shift is no longer available to claim.", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.shiftsForEmployee(any()) }
        coVerify(exactly = 0) { repository.claimOpenShift(any(), any()) }
    }

    @Test
    fun `rejects an open shift that already has an employee`() = runTest {
        val result = useCase("emp-1", openShift().copy(employeeId = "emp-2"))

        assertTrue(result.isFailure)
        assertEquals("This shift has already been assigned.", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.shiftsForEmployee(any()) }
        coVerify(exactly = 0) { repository.claimOpenShift(any(), any()) }
    }

    private fun openShift(): Shift {
        return Shift(
            id = "shift-open",
            employeeId = null,
            role = RestaurantRole.Server,
            section = "Patio",
            startsAt = LocalDateTime.of(2026, 7, 20, 11, 0),
            endsAt = LocalDateTime.of(2026, 7, 20, 17, 0),
            status = ShiftStatus.UpForGrabs
        )
    }
}
