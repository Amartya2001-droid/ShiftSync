package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.EligibilityResult
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.repository.ScheduleRepository

class ClaimOpenShiftUseCase(
    private val repository: ScheduleRepository,
    private val validateTradeEligibility: ValidateTradeEligibilityUseCase
) {
    suspend operator fun invoke(employeeId: String, shift: Shift): Result<Shift> {
        return when (val eligibility = validateTradeEligibility(employeeId, shift)) {
            EligibilityResult.Eligible -> Result.success(repository.claimOpenShift(shift.id, employeeId))
            is EligibilityResult.Ineligible -> Result.failure(IllegalStateException(eligibility.reason))
        }
    }
}
