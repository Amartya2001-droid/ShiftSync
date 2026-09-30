package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.RestaurantRole
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SummarizeOpenCoverageUseCaseTest {
    private val useCase = SummarizeOpenCoverageUseCase()

    @Test
    fun `groups open coverage by date and role`() {
        val shifts = listOf(
            shift("server-1", RestaurantRole.Server, 28),
            shift("server-2", RestaurantRole.Server, 28),
            shift("cook-1", RestaurantRole.LineCook, 29)
        )

        assertEquals(
            listOf(
                "Mon, Sep 28 needs 2 Server shifts covered",
                "Tue, Sep 29 needs 1 Line Cook shift covered"
            ),
            useCase(shifts)
        )
    }

    @Test
    fun `ignores shifts that are already covered`() {
        val covered = shift("covered", RestaurantRole.Host, 28).copy(status = ShiftStatus.Confirmed)

        assertEquals(emptyList<String>(), useCase(listOf(covered)))
    }

    private fun shift(id: String, role: RestaurantRole, day: Int): Shift {
        val start = LocalDateTime.of(2026, 9, day, 11, 0)
        return Shift(
            id = id,
            employeeId = null,
            role = role,
            section = "Dining Room",
            startsAt = start,
            endsAt = start.plusHours(6),
            status = ShiftStatus.UpForGrabs
        )
    }
}
