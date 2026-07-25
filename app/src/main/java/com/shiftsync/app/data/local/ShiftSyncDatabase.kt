package com.shiftsync.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ShiftEntity::class, EmployeeEntity::class, TradeRequestEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ShiftSyncDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
}
