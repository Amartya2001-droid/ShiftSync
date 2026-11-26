package com.shiftsync.app.presentation.requests

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import com.shiftsync.app.data.local.ShiftSyncDatabase
import com.shiftsync.app.data.repository.OfflineFirstScheduleRepository
import com.shiftsync.app.data.repository.SeedData
import com.shiftsync.app.domain.model.ShiftStatus
import com.shiftsync.app.domain.model.TradeRequestStatus
import com.shiftsync.app.domain.usecase.*
import com.shiftsync.app.presentation.MainViewModel
import com.shiftsync.app.presentation.ShiftSyncApp
import com.shiftsync.app.ui.theme.ShiftSyncTheme
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Full app UI on an Android device, using the production repository with an isolated Room DB. */
class RequestOwnershipDeviceFlowTest {
    @get:Rule val ui = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "device-flow-${UUID.randomUUID()}.db"
    private lateinit var database: ShiftSyncDatabase
    private lateinit var repository: OfflineFirstScheduleRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun launchAppUi() {
        database = Room.databaseBuilder(context, ShiftSyncDatabase::class.java, databaseName).build()
        repository = OfflineFirstScheduleRepository(database)
        runBlocking { repository.initialize() }
        ui.runOnUiThread {
            viewModel = MainViewModel(repository, RequestShiftSwapUseCase(repository),
                ClaimOpenShiftUseCase(repository), GetScheduleInsightsUseCase(repository), SeedData.currentEmployeeId)
        }
        ui.setContent { ShiftSyncTheme { ShiftSyncApp(viewModel) } }
        await { !viewModel.state.value.isLoading && viewModel.state.value.myShifts.size == 3 }
    }

    @After
    fun closeDatabase() {
        if (::viewModel.isInitialized) ui.runOnUiThread { viewModel.viewModelScope.cancel() }
        if (::database.isInitialized) database.close()
        context.deleteDatabase(databaseName)
    }

    @Test fun senderRecipientHistoryAcceptTransfersOwnership() = runBlocking {
        exerciseResponse(accept = true)
    }

    @Test fun senderRecipientHistoryDeclineRetainsOwnership() = runBlocking {
        exerciseResponse(accept = false)
    }

    private suspend fun exerciseResponse(accept: Boolean) {
        val scenario = if (accept) "accept" else "decline"
        ui.onAllNodesWithText("Request Swap").onFirst().performClick()
        ui.onNodeWithText("Reason").performTextInput("Exam cover request")
        ui.onNodeWithText("Reason").assertTextContains("Exam cover request")
        closeSoftKeyboard()
        ui.onNodeWithText("Sam Rivera").performClick().assertIsSelected()
        ui.onNodeWithText("Send Smart Note").performClick()
        await { !viewModel.state.value.isBusy && viewModel.state.value.requestHistory.size == 1 }
        val request = repository.observeRequestHistory(SeedData.currentEmployeeId).first().single().request
        assertEquals("shift-1", request.shiftId)
        assertEquals("emp-sam", request.recipientId)
        assertTrue(request.note.contains("Exam cover request"))
        assertEquals(SeedData.currentEmployeeId, database.scheduleDao().shiftById("shift-1")!!.employeeId)
        ui.onNodeWithText("Requests").performClick()
        ui.onNodeWithText("Status: Pending").assertIsDisplayed()
        ui.onNodeWithText(request.note).performScrollTo().assertIsDisplayed()
        screenshot("$scenario-sender-pending")

        switchEmployee("Maya Chen", "Sam Rivera", "emp-sam")
        ui.onNodeWithText("To respond (1)").performClick()
        ui.onNodeWithText("Status: Pending").assertIsDisplayed()
        ui.onNodeWithText(request.note).performScrollTo().assertIsDisplayed()
        screenshot("$scenario-recipient-pending")
        val action = if (accept) "Accept" else "Decline"
        val title = if (accept) "Accept this shift?" else "Decline this request?"
        ui.onNodeWithText(action).performScrollTo().performClick()
        ui.onNodeWithText(title).assertIsDisplayed()
        ui.onNodeWithText("Go back").performClick()
        assertEquals(TradeRequestStatus.Pending, database.scheduleDao().requestById(request.id)!!.status)
        assertEquals(SeedData.currentEmployeeId, database.scheduleDao().shiftById("shift-1")!!.employeeId)
        ui.onNodeWithText(action).performScrollTo().performClick()
        ui.onNodeWithText(if (accept) "Accept shift" else "Decline request").performClick()
        val expected = if (accept) TradeRequestStatus.Accepted else TradeRequestStatus.Rejected
        await { !viewModel.state.value.isBusy && viewModel.state.value.requestHistory.singleOrNull()?.request?.status == expected }
        ui.onNodeWithText("To respond (0)").assertIsDisplayed()
        ui.onNodeWithText("Resolved (1)").performScrollTo().performClick()
        ui.onNodeWithText("Status: ${expected.name}").assertIsDisplayed()
        assertEquals(expected, database.scheduleDao().requestById(request.id)!!.status)
        assertEquals(if (accept) "emp-sam" else SeedData.currentEmployeeId, database.scheduleDao().shiftById("shift-1")!!.employeeId)
        assertEquals(ShiftStatus.Confirmed, database.scheduleDao().shiftById("shift-1")!!.status)
        ui.onNodeWithText("Roster").performClick()
        if (accept) ui.onNodeWithContentDescription("Shift shift-1").assertIsDisplayed()
        else ui.onNodeWithText("No shifts scheduled").assertIsDisplayed()
        screenshot("$scenario-recipient-roster")

        switchEmployee("Sam Rivera", "Maya Chen", SeedData.currentEmployeeId)
        await { viewModel.state.value.myShifts.any { it.id == "shift-1" } == !accept }
        if (accept) ui.onNodeWithContentDescription("Shift shift-1").assertDoesNotExist()
        else ui.onNodeWithContentDescription("Shift shift-1").assertIsDisplayed()
        ui.onNodeWithText("Requests").performClick()
        ui.onNodeWithText("Status: ${expected.name}").assertIsDisplayed()
        ui.onNodeWithText(request.note).performScrollTo().assertIsDisplayed()
        screenshot("$scenario-sender-resolved")
    }

    private fun switchEmployee(currentName: String, nextName: String, employeeId: String) {
        ui.onNodeWithText("Demo employee: $currentName").performClick()
        ui.onNodeWithText(nextName).performClick()
        await { !viewModel.state.value.isLoading && viewModel.state.value.currentEmployeeId == employeeId }
    }

    private fun await(condition: () -> Boolean) {
        ui.waitUntil(timeoutMillis = 15_000, condition = condition)
        ui.waitForIdle()
    }

    private fun screenshot(name: String) {
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val directory = File(checkNotNull(context.getExternalFilesDir(null)), "ui-evidence").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
