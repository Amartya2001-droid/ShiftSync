package com.shiftsync.app.domain.model

import java.time.Instant

data class TradeRequest(
    val id: String,
    val shiftId: String,
    val requesterId: String,
    val recipientId: String?,
    val note: String,
    val status: TradeRequestStatus,
    val createdAt: Instant
)

enum class TradeRequestStatus {
    Pending,
    Accepted,
    Rejected
}
