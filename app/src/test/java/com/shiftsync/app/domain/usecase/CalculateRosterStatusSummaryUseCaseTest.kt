package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.RestaurantRole
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class CalculateRosterStatusSummaryUseCaseTest {
    private val useCase = CalculateRosterStatusSummaryUseCase()

    @Test
    fun `counts roster shifts by status`() {
        val result = useCase(
            listOf(
                shift("confirmed", ShiftStatus.Confirmed),
                shift("pending", ShiftStatus.PendingTrade),
                shift("open", ShiftStatus.UpForGrabs),
                shift("confirmed-2", ShiftStatus.Confirmed)
            )
        )

        assertEquals(RosterStatusSummary(confirmed = 2, pendingTrade = 1, upForGrabs = 1), result)
    }

    private fun shift(id: String, status: ShiftStatus): Shift {
        return Shift(
            id = id,
            employeeId = "emp-1",
            role = RestaurantRole.Server,
            section = "Patio",
            startsAt = LocalDateTime.of(2026, 7, 20, 11, 0),
            endsAt = LocalDateTime.of(2026, 7, 20, 17, 0),
            status = status
        )
    }
}
