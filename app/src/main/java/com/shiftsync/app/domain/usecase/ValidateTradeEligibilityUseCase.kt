package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.EligibilityResult
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.repository.ScheduleRepository

class ValidateTradeEligibilityUseCase(
    private val repository: ScheduleRepository,
    private val maxWeeklyHours: Double = 40.0
) {
    suspend operator fun invoke(employeeId: String, candidateShift: Shift): EligibilityResult {
        val existingShifts = repository.shiftsForEmployee(employeeId)
        val overlappingShift = existingShifts.firstOrNull { it.overlaps(candidateShift) }
        if (overlappingShift != null) {
            return EligibilityResult.Ineligible("This shift overlaps with ${overlappingShift.section}.")
        }

        val sameWeekHours = existingShifts
            .filter { it.startsAt.toLocalDate().with(java.time.DayOfWeek.MONDAY) == candidateShift.startsAt.toLocalDate().with(java.time.DayOfWeek.MONDAY) }
            .sumOf { it.hours } + candidateShift.hours

        if (sameWeekHours > maxWeeklyHours) {
            return EligibilityResult.Ineligible("Claiming this shift would exceed $maxWeeklyHours weekly hours.")
        }

        return EligibilityResult.Eligible
    }
}
