package com.shiftsync.app.domain.usecase

class DraftSwapNoteUseCase {
    operator fun invoke(reason: String): String {
        val cleanedReason = reason.trim().ifBlank { "a scheduling conflict" }
        return "Hi team, could someone please cover this shift? I am requesting the swap because of $cleanedReason. Thanks for considering it."
    }
}
