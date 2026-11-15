package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.*

class BuildRequestHistoryUseCase {
    operator fun invoke(employeeId: String, requests: List<TradeRequest>, shifts: List<Shift>, team: List<Employee>): List<RequestHistoryItem> {
        val shiftsById = shifts.associateBy { it.id }
        val employeesById = team.associateBy { it.id }
        return requests.filter { it.requesterId == employeeId || it.recipientId == employeeId }
            .sortedWith(compareByDescending<TradeRequest> { it.createdAt }.thenBy { it.id })
            .map { request ->
                RequestHistoryItem(request, shiftsById[request.shiftId],
                    employeesById[request.requesterId]?.name ?: "Unknown employee",
                    request.recipientId?.let { employeesById[it]?.name ?: "Unknown employee" })
            }
    }
}
