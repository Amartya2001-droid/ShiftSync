package com.shiftsync.app.presentation.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiftsync.app.presentation.ScheduleUiState

@Composable
fun AssistantScreen(
    state: ScheduleUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val insight = state.insight
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Smart Shift Assistant", style = MaterialTheme.typography.headlineMedium)
        Text(insight?.summary ?: "Generating schedule insight...")
        insight?.coverageGaps?.forEach { gap ->
            Text("Coverage gap: $gap")
        }
        Button(onClick = onRefresh) {
            Text("Refresh Insight")
        }
    }
}
