package com.shiftsync.app.presentation.requests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiftsync.app.domain.model.TradeRequestStatus
import com.shiftsync.app.presentation.ScheduleUiState
import com.shiftsync.app.presentation.common.EmptyState
import com.shiftsync.app.presentation.formatting.ShiftTimeFormatter
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RequestHistoryScreen(state: ScheduleUiState, onRespond: (String, Boolean) -> Unit, onCancel: (String) -> Unit, modifier: Modifier = Modifier) {
    var confirmationId by remember(state.currentEmployeeId) { mutableStateOf<String?>(null) }
    val confirmation = state.requestHistory.firstOrNull {
        it.request.id == confirmationId && it.request.requesterId == state.currentEmployeeId && it.request.status == TradeRequestStatus.Pending
    }
    if (confirmation != null) {
        AlertDialog(
            onDismissRequest = { confirmationId = null },
            title = { Text("Withdraw offer?") },
            text = { Text("You'll keep this shift. The offer will no longer be available to teammates.") },
            confirmButton = {
                Button(onClick = { onCancel(confirmation.request.id); confirmationId = null }, enabled = !state.isBusy) {
                    Text("Withdraw offer")
                }
            },
            dismissButton = { TextButton(onClick = { confirmationId = null }) { Text("Keep offer") } }
        )
    }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Request history", style = MaterialTheme.typography.headlineMedium)
        Text("Requests you sent or received")
        if (state.requestHistory.isEmpty()) {
            EmptyState(title = "No requests yet", message = "Post a shift or send a teammate a request to get started.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.requestHistory, key = { it.request.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(item.shift?.section ?: "Shift unavailable", style = MaterialTheme.typography.titleMedium)
                            item.shift?.let { Text(ShiftTimeFormatter.fullRange(it)) }
                            Text("Status: ${item.request.status.name}")
                            Text("From ${item.requesterName} → ${item.recipientName ?: "Open board"}")
                            Text(item.request.note.ifBlank { "No note provided" })
                            if (item.request.requesterId == state.currentEmployeeId && item.request.status == TradeRequestStatus.Pending) {
                                OutlinedButton(onClick = { confirmationId = item.request.id }, enabled = !state.isBusy) {
                                    Text(if (state.cancellingRequestId == item.request.id) "Withdrawing…" else "Withdraw offer")
                                }
                            }
                            if (item.request.recipientId == state.currentEmployeeId && item.request.status == TradeRequestStatus.Pending) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { onRespond(item.request.id, true) }, enabled = !state.isBusy) {
                                        Text(if (state.respondingRequestId == item.request.id) "Saving…" else "Accept")
                                    }
                                    OutlinedButton(onClick = { onRespond(item.request.id, false) }, enabled = !state.isBusy) { Text("Decline") }
                                }
                            }
                            Text(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm").withZone(ZoneId.systemDefault()).format(item.request.createdAt))
                        }
                    }
                }
            }
        }
    }
}
