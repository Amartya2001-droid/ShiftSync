package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.RestaurantRole
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PrioritizeOpenShiftMarketplaceUseCaseTest {
    private val useCase = PrioritizeOpenShiftMarketplaceUseCase()

    @Test
    fun `orders open shifts by start role and section`() {
        val later = shift("later", RestaurantRole.Server, "Patio", day = 21, hour = 10)
        val host = shift("host", RestaurantRole.Host, "Front Door", day = 20, hour = 12)
        val server = shift("server", RestaurantRole.Server, "Bar Rail", day = 20, hour = 12)

        val result = useCase(listOf(later, server, host))

        assertEquals(listOf("host", "server", "later"), result.map { it.id })
    }

    private fun shift(id: String, role: RestaurantRole, section: String, day: Int, hour: Int): Shift {
        return Shift(
            id = id,
            employeeId = null,
            role = role,
            section = section,
            startsAt = LocalDateTime.of(2026, 7, day, hour, 0),
            endsAt = LocalDateTime.of(2026, 7, day, hour + 6, 0),
            status = ShiftStatus.UpForGrabs
        )
    }
}
