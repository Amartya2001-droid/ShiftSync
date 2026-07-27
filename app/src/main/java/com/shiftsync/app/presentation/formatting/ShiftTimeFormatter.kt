package com.shiftsync.app.presentation.formatting

import com.shiftsync.app.domain.model.Shift
import java.time.format.DateTimeFormatter

object ShiftTimeFormatter {
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
    private val dayFormatter = DateTimeFormatter.ofPattern("EEE d")
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    fun dateLabel(shift: Shift): String = shift.startsAt.format(dateFormatter)

    fun dayChipLabel(shift: Shift): String = shift.startsAt.format(dayFormatter)

    fun timeRange(shift: Shift): String {
        return "${shift.startsAt.format(timeFormatter)} - ${shift.endsAt.format(timeFormatter)}"
    }

    fun fullRange(shift: Shift): String = "${dateLabel(shift)}  ${timeRange(shift)}"
}
