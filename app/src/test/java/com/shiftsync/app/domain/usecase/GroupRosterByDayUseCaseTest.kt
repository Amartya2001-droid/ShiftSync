package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.RestaurantRole
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class GroupRosterByDayUseCaseTest {
    private val useCase = GroupRosterByDayUseCase()

    @Test
    fun `groups shifts by date in chronological order`() {
        val tuesdayDinner = shift("tuesday-dinner", 29, 17)
        val mondayDinner = shift("monday-dinner", 28, 17)
        val mondayLunch = shift("monday-lunch", 28, 11)

        val result = useCase(listOf(tuesdayDinner, mondayDinner, mondayLunch))

        assertEquals(
            listOf(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 29)),
            result.keys.toList()
        )
        assertEquals(listOf("monday-lunch", "monday-dinner"), result.values.first().map { it.id })
    }

    @Test
    fun `returns an empty roster for no shifts`() {
        assertEquals(emptyMap<LocalDate, List<Shift>>(), useCase(emptyList()))
    }

    private fun shift(id: String, day: Int, startHour: Int): Shift {
        val start = LocalDateTime.of(2026, 9, day, startHour, 0)
        return Shift(
            id = id,
            employeeId = "emp-1",
            role = RestaurantRole.Server,
            section = "Dining Room",
            startsAt = start,
            endsAt = start.plusHours(6),
            status = ShiftStatus.Confirmed
        )
    }
}
