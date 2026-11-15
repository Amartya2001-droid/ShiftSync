package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.TradeRequest
import com.shiftsync.app.domain.repository.ScheduleRepository
import kotlinx.coroutines.CancellationException

class RespondToRequestUseCase(private val repository: ScheduleRepository) {
    suspend operator fun invoke(requestId: String, employeeId: String, accept: Boolean): Result<TradeRequest> = try {
        Result.success(repository.respondToRequest(requestId, employeeId, accept))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}
