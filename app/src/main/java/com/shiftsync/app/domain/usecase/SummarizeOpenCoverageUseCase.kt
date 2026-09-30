package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import java.time.format.DateTimeFormatter
import java.util.Locale

class SummarizeOpenCoverageUseCase {
    operator fun invoke(shifts: List<Shift>): List<String> {
        return shifts
            .asSequence()
            .filter { it.status == ShiftStatus.UpForGrabs }
            .groupBy { it.startsAt.toLocalDate() to it.role }
            .toSortedMap(compareBy({ it.first }, { it.second.name }))
            .map { (dateAndRole, openShifts) ->
                val (date, role) = dateAndRole
                val roleLabel = role.name.replace(LOWERCASE_TO_UPPERCASE, "\$1 \$2")
                val shiftLabel = if (openShifts.size == 1) "shift" else "shifts"
                "${date.format(DATE_FORMAT)} needs ${openShifts.size} $roleLabel $shiftLabel covered"
            }
    }

    private companion object {
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
        val LOWERCASE_TO_UPPERCASE = Regex("([a-z])([A-Z])")
    }
}
