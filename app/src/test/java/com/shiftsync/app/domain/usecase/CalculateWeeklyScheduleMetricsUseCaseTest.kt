package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.RestaurantRole
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class CalculateWeeklyScheduleMetricsUseCaseTest {
    private val useCase = CalculateWeeklyScheduleMetricsUseCase()

    @Test
    fun `calculates total hours dinner shifts and estimated earnings`() {
        val shifts = listOf(
            shift("lunch", startHour = 10, endHour = 16),
            shift("dinner", startHour = 17, endHour = 23)
        )

        val result = useCase(shifts, hourlyRateCents = 1850)

        assertEquals(12.0, result.totalHours)
        assertEquals(1, result.dinnerShiftCount)
        assertEquals(22200, result.estimatedEarningsCents)
    }

    private fun shift(id: String, startHour: Int, endHour: Int): Shift {
        val day = LocalDateTime.of(2026, 7, 20, 0, 0)
        return Shift(
            id = id,
            employeeId = "emp-1",
            role = RestaurantRole.Server,
            section = "Patio",
            startsAt = day.withHour(startHour),
            endsAt = day.withHour(endHour),
            status = ShiftStatus.Confirmed
        )
    }
}
