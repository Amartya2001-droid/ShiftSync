package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.ScheduleInsight
import com.shiftsync.app.domain.repository.ScheduleRepository

class GetScheduleInsightsUseCase(
    private val repository: ScheduleRepository
) {
    suspend operator fun invoke(employeeId: String): ScheduleInsight {
        return repository.generateScheduleInsight(employeeId)
    }
}
