package com.shiftsync.app.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiftsync.app.data.local.ShiftSyncDatabase
import com.shiftsync.app.domain.model.ShiftStatus
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
    fun claimsPostsAndRequestsSurviveDatabaseReopen() = runBlocking<Unit> {
        repository.initialize()
        repository.claimOpenShift("shift-4", SeedData.currentEmployeeId)
        val posted = repository.postOpenShift("shift-1", "Need coverage")
        val targeted = repository.requestSwap("shift-3", "emp-jon", "Exam")
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
        val result = runCatching { repository.postOpenShift("shift-1", "Need coverage") }
        assertTrue(result.isFailure)
        assertEquals(ShiftStatus.Confirmed, database.scheduleDao().shiftById("shift-1")!!.status)
        assertTrue(database.scheduleDao().tradeRequests().isEmpty())
    }

    @Test
    fun simultaneousClaimsCannotOverwriteTheWinner() = runBlocking<Unit> {
        repository.initialize()
        val outcomes = coroutineScope {
            val first = async { runCatching { repository.claimOpenShift("shift-4", "emp-sam") } }
            val second = async { runCatching { repository.claimOpenShift("shift-4", "emp-jon") } }
            listOf(first.await(), second.await())
        }
        assertEquals(1, outcomes.count { it.isSuccess })
        val winner = outcomes.single { it.isSuccess }.getOrThrow()
        assertEquals(winner, database.scheduleDao().shiftById("shift-4")!!.toDomain())
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
        repository.postOpenShift("shift-4", "Available")
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
