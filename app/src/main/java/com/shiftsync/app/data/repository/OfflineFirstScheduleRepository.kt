package com.shiftsync.app.data.repository

import com.shiftsync.app.data.local.ScheduleDao
import com.shiftsync.app.data.local.toEntity
import com.shiftsync.app.domain.model.Employee
import com.shiftsync.app.domain.model.ScheduleInsight
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import com.shiftsync.app.domain.model.TradeRequest
import com.shiftsync.app.domain.model.TradeRequestStatus
import com.shiftsync.app.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class OfflineFirstScheduleRepository(
    private val dao: ScheduleDao
) : ScheduleRepository {
    override fun observeMyShifts(employeeId: String): Flow<List<Shift>> {
        return dao.observeShiftsForEmployee(employeeId).map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeOpenShifts(): Flow<List<Shift>> {
        return dao.observeOpenShifts().map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeTeam(): Flow<List<Employee>> {
        return dao.observeTeam().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun shiftsForEmployee(employeeId: String): List<Shift> {
        return dao.shiftsForEmployee(employeeId).map { it.toDomain() }
    }

    override suspend fun postOpenShift(shiftId: String, note: String): TradeRequest {
        val shift = dao.shiftById(shiftId)?.toDomain() ?: error("Shift $shiftId not found")
        dao.upsertShift(shift.copy(status = ShiftStatus.UpForGrabs).toEntity())
        return createTradeRequest(shiftId, null, note)
    }

    override suspend fun requestSwap(shiftId: String, recipientId: String?, note: String): TradeRequest {
        val shift = dao.shiftById(shiftId)?.toDomain() ?: error("Shift $shiftId not found")
        dao.upsertShift(shift.copy(status = ShiftStatus.PendingTrade).toEntity())
        return createTradeRequest(shiftId, recipientId, note)
    }

    override suspend fun claimOpenShift(shiftId: String, employeeId: String): Shift {
        val claimed = dao.shiftById(shiftId)
            ?.toDomain()
            ?.copy(employeeId = employeeId, status = ShiftStatus.Confirmed)
            ?: error("Shift $shiftId not found")
        dao.upsertShift(claimed.toEntity())
        return claimed
    }

    override suspend fun generateSwapNote(reason: String): String {
        val cleanedReason = reason.trim().ifBlank { "a scheduling conflict" }
        return "Hi team, could someone please cover this shift? I am requesting the swap because of $cleanedReason. Thanks for considering it."
    }

    override suspend fun generateScheduleInsight(employeeId: String): ScheduleInsight {
        val employee = SeedData.employees.first { it.id == employeeId }
        val shifts = shiftsForEmployee(employeeId)
        val totalHours = shifts.sumOf { it.hours }
        val earnings = (totalHours * employee.hourlyRateCents).toInt()
        val gaps = shifts
            .groupBy { it.role }
            .filterValues { roleShifts -> roleShifts.sumOf { it.hours } < 10.0 }
            .keys
            .map { "Light ${it.name} coverage this week" }

        return ScheduleInsight(
            totalHours = totalHours,
            estimatedEarningsCents = earnings,
            coverageGaps = gaps,
            summary = "You are scheduled for ${"%.1f".format(totalHours)} hours with an estimated ${formatCents(earnings)} before tips."
        )
    }

    private suspend fun createTradeRequest(shiftId: String, recipientId: String?, note: String): TradeRequest {
        val request = TradeRequest(
            id = UUID.randomUUID().toString(),
            shiftId = shiftId,
            requesterId = SeedData.currentEmployeeId,
            recipientId = recipientId,
            note = note,
            status = TradeRequestStatus.Pending,
            createdAt = Instant.now()
        )
        dao.upsertTradeRequest(request.toEntity())
        return request
    }

    private fun formatCents(cents: Int): String = "$${"%.2f".format(cents / 100.0)}"
}
