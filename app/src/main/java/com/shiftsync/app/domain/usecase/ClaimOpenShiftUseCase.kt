package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.repository.ScheduleRepository
import kotlinx.coroutines.CancellationException

class ClaimOpenShiftUseCase(private val repository: ScheduleRepository) {
    suspend operator fun invoke(employeeId: String, shift: Shift): Result<Shift> = try {
        // Only the id crosses this boundary. The repository validates current data atomically.
        Result.success(repository.claimOpenShift(shift.id, employeeId))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}
