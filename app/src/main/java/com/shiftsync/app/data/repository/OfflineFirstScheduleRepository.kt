package com.shiftsync.app.data.repository

import androidx.room.withTransaction
import com.shiftsync.app.data.local.ShiftSyncDatabase
import com.shiftsync.app.data.local.toEntity
import com.shiftsync.app.domain.model.EligibilityResult
import com.shiftsync.app.domain.usecase.ValidateTradeEligibilityUseCase
import com.shiftsync.app.domain.model.Employee
import com.shiftsync.app.domain.model.ScheduleInsight
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import com.shiftsync.app.domain.model.TradeRequest
import com.shiftsync.app.domain.model.TradeRequestStatus
import com.shiftsync.app.domain.repository.ScheduleRepository
import com.shiftsync.app.domain.usecase.CalculateWeeklyScheduleMetricsUseCase
import com.shiftsync.app.domain.usecase.DraftSwapNoteUseCase
import com.shiftsync.app.domain.usecase.SummarizeOpenCoverageUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class OfflineFirstScheduleRepository(
    private val database: ShiftSyncDatabase
) : ScheduleRepository {
    private val dao = database.scheduleDao()
    private val calculateWeeklyScheduleMetrics = CalculateWeeklyScheduleMetricsUseCase()
    private val draftSwapNote = DraftSwapNoteUseCase()
    private val summarizeOpenCoverage = SummarizeOpenCoverageUseCase()

    override suspend fun initialize() {
        database.withTransaction {
            // Employees are the seed marker; an empty open board must never be reseeded.
            if (dao.employeeCount() == 0) {
                dao.upsertEmployees(SeedData.employees.map { it.toEntity() })
                dao.upsertShifts(SeedData.shifts().map { it.toEntity() })
            }
        }
    }

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

    override suspend fun postOpenShift(shiftId: String, note: String, requesterId: String): TradeRequest = database.withTransaction {
        val shift = dao.shiftById(shiftId)?.toDomain() ?: error("Shift $shiftId not found")
        check(shift.employeeId == requesterId) { "Only the current owner can offer this shift." }
        dao.upsertShift(shift.copy(status = ShiftStatus.UpForGrabs).toEntity())
        createTradeRequest(shiftId, null, note, requesterId)
    }

    override suspend fun requestSwap(shiftId: String, recipientId: String?, note: String, requesterId: String): TradeRequest = database.withTransaction {
        val shift = dao.shiftById(shiftId)?.toDomain() ?: error("Shift $shiftId not found")
        check(shift.employeeId == requesterId) { "Only the current owner can offer this shift." }
        dao.upsertShift(shift.copy(status = ShiftStatus.PendingTrade).toEntity())
        createTradeRequest(shiftId, recipientId, note, requesterId)
    }

    override suspend fun claimOpenShift(shiftId: String, employeeId: String): Shift = database.withTransaction {
        val shift = dao.shiftById(shiftId)?.toDomain() ?: error("Shift $shiftId not found")
        check(shift.status == ShiftStatus.UpForGrabs) { "This shift is no longer available to claim." }
        check(shift.employeeId != employeeId) { "You already own this shift." }
        check(dao.employeeById(employeeId) != null) { "Employee not found." }
        val eligibility = ValidateTradeEligibilityUseCase(this)(employeeId, shift)
        check(eligibility == EligibilityResult.Eligible) {
            (eligibility as EligibilityResult.Ineligible).reason
        }
        val claimed = shift.copy(employeeId = employeeId, status = ShiftStatus.Confirmed)
        dao.upsertShift(claimed.toEntity())
        dao.resolveRequestsForClaim(shiftId, employeeId)
        claimed
    }

    override suspend fun generateSwapNote(reason: String): String {
        return draftSwapNote(reason)
    }

    override suspend fun generateScheduleInsight(employeeId: String): ScheduleInsight = database.withTransaction {
        val employee = dao.employeeById(employeeId)?.toDomain() ?: error("Employee $employeeId not found")
        val shifts = shiftsForEmployee(employeeId)
        val metrics = calculateWeeklyScheduleMetrics(shifts, employee.hourlyRateCents)
        val gaps = summarizeOpenCoverage(dao.openShifts().map { it.toDomain() })

        ScheduleInsight(
            totalHours = metrics.totalHours,
            estimatedEarningsCents = metrics.estimatedEarningsCents,
            coverageGaps = gaps,
            summary = "You are scheduled for ${"%.1f".format(metrics.totalHours)} hours with an estimated ${formatCents(metrics.estimatedEarningsCents)} before tips."
        )
    }

    private suspend fun createTradeRequest(shiftId: String, recipientId: String?, note: String, requesterId: String): TradeRequest {
        val request = TradeRequest(
            id = UUID.randomUUID().toString(),
            shiftId = shiftId,
            requesterId = requesterId,
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
