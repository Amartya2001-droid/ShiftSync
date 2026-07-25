package com.shiftsync.app.domain.model

sealed interface EligibilityResult {
    data object Eligible : EligibilityResult
    data class Ineligible(val reason: String) : EligibilityResult
}
