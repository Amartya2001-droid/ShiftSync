package com.shiftsync.app.presentation.marketplace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.presentation.ScheduleUiState
import com.shiftsync.app.presentation.calendar.StatusBadge
import java.time.format.DateTimeFormatter

@Composable
fun MarketplaceScreen(
    state: ScheduleUiState,
    onClaimShift: (Shift) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Trade Marketplace", style = MaterialTheme.typography.headlineMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.openShifts, key = { it.id }) { shift ->
                OpenShiftCard(shift = shift, onClaimShift = { onClaimShift(shift) })
            }
        }
    }
}

@Composable
private fun OpenShiftCard(shift: Shift, onClaimShift: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${shift.role.name} - ${shift.section}", style = MaterialTheme.typography.titleMedium)
            Text("${shift.startsAt.format(DateTimeFormatter.ofPattern("EEE, MMM d"))} ${shift.startsAt.format(DateTimeFormatter.ofPattern("h:mm a"))} - ${shift.endsAt.format(DateTimeFormatter.ofPattern("h:mm a"))}")
            StatusBadge(status = shift.status)
            Button(onClick = onClaimShift) {
                Text("Claim Shift")
            }
        }
    }
}
