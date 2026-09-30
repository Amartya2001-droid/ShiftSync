package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.RestaurantRole
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class CalculatePaidHoursUseCaseTest {
    private val useCase = CalculatePaidHoursUseCase()

    @Test
    fun `subtracts an unpaid break from shift hours`() {
        assertEquals(7.5, useCase(shift(hours = 8), unpaidBreakMinutes = 30))
    }

    @Test
    fun `never returns negative paid hours`() {
        assertEquals(0.0, useCase(shift(hours = 1), unpaidBreakMinutes = 90))
    }

    @Test
    fun `rejects a negative break duration`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase(shift(hours = 8), unpaidBreakMinutes = -1)
        }
    }

    private fun shift(hours: Long): Shift {
        val start = LocalDateTime.of(2026, 9, 28, 9, 0)
        return Shift(
            id = "shift-1",
            employeeId = "emp-1",
            role = RestaurantRole.Server,
            section = "Dining Room",
            startsAt = start,
            endsAt = start.plusHours(hours),
            status = ShiftStatus.Confirmed
        )
    }
}
