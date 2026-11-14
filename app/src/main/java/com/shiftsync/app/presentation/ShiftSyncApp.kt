package com.shiftsync.app.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiftsync.app.presentation.assistant.AssistantScreen
import com.shiftsync.app.presentation.calendar.RosterScreen
import com.shiftsync.app.presentation.marketplace.MarketplaceScreen
import com.shiftsync.app.presentation.swap.SwapRequestDialog

@Composable
fun ShiftSyncApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Roster", "Marketplace", "Assistant")

    if (state.isLoading || state.loadError != null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
                Text("Loading your saved schedule…")
            } else {
                Text(state.loadError.orEmpty())
                Button(onClick = viewModel::loadSchedule) { Text("Retry") }
            }
        }
        return
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            var expanded by remember { mutableStateOf(false) }
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                TextButton(onClick = { expanded = true }, enabled = !state.isBusy) {
                    Text("Demo employee: ${state.team.find { it.id == state.currentEmployeeId }?.name.orEmpty()}")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    state.team.forEach { employee ->
                        DropdownMenuItem(
                            text = { Text(employee.name) },
                            onClick = { expanded = false; viewModel.switchEmployee(employee.id) }
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(label) },
                        icon = { Text(label.first().toString()) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> RosterScreen(
                state = state,
                onRequestSwap = viewModel::openSwapDialog,
                modifier = Modifier.padding(padding)
            )
            1 -> MarketplaceScreen(
                state = state,
                onClaimShift = viewModel::claimShift,
                modifier = Modifier.padding(padding)
            )
            2 -> AssistantScreen(
                state = state,
                onRefresh = viewModel::refreshInsight,
                modifier = Modifier.padding(padding)
            )
        }
    }

    state.selectedShift?.let { shift ->
        SwapRequestDialog(
            shift = shift,
            isSubmitting = state.isSavingSwap,
            team = state.team,
            selectedRecipientId = state.selectedRecipientId,
            reason = state.swapReason,
            onReasonChange = viewModel::updateSwapReason,
            onRecipientSelected = viewModel::selectRecipient,
            onDismiss = viewModel::dismissSwapDialog,
            onSubmit = viewModel::submitSwap
        )
    }
}
