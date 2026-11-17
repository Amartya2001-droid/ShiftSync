package com.shiftsync.app.domain.usecase

import com.shiftsync.app.data.repository.InMemoryScheduleRepository
import com.shiftsync.app.data.repository.SeedData
import com.shiftsync.app.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class FilterRequestHistoryUseCaseTest {
    private val filter = FilterRequestHistoryUseCase()
    private fun item(id: String, from: String, to: String?, status: TradeRequestStatus) = RequestHistoryItem(
        TradeRequest(id, "shift-1", from, to, "Saved note", status, Instant.EPOCH),
        null, from, to
    )

    @Test
    fun `views distinguish incoming pending sent and all resolved statuses without leaking unrelated history`() {
        val history = listOf(
            item("received", "emp-sam", "emp-maya", TradeRequestStatus.Pending),
            item("posted", "emp-maya", null, TradeRequestStatus.Pending),
            item("accepted", "emp-jon", "emp-maya", TradeRequestStatus.Accepted),
            item("declined", "emp-maya", "emp-sam", TradeRequestStatus.Rejected),
            item("cancelled", "emp-maya", "emp-sam", TradeRequestStatus.Cancelled),
            item("unrelated", "emp-jon", "emp-sam", TradeRequestStatus.Pending)
        )
        val all = filter("emp-maya", history, RequestHistoryFilter.All)
        assertEquals(listOf("received", "posted", "accepted", "declined", "cancelled"), all.items.map { it.request.id })
        assertEquals(mapOf(RequestHistoryFilter.All to 5, RequestHistoryFilter.NeedsResponse to 1,
            RequestHistoryFilter.Sent to 3, RequestHistoryFilter.Resolved to 3), all.counts)
        assertEquals(listOf("received"), filter("emp-maya", history, RequestHistoryFilter.NeedsResponse).items.map { it.request.id })
        assertEquals(listOf("posted", "declined", "cancelled"), filter("emp-maya", history, RequestHistoryFilter.Sent).items.map { it.request.id })
        assertEquals(listOf("accepted", "declined", "cancelled"), filter("emp-maya", history, RequestHistoryFilter.Resolved).items.map { it.request.id })
        assertEquals(all.counts, filter("emp-maya", history, RequestHistoryFilter.Resolved).counts)
        assertEquals(6, history.size)
        assertEquals("Saved note", all.items.first().request.note)
    }

    @Test
    fun `changing employee changes scope and open board or self addressed offers do not need response`() {
        val history = listOf(item("targeted", "emp-maya", "emp-sam", TradeRequestStatus.Pending),
            item("board", "emp-maya", null, TradeRequestStatus.Pending),
            item("self", "emp-sam", "emp-sam", TradeRequestStatus.Pending))
        assertTrue(filter("emp-maya", history, RequestHistoryFilter.NeedsResponse).items.isEmpty())
        assertEquals(listOf("targeted"), filter("emp-sam", history, RequestHistoryFilter.NeedsResponse).items.map { it.request.id })
        assertTrue(filter("emp-lina", history, RequestHistoryFilter.All).items.isEmpty())
        assertTrue(filter("emp-lina", history, RequestHistoryFilter.All).counts.values.all { it == 0 })
        assertTrue(filter("emp-sam", emptyList(), RequestHistoryFilter.All).counts.values.all { it == 0 })
    }

    @Test
    fun `response and withdrawal transitions update views while keeping resolved history`() = runTest {
        val repository = InMemoryScheduleRepository()
        val sender = SeedData.currentEmployeeId
        val decline = repository.requestSwap("shift-1", "emp-sam", "Decline me", sender)
        fun selected(history: List<RequestHistoryItem>, view: RequestHistoryFilter) = filter("emp-sam", history, view)
        assertEquals(1, selected(repository.observeRequestHistory("emp-sam").first(), RequestHistoryFilter.NeedsResponse).items.size)
        repository.respondToRequest(decline.id, "emp-sam", false)
        val accept = repository.requestSwap("shift-3", "emp-sam", "Accept me", sender)
        repository.respondToRequest(accept.id, "emp-sam", true)
        val cancel = repository.requestSwap("shift-2", "emp-sam", "Withdraw me", sender)
        repository.cancelRequest(cancel.id, sender)
        val history = repository.observeRequestHistory("emp-sam").first()
        assertTrue(selected(history, RequestHistoryFilter.NeedsResponse).items.isEmpty())
        assertEquals(3, selected(history, RequestHistoryFilter.Resolved).items.size)
        assertEquals(setOf(TradeRequestStatus.Accepted, TradeRequestStatus.Rejected, TradeRequestStatus.Cancelled),
            selected(history, RequestHistoryFilter.Resolved).items.map { it.request.status }.toSet())
        assertEquals(setOf("Decline me", "Accept me", "Withdraw me"), history.map { it.request.note }.toSet())
    }
}
