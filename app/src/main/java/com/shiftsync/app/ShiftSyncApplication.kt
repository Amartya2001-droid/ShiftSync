package com.shiftsync.app

import android.app.Application
import androidx.room.Room
import com.shiftsync.app.data.local.ShiftSyncDatabase
import com.shiftsync.app.data.repository.OfflineFirstScheduleRepository

class ShiftSyncApplication : Application() {
    private val database by lazy {
        Room.databaseBuilder(this, ShiftSyncDatabase::class.java, "shiftsync.db").build()
    }

    val repository by lazy { OfflineFirstScheduleRepository(database) }
}
