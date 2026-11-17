package com.shiftsync.app.presentation

import com.shiftsync.app.domain.model.RequestHistoryItem

// A display snapshot: if the visible offer changes, the prompt must be reviewed again.
data class RequestResponseConfirmation(val item: RequestHistoryItem, val accept: Boolean)
