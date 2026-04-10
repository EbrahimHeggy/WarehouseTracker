package com.example.warehousetracker

import androidx.compose.ui.graphics.Color

val NavyBlue = Color(0xFF1a3a5c)
val LightBg = Color(0xFFF4F6F9)
val GreenColor = Color(0xFF4CAF50)
val RedColor = Color(0xFFE53935)
val AmberColor = Color(0xFFFFA000)

fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}
