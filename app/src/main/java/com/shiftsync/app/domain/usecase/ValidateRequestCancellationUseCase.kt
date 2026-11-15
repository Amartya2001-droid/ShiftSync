package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.*

class ValidateRequestCancellationUseCase {
    operator fun invoke(request: TradeRequest, shift: Shift, employeeId: String) {
        check(request.status == TradeRequestStatus.Pending) { "This request has already been resolved." }
        check(request.requesterId == employeeId && shift.employeeId == employeeId) {
            "Only the current sender can withdraw this offer."
        }
        val expectedStatus = if (request.recipientId == null) ShiftStatus.UpForGrabs else ShiftStatus.PendingTrade
        check(shift.status == expectedStatus) { "This offer is no longer available." }
    }
}
