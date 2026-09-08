package com.ayng.kebiao.ui

import androidx.compose.ui.graphics.Color

fun parseColorSafe(hex: String): Color? {
    if (hex.isBlank()) return null
    return try {
        Color(android.graphics.Color.parseColor(hex.trim()))
    } catch (_: IllegalArgumentException) {
        null
    }
}
