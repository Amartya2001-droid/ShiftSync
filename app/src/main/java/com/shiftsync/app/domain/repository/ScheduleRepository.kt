package com.shiftsync.app.domain.repository

import com.shiftsync.app.domain.model.Employee
import com.shiftsync.app.domain.model.ScheduleInsight
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.TradeRequest
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun observeMyShifts(employeeId: String): Flow<List<Shift>>
    fun observeOpenShifts(): Flow<List<Shift>>
    fun observeTeam(): Flow<List<Employee>>
    suspend fun shiftsForEmployee(employeeId: String): List<Shift>
    suspend fun postOpenShift(shiftId: String, note: String): TradeRequest
    suspend fun requestSwap(shiftId: String, recipientId: String?, note: String): TradeRequest
    suspend fun claimOpenShift(shiftId: String, employeeId: String): Shift
    suspend fun generateSwapNote(reason: String): String
    suspend fun generateScheduleInsight(employeeId: String): ScheduleInsight
}
