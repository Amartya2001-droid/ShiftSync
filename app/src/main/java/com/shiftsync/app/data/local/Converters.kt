package com.shiftsync.app.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime

class Converters {
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime): String = value.toString()

    @TypeConverter
    fun toLocalDateTime(value: String): LocalDateTime = LocalDateTime.parse(value)

    @TypeConverter
    fun fromInstant(value: Instant): String = value.toString()

    @TypeConverter
    fun toInstant(value: String): Instant = Instant.parse(value)
}
