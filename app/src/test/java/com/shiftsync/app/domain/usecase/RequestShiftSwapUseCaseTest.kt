package com.shiftsync.app.domain.usecase

import com.shiftsync.app.domain.model.TradeRequest
import com.shiftsync.app.domain.model.TradeRequestStatus
import com.shiftsync.app.domain.repository.ScheduleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class RequestShiftSwapUseCaseTest {
    private val repository = mockk<ScheduleRepository>()
    private val useCase = RequestShiftSwapUseCase(repository)

    @Test
    fun `uses assistant generated note when smart note is enabled`() = runTest {
        val request = request()
        coEvery { repository.generateSwapNote("exam conflict") } returns "Polite generated note"
        coEvery { repository.requestSwap("shift-1", "emp-2", "Polite generated note") } returns request

        val result = useCase("shift-1", "emp-2", "exam conflict", useSmartNote = true)

        assertEquals(request, result)
        coVerify { repository.generateSwapNote("exam conflict") }
        coVerify { repository.requestSwap("shift-1", "emp-2", "Polite generated note") }
    }

    @Test
    fun `posts to open board when no recipient is selected`() = runTest {
        val request = request(recipientId = null)
        coEvery { repository.postOpenShift("shift-1", "Need coverage") } returns request

        val result = useCase("shift-1", null, "Need coverage", useSmartNote = false)

        assertEquals(request, result)
        coVerify { repository.postOpenShift("shift-1", "Need coverage") }
    }

    private fun request(recipientId: String? = "emp-2"): TradeRequest {
        return TradeRequest(
            id = "trade-1",
            shiftId = "shift-1",
            requesterId = "emp-1",
            recipientId = recipientId,
            note = "Need coverage",
            status = TradeRequestStatus.Pending,
            createdAt = Instant.parse("2026-07-20T12:00:00Z")
        )
    }
}
