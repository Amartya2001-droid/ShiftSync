package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.Shift

class PrioritizeOpenShiftMarketplaceUseCase {
    operator fun invoke(openShifts: List<Shift>): List<Shift> {
        return openShifts.sortedWith(
            compareBy<Shift> { it.startsAt }
                .thenBy { it.role.name }
                .thenBy { it.section }
        )
    }
}
