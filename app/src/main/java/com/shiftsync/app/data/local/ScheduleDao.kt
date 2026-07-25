package com.shiftsync.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM shifts WHERE employeeId = :employeeId ORDER BY startsAt")
    fun observeShiftsForEmployee(employeeId: String): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE status = 'UpForGrabs' ORDER BY startsAt")
    fun observeOpenShifts(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM employees ORDER BY name")
    fun observeTeam(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM shifts WHERE employeeId = :employeeId")
    suspend fun shiftsForEmployee(employeeId: String): List<ShiftEntity>

    @Query("SELECT * FROM shifts WHERE id = :shiftId")
    suspend fun shiftById(shiftId: String): ShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShifts(shifts: List<ShiftEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShift(shift: ShiftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEmployees(employees: List<EmployeeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTradeRequest(request: TradeRequestEntity)
}
