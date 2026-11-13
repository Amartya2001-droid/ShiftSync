package com.shiftsync.app.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiftsync.app.data.local.ShiftSyncDatabase
import com.shiftsync.app.domain.model.ShiftStatus
import com.shiftsync.app.domain.model.TradeRequestStatus
import com.shiftsync.app.domain.usecase.ClaimOpenShiftUseCase
import com.shiftsync.app.data.local.toEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class PersistentScheduleRepositoryTest {
    private lateinit var context: Application
    private lateinit var database: ShiftSyncDatabase
    private lateinit var repository: OfflineFirstScheduleRepository
    private val databaseName = "persistence-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
        openDatabase()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    private fun openDatabase() {
        database = Room.databaseBuilder(context, ShiftSyncDatabase::class.java, databaseName).build()
        repository = OfflineFirstScheduleRepository(database)
    }

    @Test
    fun postedShiftTransfersOwnershipAndResolvesRequestsAcrossReopen() = runBlocking<Unit> {
        repository.initialize()
        val targeted = repository.requestSwap("shift-1", "emp-jon", "Can you cover?", SeedData.currentEmployeeId)
        val posted = repository.postOpenShift("shift-1", "Open to the team", SeedData.currentEmployeeId)
        assertEquals(SeedData.currentEmployeeId, database.scheduleDao().shiftById("shift-1")!!.employeeId)
        assertEquals(SeedData.currentEmployeeId, posted.requesterId)
        val snapshot = repository.observeOpenShifts().first().first { it.id == "shift-1" }
        val claimed = ClaimOpenShiftUseCase(repository)("emp-sam", snapshot).getOrThrow()
        assertEquals("emp-sam", claimed.employeeId)
        database.close()
        openDatabase()
        repository.initialize()

        assertFalse(repository.shiftsForEmployee(SeedData.currentEmployeeId).any { it.id == "shift-1" })
        assertTrue(repository.shiftsForEmployee("emp-sam").any { it.id == "shift-1" })
        assertFalse(repository.observeOpenShifts().first().any { it.id == "shift-1" })
        val requests = database.scheduleDao().tradeRequests().associateBy { it.id }
        assertEquals(TradeRequestStatus.Accepted, requests[posted.id]!!.status)
        assertEquals("emp-sam", requests[posted.id]!!.recipientId)
        assertEquals(TradeRequestStatus.Rejected, requests[targeted.id]!!.status)
        assertEquals(13.5, repository.generateScheduleInsight(SeedData.currentEmployeeId).totalHours, 0.0)
        assertEquals(6.0, repository.generateScheduleInsight("emp-sam").totalHours, 0.0)
        assertTrue(ClaimOpenShiftUseCase(repository)("emp-jon", snapshot).isFailure)
        assertEquals("emp-sam", database.scheduleDao().shiftById("shift-1")!!.employeeId)
        assertTrue(runCatching { repository.postOpenShift("shift-1", "Stale offer", SeedData.currentEmployeeId) }.isFailure)
        assertEquals(ShiftStatus.Confirmed, database.scheduleDao().shiftById("shift-1")!!.status)
        // A subsequent post is attributed to the new owner, not the default demo employee.
        assertEquals("emp-sam", repository.postOpenShift("shift-1", "Available again", "emp-sam").requesterId)
    }

    @Test
    fun selfClaimsUnknownEmployeesAndConflictsPreserveOwnershipAndRequest() = runBlocking<Unit> {
        repository.initialize()
        val posted = repository.postOpenShift("shift-1", "Need coverage", SeedData.currentEmployeeId)
        val shift = database.scheduleDao().shiftById("shift-1")!!
        val own = runCatching { repository.claimOpenShift(shift.id, SeedData.currentEmployeeId) }
        assertEquals("You already own this shift.", own.exceptionOrNull()!!.message)
        assertTrue(runCatching { repository.claimOpenShift(shift.id, "missing") }.isFailure)
        database.scheduleDao().upsertShift(shift.copy(id = "busy", employeeId = "emp-sam", status = ShiftStatus.Confirmed))
        assertTrue(runCatching { repository.claimOpenShift(shift.id, "emp-sam") }.exceptionOrNull()!!.message!!.contains("overlaps"))
        assertEquals(shift, database.scheduleDao().shiftById(shift.id))
        assertEquals(TradeRequestStatus.Pending, database.scheduleDao().tradeRequests().single { it.id == posted.id }.status)
    }

    @Test
    fun failedRequestResolutionRollsBackOwnershipTransfer() = runBlocking<Unit> {
        repository.initialize()
        repository.postOpenShift("shift-1", "Need coverage", SeedData.currentEmployeeId)
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_resolution BEFORE UPDATE ON trade_requests BEGIN SELECT RAISE(ABORT, 'test failure'); END"
        )
        assertTrue(runCatching { repository.claimOpenShift("shift-1", "emp-sam") }.isFailure)
        assertEquals(SeedData.currentEmployeeId, database.scheduleDao().shiftById("shift-1")!!.employeeId)
        assertEquals(ShiftStatus.UpForGrabs, database.scheduleDao().shiftById("shift-1")!!.status)
        assertEquals(TradeRequestStatus.Pending, database.scheduleDao().tradeRequests().single().status)
    }

    @Test
    fun simultaneousDifferentClaimsCannotExceedWeeklyHours() = runBlocking<Unit> {
        repository.initialize()
        val template = SeedData.shifts().first()
        val monday = template.startsAt.toLocalDate().atStartOfDay()
        val longShift = template.copy(id = "thirty-hours", employeeId = "emp-lina", startsAt = monday, endsAt = monday.plusHours(30))
        val first = template.copy(id = "offer-a", employeeId = null, status = ShiftStatus.UpForGrabs,
            startsAt = monday.plusDays(3).plusHours(8), endsAt = monday.plusDays(3).plusHours(14))
        val second = first.copy(id = "offer-b", startsAt = first.startsAt.plusDays(2), endsAt = first.endsAt.plusDays(2))
        database.scheduleDao().upsertShifts(listOf(longShift, first, second).map { it.toEntity() })
        val results = coroutineScope {
            val a = async { runCatching { repository.claimOpenShift(first.id, "emp-lina") } }
            val b = async { runCatching { repository.claimOpenShift(second.id, "emp-lina") } }
            listOf(a.await(), b.await())
        }
        assertEquals(1, results.count { it.isSuccess })
        assertEquals(36.0, repository.shiftsForEmployee("emp-lina").sumOf { it.hours }, 0.0)
        assertTrue(results.single { it.isFailure }.exceptionOrNull()!!.message!!.contains("weekly hours"))
    }

    @Test
    fun minimumRestIsCheckedAgainstTheSavedSchedule() = runBlocking<Unit> {
        repository.initialize()
        val offered = SeedData.shifts().first()
        repository.postOpenShift(offered.id, "Need coverage", SeedData.currentEmployeeId)
        val prior = offered.copy(id = "late-night", employeeId = "emp-sam", status = ShiftStatus.Confirmed,
            startsAt = offered.startsAt.minusHours(10), endsAt = offered.startsAt.minusHours(7))
        database.scheduleDao().upsertShift(prior.toEntity())
        val failure = runCatching { repository.claimOpenShift(offered.id, "emp-sam") }.exceptionOrNull()
        assertTrue(failure!!.message!!.contains("hours of rest"))
        assertEquals(SeedData.currentEmployeeId, database.scheduleDao().shiftById(offered.id)!!.employeeId)
    }

    @Test
    fun claimsPostsAndRequestsSurviveDatabaseReopen() = runBlocking<Unit> {
        repository.initialize()
        repository.claimOpenShift("shift-4", SeedData.currentEmployeeId)
        val posted = repository.postOpenShift("shift-1", "Need coverage", SeedData.currentEmployeeId)
        val targeted = repository.requestSwap("shift-3", "emp-jon", "Exam", SeedData.currentEmployeeId)
        database.close()
        openDatabase()
        repository.initialize()

        assertEquals(ShiftStatus.Confirmed, database.scheduleDao().shiftById("shift-4")!!.status)
        assertEquals(SeedData.currentEmployeeId, database.scheduleDao().shiftById("shift-4")!!.employeeId)
        assertEquals(ShiftStatus.UpForGrabs, database.scheduleDao().shiftById("shift-1")!!.status)
        assertEquals(ShiftStatus.PendingTrade, database.scheduleDao().shiftById("shift-3")!!.status)
        val requests = database.scheduleDao().tradeRequests().map { it.toDomain() }.associateBy { it.id }
        assertEquals(posted, requests[posted.id])
        assertEquals(targeted, requests[targeted.id])
        assertEquals(25.5, repository.generateScheduleInsight(SeedData.currentEmployeeId).totalHours, 0.0)
        assertEquals(2, repository.generateScheduleInsight(SeedData.currentEmployeeId).coverageGaps.size)
    }

    @Test
    fun initializationDoesNotRestoreAnEmptyBoardOrOverwriteEmployeeData() = runBlocking<Unit> {
        repository.initialize()
        repository.claimOpenShift("shift-4", "emp-sam")
        repository.claimOpenShift("shift-5", "emp-sam")
        val employee = database.scheduleDao().employeeById(SeedData.currentEmployeeId)!!
        database.scheduleDao().upsertEmployees(listOf(employee.copy(hourlyRateCents = 2400)))
        database.close()
        openDatabase()
        repository.initialize()
        repository.initialize()

        assertTrue(repository.observeOpenShifts().first().isEmpty())
        assertEquals(4, database.scheduleDao().employeeCount())
        assertEquals(46800, repository.generateScheduleInsight(SeedData.currentEmployeeId).estimatedEarningsCents)
        assertTrue(repository.generateScheduleInsight(SeedData.currentEmployeeId).coverageGaps.isEmpty())
    }

    @Test
    fun failedTradeInsertRollsBackTheShiftUpdate() = runBlocking<Unit> {
        repository.initialize()
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_trade BEFORE INSERT ON trade_requests BEGIN SELECT RAISE(ABORT, 'test failure'); END"
        )
        val result = runCatching { repository.postOpenShift("shift-1", "Need coverage", SeedData.currentEmployeeId) }
        assertTrue(result.isFailure)
        assertEquals(ShiftStatus.Confirmed, database.scheduleDao().shiftById("shift-1")!!.status)
        assertTrue(database.scheduleDao().tradeRequests().isEmpty())
    }

    @Test
    fun simultaneousClaimsCannotOverwriteTheWinner() = runBlocking<Unit> {
        repository.initialize()
        val post = repository.postOpenShift("shift-1", "Need coverage", SeedData.currentEmployeeId)
        val outcomes = coroutineScope {
            val first = async { runCatching { repository.claimOpenShift("shift-1", "emp-sam") } }
            val second = async { runCatching { repository.claimOpenShift("shift-1", "emp-jon") } }
            listOf(first.await(), second.await())
        }
        assertEquals(1, outcomes.count { it.isSuccess })
        val winner = outcomes.single { it.isSuccess }.getOrThrow()
        assertEquals(winner, database.scheduleDao().shiftById("shift-1")!!.toDomain())
        val request = database.scheduleDao().tradeRequests().single { it.id == post.id }
        assertEquals(TradeRequestStatus.Accepted, request.status)
        assertEquals(winner.employeeId, request.recipientId)
    }

    @Test
    fun roomFlowsReflectClaimAndOpenBoardChanges() = runBlocking<Unit> {
        repository.initialize()
        assertEquals(2, repository.observeOpenShifts().first().size)
        repository.claimOpenShift("shift-4", SeedData.currentEmployeeId)
        withTimeout(5000) {
            assertEquals(listOf("shift-5"), repository.observeOpenShifts().first { it.size == 1 }.map { it.id })
            assertTrue(repository.observeMyShifts(SeedData.currentEmployeeId).first().any { it.id == "shift-4" })
        }
        assertEquals(1, repository.generateScheduleInsight(SeedData.currentEmployeeId).coverageGaps.size)
        repository.postOpenShift("shift-4", "Available", SeedData.currentEmployeeId)
        assertEquals(2, repository.observeOpenShifts().first().size)
        assertEquals(2, repository.generateScheduleInsight(SeedData.currentEmployeeId).coverageGaps.size)
    }

    @Test
    fun simultaneousInitializationSeedsOnlyOnce() = runBlocking<Unit> {
        coroutineScope {
            val first = async { repository.initialize() }
            val second = async { repository.initialize() }
            first.await()
            second.await()
        }
        assertEquals(4, database.scheduleDao().employeeCount())
        assertEquals(3, repository.shiftsForEmployee(SeedData.currentEmployeeId).size)
        assertEquals(2, repository.observeOpenShifts().first().size)
    }
}
