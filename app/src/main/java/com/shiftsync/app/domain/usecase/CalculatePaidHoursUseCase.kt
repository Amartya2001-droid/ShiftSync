package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.Shift

class CalculatePaidHoursUseCase {
    operator fun invoke(shift: Shift, unpaidBreakMinutes: Int = 0): Double {
        require(unpaidBreakMinutes >= 0) { "Unpaid break minutes cannot be negative." }

        val breakHours = unpaidBreakMinutes / MINUTES_PER_HOUR
        return (shift.hours - breakHours).coerceAtLeast(0.0)
    }

    private companion object {
        const val MINUTES_PER_HOUR = 60.0
    }
}
