package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.EligibilityResult
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import com.shiftsync.app.domain.repository.ScheduleRepository

class ClaimOpenShiftUseCase(
    private val repository: ScheduleRepository,
    private val validateTradeEligibility: ValidateTradeEligibilityUseCase
) {
    suspend operator fun invoke(employeeId: String, shift: Shift): Result<Shift> {
        if (shift.status != ShiftStatus.UpForGrabs) {
            return Result.failure(IllegalStateException("This shift is no longer available to claim."))
        }
        if (shift.employeeId != null) {
            return Result.failure(IllegalStateException("This shift has already been assigned."))
        }

        return when (val eligibility = validateTradeEligibility(employeeId, shift)) {
            EligibilityResult.Eligible -> Result.success(repository.claimOpenShift(shift.id, employeeId))
            is EligibilityResult.Ineligible -> Result.failure(IllegalStateException(eligibility.reason))
        }
    }
}
