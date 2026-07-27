package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus

class CalculateRosterStatusSummaryUseCase {
    operator fun invoke(shifts: List<Shift>): RosterStatusSummary {
        return RosterStatusSummary(
            confirmed = shifts.count { it.status == ShiftStatus.Confirmed },
            pendingTrade = shifts.count { it.status == ShiftStatus.PendingTrade },
            upForGrabs = shifts.count { it.status == ShiftStatus.UpForGrabs }
        )
    }
}

data class RosterStatusSummary(
    val confirmed: Int,
    val pendingTrade: Int,
    val upForGrabs: Int
)
