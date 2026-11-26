package com.shiftsync.app.presentation.swap

import androidx.compose.runtime.*
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.test.espresso.Espresso.closeSoftKeyboard
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.shiftsync.app.domain.model.Employee
import com.shiftsync.app.domain.model.RestaurantRole
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import org.junit.Rule
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class SwapRequestDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun submitSmartNoteFromSwapDialog() {
        var submittedSmartNote = false
        var submittedReason = ""
        var submittedRecipient: String? = null

        composeRule.setContent {
            var reason by remember { mutableStateOf("") }
            var recipient by remember { mutableStateOf<String?>(null) }
            SwapRequestDialog(
                shift = shift(),
                team = listOf(Employee("emp-2", "Jon Patel", RestaurantRole.LineCook, 2100)),
                selectedRecipientId = recipient,
                reason = reason,
                onReasonChange = { reason = it },
                onRecipientSelected = { recipient = it },
                onDismiss = {},
                onSubmit = { submittedSmartNote = it; submittedReason = reason; submittedRecipient = recipient }
            )
        }

        composeRule.onNodeWithText("Request Swap").assertIsDisplayed()
        composeRule.onNodeWithText("Reason").performTextInput("exam conflict")
        composeRule.onNodeWithText("Reason").assertTextContains("exam conflict")
        closeSoftKeyboard()
        composeRule.onNodeWithText("Jon Patel").performClick().assertIsSelected()
        composeRule.onNodeWithText("Send Smart Note").performClick()

        assertTrue(submittedSmartNote)
        assertEquals("exam conflict", submittedReason)
        assertEquals("emp-2", submittedRecipient)
    }

    private fun shift(): Shift {
        return Shift(
            id = "shift-1",
            employeeId = "emp-1",
            role = RestaurantRole.Server,
            section = "Patio",
            startsAt = LocalDateTime.of(2026, 7, 20, 11, 0),
            endsAt = LocalDateTime.of(2026, 7, 20, 17, 0),
            status = ShiftStatus.Confirmed
        )
    }
}
