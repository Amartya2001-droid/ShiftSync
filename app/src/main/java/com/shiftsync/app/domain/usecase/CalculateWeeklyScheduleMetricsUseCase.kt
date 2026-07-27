package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.Shift

class CalculateWeeklyScheduleMetricsUseCase {
    operator fun invoke(shifts: List<Shift>, hourlyRateCents: Int): WeeklyScheduleMetrics {
        val totalHours = shifts.sumOf { it.hours }
        val dinnerShiftCount = shifts.count { it.startsAt.hour >= DINNER_START_HOUR }
        val estimatedEarningsCents = (totalHours * hourlyRateCents).toInt()

        return WeeklyScheduleMetrics(
            totalHours = totalHours,
            dinnerShiftCount = dinnerShiftCount,
            estimatedEarningsCents = estimatedEarningsCents
        )
    }

    private companion object {
        const val DINNER_START_HOUR = 16
    }
}

data class WeeklyScheduleMetrics(
    val totalHours: Double,
    val dinnerShiftCount: Int,
    val estimatedEarningsCents: Int
)
