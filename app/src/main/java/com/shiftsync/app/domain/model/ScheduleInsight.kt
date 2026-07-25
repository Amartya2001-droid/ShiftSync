package com.shiftsync.app.domain.model

data class ScheduleInsight(
    val totalHours: Double,
    val estimatedEarningsCents: Int,
    val coverageGaps: List<String>,
    val summary: String
)
