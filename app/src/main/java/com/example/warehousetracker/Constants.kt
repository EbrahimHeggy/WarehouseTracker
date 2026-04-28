package com.example.warehousetracker

import androidx.compose.ui.graphics.Color

val NavyBlue = Color(0xFF1F4E5F)
val LightBg = Color(0xFFF6F8FA)
val GreenColor = Color(0xFF1F9D68)
val RedColor = Color(0xFFD94B4B)
val AmberColor = Color(0xFFE8A317)

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
