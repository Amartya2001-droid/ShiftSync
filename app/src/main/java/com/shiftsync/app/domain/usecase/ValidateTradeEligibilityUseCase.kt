package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.EligibilityResult
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.repository.ScheduleRepository
import java.time.Duration

class ValidateTradeEligibilityUseCase(
    private val repository: ScheduleRepository,
    private val maxWeeklyHours: Double = 40.0,
    private val minimumRestHours: Long = 8
) {
    suspend operator fun invoke(employeeId: String, candidateShift: Shift): EligibilityResult {
        val existingShifts = repository.shiftsForEmployee(employeeId)
        val overlappingShift = existingShifts.firstOrNull { it.overlaps(candidateShift) }
        if (overlappingShift != null) {
            return EligibilityResult.Ineligible("This shift overlaps with ${overlappingShift.section}.")
        }

        val tightTurnaround = existingShifts.firstOrNull { it.restHoursBetween(candidateShift) < minimumRestHours }
        if (tightTurnaround != null) {
            return EligibilityResult.Ineligible("This claim leaves less than $minimumRestHours hours of rest after ${tightTurnaround.section}.")
        }

        val sameWeekHours = existingShifts
            .filter { it.startsAt.toLocalDate().with(java.time.DayOfWeek.MONDAY) == candidateShift.startsAt.toLocalDate().with(java.time.DayOfWeek.MONDAY) }
            .sumOf { it.hours } + candidateShift.hours

        if (sameWeekHours > maxWeeklyHours) {
            return EligibilityResult.Ineligible("Claiming this shift would exceed $maxWeeklyHours weekly hours.")
        }

        return EligibilityResult.Eligible
    }

    private fun Shift.restHoursBetween(other: Shift): Long {
        val rest = when {
            endsAt <= other.startsAt -> Duration.between(endsAt, other.startsAt)
            other.endsAt <= startsAt -> Duration.between(other.endsAt, startsAt)
            else -> Duration.ofHours(Long.MAX_VALUE)
        }
        return rest.toHours()
    }
}
