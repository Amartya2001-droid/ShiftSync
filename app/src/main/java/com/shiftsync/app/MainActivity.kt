package com.shiftsync.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiftsync.app.data.repository.InMemoryScheduleRepository
import com.shiftsync.app.data.repository.SeedData
import com.shiftsync.app.domain.usecase.ClaimOpenShiftUseCase
import com.shiftsync.app.domain.usecase.GetScheduleInsightsUseCase
import com.shiftsync.app.domain.usecase.RequestShiftSwapUseCase
import com.shiftsync.app.domain.usecase.ValidateTradeEligibilityUseCase
import com.shiftsync.app.presentation.MainViewModel
import com.shiftsync.app.presentation.MainViewModelFactory
import com.shiftsync.app.presentation.ShiftSyncApp
import com.shiftsync.app.ui.theme.ShiftSyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = InMemoryScheduleRepository()
        val validate = ValidateTradeEligibilityUseCase(repository)
        val factory = MainViewModelFactory(
            repository = repository,
            requestShiftSwap = RequestShiftSwapUseCase(repository),
            claimOpenShift = ClaimOpenShiftUseCase(repository, validate),
            getScheduleInsights = GetScheduleInsightsUseCase(repository),
            employeeId = SeedData.currentEmployeeId
        )

        setContent {
            ShiftSyncTheme {
                val viewModel: MainViewModel = viewModel(factory = factory)
                ShiftSyncApp(viewModel = viewModel)
            }
        }
    }
}
