package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.TradeRequest
import com.shiftsync.app.domain.repository.ScheduleRepository

class RequestShiftSwapUseCase(
    private val repository: ScheduleRepository
) {
    suspend operator fun invoke(
        shiftId: String,
        recipientId: String?,
        reason: String,
        useSmartNote: Boolean
    ): TradeRequest {
        val note = if (useSmartNote) {
            repository.generateSwapNote(reason)
        } else {
            reason
        }

        return if (recipientId == null) {
            repository.postOpenShift(shiftId, note)
        } else {
            repository.requestSwap(shiftId, recipientId, note)
        }
    }
}
