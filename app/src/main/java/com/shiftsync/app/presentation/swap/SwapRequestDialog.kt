package com.shiftsync.app.presentation.swap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiftsync.app.domain.model.Employee
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.presentation.formatting.RoleFormatter

@Composable
fun SwapRequestDialog(
    shift: Shift,
    team: List<Employee>,
    selectedRecipientId: String?,
    reason: String,
    onReasonChange: (String) -> Unit,
    onRecipientSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (Boolean) -> Unit,
    isSubmitting: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Swap") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${RoleFormatter.label(shift.role)} in ${shift.section}")
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text("Reason") },
                    placeholder = { Text("University exam conflict") }
                )
                Text("Send to")
                FilterChip(
                    selected = selectedRecipientId == null,
                    onClick = { onRecipientSelected(null) },
                    label = { Text("Open board") }
                )
                team.filter { it.id != shift.employeeId }.take(3).forEach { employee ->
                    FilterChip(
                        selected = selectedRecipientId == employee.id,
                        onClick = { onRecipientSelected(employee.id) },
                        label = { Text(employee.name) }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(true) }, enabled = !isSubmitting) {
                Text(if (isSubmitting) "Saving…" else "Send Smart Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}
