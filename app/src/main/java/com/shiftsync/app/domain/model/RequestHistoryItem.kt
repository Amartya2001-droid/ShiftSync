package com.shiftsync.app.domain.model

data class RequestHistoryItem(
    val request: TradeRequest,
    val shift: Shift?,
    val requesterName: String,
    val recipientName: String?
)
