package com.shiftsync.app.domain.model

enum class RequestHistoryFilter {
    All,
    NeedsResponse,
    Sent,
    Resolved
}

data class FilteredRequestHistory(
    val items: List<RequestHistoryItem>,
    val counts: Map<RequestHistoryFilter, Int>
)
