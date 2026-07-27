package com.shiftsync.app.presentation.formatting

import com.shiftsync.app.domain.model.RestaurantRole
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ShiftTimeFormatterTest {
    @Test
    fun `formats full shift range for cards`() {
        val shift = Shift(
            id = "shift-1",
            employeeId = "emp-1",
            role = RestaurantRole.Server,
            section = "Patio",
            startsAt = LocalDateTime.of(2026, 7, 20, 11, 0),
            endsAt = LocalDateTime.of(2026, 7, 20, 17, 30),
            status = ShiftStatus.Confirmed
        )

        assertEquals("Mon, Jul 20  11:00 AM - 5:30 PM", ShiftTimeFormatter.fullRange(shift))
    }
}
