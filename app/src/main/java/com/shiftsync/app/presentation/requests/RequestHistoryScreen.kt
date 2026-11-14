package com.shiftsync.app.presentation.requests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiftsync.app.presentation.ScheduleUiState
import com.shiftsync.app.presentation.common.EmptyState
import com.shiftsync.app.presentation.formatting.ShiftTimeFormatter
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RequestHistoryScreen(state: ScheduleUiState, modifier: Modifier = Modifier) {
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
                            Text(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm").withZone(ZoneId.systemDefault()).format(item.request.createdAt))
                        }
                    }
                }
            }
        }
    }
}
