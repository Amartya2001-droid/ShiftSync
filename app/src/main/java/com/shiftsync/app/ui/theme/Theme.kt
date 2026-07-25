package com.shiftsync.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ShiftSyncColors = lightColorScheme(
    primary = Color(0xFF0E6F68),
    onPrimary = Color.White,
    secondary = Color(0xFF7A4E12),
    secondaryContainer = Color(0xFFFFE8C2),
    surface = Color(0xFFFFFBF5),
    surfaceVariant = Color(0xFFF1F4EF),
    background = Color(0xFFFFFBF5)
)

@Composable
fun ShiftSyncTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShiftSyncColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
