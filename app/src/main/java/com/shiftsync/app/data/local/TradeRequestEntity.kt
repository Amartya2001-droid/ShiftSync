package com.shiftsync.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shiftsync.app.domain.model.TradeRequest
import com.shiftsync.app.domain.model.TradeRequestStatus
import java.time.Instant

@Entity(tableName = "trade_requests")
data class TradeRequestEntity(
    @PrimaryKey val id: String,
    val shiftId: String,
    val requesterId: String,
    val recipientId: String?,
    val note: String,
    val status: TradeRequestStatus,
    val createdAt: Instant
) {
    fun toDomain(): TradeRequest = TradeRequest(id, shiftId, requesterId, recipientId, note, status, createdAt)
}

fun TradeRequest.toEntity(): TradeRequestEntity = TradeRequestEntity(
    id = id,
    shiftId = shiftId,
    requesterId = requesterId,
    recipientId = recipientId,
    note = note,
    status = status,
    createdAt = createdAt
)
