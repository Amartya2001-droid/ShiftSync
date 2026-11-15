package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.TradeRequest
import com.shiftsync.app.domain.repository.ScheduleRepository
import kotlinx.coroutines.CancellationException

class CancelRequestUseCase(private val repository: ScheduleRepository) {
    suspend operator fun invoke(requestId: String, employeeId: String): Result<TradeRequest> = try {
        Result.success(repository.cancelRequest(requestId, employeeId))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}
