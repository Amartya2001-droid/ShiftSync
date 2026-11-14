package com.shiftsync.app.domain.usecase

import com.shiftsync.app.data.repository.SeedData
import com.shiftsync.app.domain.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class BuildRequestHistoryUseCaseTest {
    private fun request(id: String, from: String, to: String?, seconds: Long) = TradeRequest(
        id, "shift-1", from, to, "Saved note", TradeRequestStatus.Pending, Instant.ofEpochSecond(seconds)
    )

    @Test
    fun `includes sent and received requests newest first and excludes unrelated requests`() {
        val sent = request("sent", "emp-maya", null, 10)
        val received = request("received", "emp-sam", "emp-maya", 20)
        val unrelated = request("other", "emp-jon", "emp-sam", 30)
        val result = BuildRequestHistoryUseCase()("emp-maya", listOf(sent, unrelated, received), SeedData.shifts(), SeedData.employees)
        assertEquals(listOf("received", "sent"), result.map { it.request.id })
        assertEquals("Sam Rivera", result.first().requesterName)
        assertEquals("Maya Chen", result.first().recipientName)
        assertEquals("Patio", result.first().shift!!.section)
        assertNull(result.last().recipientName)
    }

    @Test
    fun `retains status and note when related records are unavailable and breaks time ties by id`() {
        val a = request("a", "missing", "emp-maya", 10).copy(status = TradeRequestStatus.Rejected)
        val b = a.copy(id = "b", status = TradeRequestStatus.Accepted)
        val result = BuildRequestHistoryUseCase()("emp-maya", listOf(b, a), emptyList(), emptyList())
        assertEquals(listOf("a", "b"), result.map { it.request.id })
        assertNull(result.first().shift)
        assertEquals("Unknown employee", result.first().requesterName)
        assertEquals("Saved note", result.first().request.note)
        assertEquals(TradeRequestStatus.Rejected, result.first().request.status)
    }
}
