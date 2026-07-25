package com.shiftsync.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shiftsync.app.domain.model.RestaurantRole
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import java.time.LocalDateTime

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val id: String,
    val employeeId: String?,
    val role: RestaurantRole,
    val section: String,
    val startsAt: LocalDateTime,
    val endsAt: LocalDateTime,
    val status: ShiftStatus
) {
    fun toDomain(): Shift = Shift(id, employeeId, role, section, startsAt, endsAt, status)
}

fun Shift.toEntity(): ShiftEntity = ShiftEntity(id, employeeId, role, section, startsAt, endsAt, status)
