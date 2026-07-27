package com.shiftsync.app.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DraftSwapNoteUseCaseTest {
    private val useCase = DraftSwapNoteUseCase()

    @Test
    fun `drafts polite swap note with provided reason`() {
        val note = useCase("university exam conflict")

        assertTrue(note.contains("university exam conflict"))
        assertTrue(note.startsWith("Hi team"))
    }

    @Test
    fun `falls back to scheduling conflict when reason is blank`() {
        val note = useCase("   ")

        assertEquals(
            "Hi team, could someone please cover this shift? I am requesting the swap because of a scheduling conflict. Thanks for considering it.",
            note
        )
    }
}
