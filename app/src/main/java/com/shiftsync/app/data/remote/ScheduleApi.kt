package com.shiftsync.app.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ScheduleApi {
    @GET("employees/{employeeId}/shifts")
    suspend fun shifts(@Path("employeeId") employeeId: String): List<ShiftDto>

    @GET("shifts/open")
    suspend fun openShifts(): List<ShiftDto>

    @POST("trade-requests")
    suspend fun createTradeRequest(@Body request: TradeRequestDto): TradeRequestDto
}

@Serializable
data class ShiftDto(
    val id: String,
    val employeeId: String?,
    val role: String,
    val section: String,
    val startsAt: String,
    val endsAt: String,
    val status: String
)

@Serializable
data class TradeRequestDto(
    val id: String,
    val shiftId: String,
    val requesterId: String,
    val recipientId: String?,
    val note: String,
    val status: String,
    val createdAt: String
)
