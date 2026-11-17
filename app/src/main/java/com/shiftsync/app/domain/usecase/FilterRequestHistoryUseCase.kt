package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.*

class FilterRequestHistoryUseCase {
    operator fun invoke(employeeId: String, history: List<RequestHistoryItem>, filter: RequestHistoryFilter): FilteredRequestHistory {
        // Filter only the current participant's history, retaining its existing chronological order.
        val scoped = history.filter { it.request.requesterId == employeeId || it.request.recipientId == employeeId }
        fun matches(item: RequestHistoryItem, selected: RequestHistoryFilter): Boolean = when (selected) {
            RequestHistoryFilter.All -> true
            RequestHistoryFilter.NeedsResponse -> item.request.status == TradeRequestStatus.Pending &&
                item.request.recipientId == employeeId && item.request.requesterId != employeeId
            RequestHistoryFilter.Sent -> item.request.requesterId == employeeId
            RequestHistoryFilter.Resolved -> item.request.status != TradeRequestStatus.Pending
        }
        return FilteredRequestHistory(
            items = scoped.filter { matches(it, filter) },
            counts = RequestHistoryFilter.values().associateWith { selected -> scoped.count { matches(it, selected) } }
        )
    }
}
