package com.shiftsync.app.data.repository

import com.shiftsync.app.domain.model.Employee
import com.shiftsync.app.domain.model.ScheduleInsight
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import com.shiftsync.app.domain.model.TradeRequest
import com.shiftsync.app.domain.model.TradeRequestStatus
import com.shiftsync.app.domain.repository.ScheduleRepository
import com.shiftsync.app.domain.usecase.CalculateWeeklyScheduleMetricsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class InMemoryScheduleRepository : ScheduleRepository {
    private val shifts = MutableStateFlow(SeedData.shifts())
    private val employees = MutableStateFlow(SeedData.employees)
    private val calculateWeeklyScheduleMetrics = CalculateWeeklyScheduleMetricsUseCase()

    override fun observeMyShifts(employeeId: String): Flow<List<Shift>> {
        return shifts.map { items -> items.filter { it.employeeId == employeeId }.sortedBy { it.startsAt } }
    }

    override fun observeOpenShifts(): Flow<List<Shift>> {
        return shifts.map { items -> items.filter { it.status == ShiftStatus.UpForGrabs }.sortedBy { it.startsAt } }
    }

    override fun observeTeam(): Flow<List<Employee>> = employees

    override suspend fun shiftsForEmployee(employeeId: String): List<Shift> {
        return shifts.value.filter { it.employeeId == employeeId }
    }

    override suspend fun postOpenShift(shiftId: String, note: String): TradeRequest {
        updateShift(shiftId) { it.copy(status = ShiftStatus.UpForGrabs) }
        return newRequest(shiftId, null, note)
    }

    override suspend fun requestSwap(shiftId: String, recipientId: String?, note: String): TradeRequest {
        updateShift(shiftId) { it.copy(status = ShiftStatus.PendingTrade) }
        return newRequest(shiftId, recipientId, note)
    }

    override suspend fun claimOpenShift(shiftId: String, employeeId: String): Shift {
        val claimed = updateShift(shiftId) { it.copy(employeeId = employeeId, status = ShiftStatus.Confirmed) }
        return claimed
    }

    override suspend fun generateSwapNote(reason: String): String {
        val cleanedReason = reason.trim().ifBlank { "a scheduling conflict" }
        return "Hi team, could someone please take this shift? I have $cleanedReason and would really appreciate the coverage."
    }

    override suspend fun generateScheduleInsight(employeeId: String): ScheduleInsight {
        val employee = employees.value.first { it.id == employeeId }
        val myShifts = shiftsForEmployee(employeeId)
        val metrics = calculateWeeklyScheduleMetrics(myShifts, employee.hourlyRateCents)
        return ScheduleInsight(
            totalHours = metrics.totalHours,
            estimatedEarningsCents = metrics.estimatedEarningsCents,
            coverageGaps = listOf("Thursday lunch has one open Server shift", "Saturday close needs backup Host coverage"),
            summary = "${employee.name} has ${"%.1f".format(metrics.totalHours)} hours, ${metrics.dinnerShiftCount} dinner shifts, and about ${formatCents(metrics.estimatedEarningsCents)} in base wages."
        )
    }

    private fun updateShift(shiftId: String, transform: (Shift) -> Shift): Shift {
        var updated: Shift? = null
        shifts.value = shifts.value.map { shift ->
            if (shift.id == shiftId) {
                transform(shift).also { updated = it }
            } else {
                shift
            }
        }
        return updated ?: error("Shift $shiftId not found")
    }

    private fun newRequest(shiftId: String, recipientId: String?, note: String): TradeRequest {
        return TradeRequest(
            id = UUID.randomUUID().toString(),
            shiftId = shiftId,
            requesterId = SeedData.currentEmployeeId,
            recipientId = recipientId,
            note = note,
            status = TradeRequestStatus.Pending,
            createdAt = Instant.now()
        )
    }

    private fun formatCents(cents: Int): String = "$${"%.2f".format(cents / 100.0)}"
}
