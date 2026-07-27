package com.shiftsync.app.presentation.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import com.shiftsync.app.presentation.ScheduleUiState
import com.shiftsync.app.presentation.formatting.ShiftTimeFormatter

@Composable
fun RosterScreen(
    state: ScheduleUiState,
    onRequestSwap: (Shift) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Weekly Roster", style = MaterialTheme.typography.headlineMedium)
        WeekStrip(shifts = state.myShifts)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.myShifts, key = { it.id }) { shift ->
                ShiftCard(shift = shift, onRequestSwap = { onRequestSwap(shift) })
            }
        }
    }
}

@Composable
private fun WeekStrip(shifts: List<Shift>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(shifts.distinctBy { it.startsAt.toLocalDate() }) { shift ->
            AssistChip(
                onClick = {},
                label = { Text(ShiftTimeFormatter.dayChipLabel(shift)) }
            )
        }
    }
}

@Composable
fun ShiftCard(
    shift: Shift,
    onRequestSwap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = "Shift ${shift.id}" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(shift.role.name, style = MaterialTheme.typography.titleMedium)
                    Text("${shift.section} section", style = MaterialTheme.typography.bodyMedium)
                }
                StatusBadge(status = shift.status)
            }
            Text(ShiftTimeFormatter.fullRange(shift))
            Button(onClick = onRequestSwap) {
                Text("Request Swap")
            }
        }
    }
}

@Composable
fun StatusBadge(status: ShiftStatus) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                when (status) {
                    ShiftStatus.Confirmed -> "Confirmed"
                    ShiftStatus.PendingTrade -> "Pending Trade"
                    ShiftStatus.UpForGrabs -> "Up for Grabs"
                }
            )
        }
    )
}
