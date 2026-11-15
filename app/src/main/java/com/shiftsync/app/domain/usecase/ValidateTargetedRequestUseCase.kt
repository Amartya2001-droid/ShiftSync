package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.*

class ValidateTargetedRequestUseCase {
    operator fun invoke(request: TradeRequest, shift: Shift, employeeId: String) {
        check(request.status == TradeRequestStatus.Pending) { "This request has already been resolved." }
        check(request.recipientId == employeeId && request.requesterId != employeeId) { "Only the named recipient can respond." }
        check(shift.employeeId == request.requesterId && shift.status == ShiftStatus.PendingTrade) {
            "This offer is no longer available."
        }
    }
}
