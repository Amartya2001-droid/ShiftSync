package com.shiftsync.app.domain.model

import java.time.Duration
import java.time.LocalDateTime

data class Shift(
    val id: String,
    val employeeId: String?,
    val role: RestaurantRole,
    val section: String,
    val startsAt: LocalDateTime,
    val endsAt: LocalDateTime,
    val status: ShiftStatus
) {
    val hours: Double
        get() = Duration.between(startsAt, endsAt).toMinutes() / 60.0

    fun overlaps(other: Shift): Boolean {
        return startsAt < other.endsAt && endsAt > other.startsAt
    }
}

enum class ShiftStatus {
    Confirmed,
    PendingTrade,
    UpForGrabs
}
