package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.EligibilityResult
import com.shiftsync.app.domain.model.RestaurantRole
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import com.shiftsync.app.domain.repository.ScheduleRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ValidateTradeEligibilityUseCaseTest {
    private val repository = mockk<ScheduleRepository>()
    private val useCase = ValidateTradeEligibilityUseCase(repository, maxWeeklyHours = 12.0)

    @Test
    fun `returns ineligible when candidate overlaps an existing shift`() = runTest {
        val existing = shift("existing", startHour = 10, endHour = 16)
        val candidate = shift("candidate", startHour = 15, endHour = 20, employeeId = null)
        coEvery { repository.shiftsForEmployee("emp-1") } returns listOf(existing)

        val result = useCase("emp-1", candidate)

        assertTrue(result is EligibilityResult.Ineligible)
        assertTrue((result as EligibilityResult.Ineligible).reason.contains("overlaps"))
    }

    @Test
    fun `returns ineligible when candidate exceeds max weekly hours`() = runTest {
        val existing = shift("existing", startHour = 8, endHour = 16)
        val candidate = shift("candidate", startHour = 0, endHour = 6, employeeId = null, dayOffset = 1)
        coEvery { repository.shiftsForEmployee("emp-1") } returns listOf(existing)

        val result = useCase("emp-1", candidate)

        assertEquals(
            EligibilityResult.Ineligible("Claiming this shift would exceed 12.0 weekly hours."),
            result
        )
    }

    @Test
    fun `returns ineligible when candidate leaves too little rest`() = runTest {
        val existing = shift("existing", startHour = 10, endHour = 16)
        val candidate = shift("candidate", startHour = 22, endHour = 23, employeeId = null)
        coEvery { repository.shiftsForEmployee("emp-1") } returns listOf(existing)

        val result = useCase("emp-1", candidate)

        assertTrue(result is EligibilityResult.Ineligible)
        assertTrue((result as EligibilityResult.Ineligible).reason.contains("less than 8 hours of rest"))
    }

    @Test
    fun `returns eligible when there is no overlap and weekly hours are valid`() = runTest {
        val existing = shift("existing", startHour = 8, endHour = 12)
        val candidate = shift("candidate", startHour = 21, endHour = 23, employeeId = null)
        coEvery { repository.shiftsForEmployee("emp-1") } returns listOf(existing)

        val result = useCase("emp-1", candidate)

        assertEquals(EligibilityResult.Eligible, result)
    }

    private fun shift(
        id: String,
        startHour: Int,
        endHour: Int,
        employeeId: String? = "emp-1",
        dayOffset: Long = 0
    ): Shift {
        val day = LocalDateTime.of(2026, 7, 20, 0, 0).plusDays(dayOffset)
        return Shift(
            id = id,
            employeeId = employeeId,
            role = RestaurantRole.Server,
            section = "Patio",
            startsAt = day.withHour(startHour),
            endsAt = day.withHour(endHour),
            status = ShiftStatus.Confirmed
        )
    }
}
