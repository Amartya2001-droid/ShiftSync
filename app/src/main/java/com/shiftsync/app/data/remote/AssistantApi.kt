package com.shiftsync.app.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

interface AssistantApi {
    @POST("assistant/swap-note")
    suspend fun generateSwapNote(@Body request: SwapNoteRequest): SwapNoteResponse

    @POST("assistant/schedule-insight")
    suspend fun generateScheduleInsight(@Body request: ScheduleInsightRequest): ScheduleInsightResponse
}

@Serializable
data class SwapNoteRequest(val reason: String)

@Serializable
data class SwapNoteResponse(val note: String)

@Serializable
data class ScheduleInsightRequest(val employeeId: String, val shifts: List<ShiftDto>)

@Serializable
data class ScheduleInsightResponse(
    val totalHours: Double,
    val estimatedEarningsCents: Int,
    val coverageGaps: List<String>,
    val summary: String
)
