package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.Shift
import java.time.LocalDate

class GroupRosterByDayUseCase {
    operator fun invoke(shifts: List<Shift>): Map<LocalDate, List<Shift>> {
        return shifts
            .sortedWith(compareBy(Shift::startsAt, Shift::endsAt, Shift::id))
            .groupBy { it.startsAt.toLocalDate() }
    }
}
