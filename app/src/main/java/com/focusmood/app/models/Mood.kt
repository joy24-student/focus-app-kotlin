package com.focusmood.app.models

import android.graphics.Color

enum class Mood(
    val key: String,
    val displayName: String,
    val quote: String,
    val primaryColorHex: String,
    val bgColorHex: String,
    val cardColorHex: String,
    val textColorHex: String,
    val mutedColorHex: String,
    val subColorHex: String
) {
    FOREST(
        key = "forest",
        displayName = "🌿 Forest",
        quote = "\"Quiet your mind, like the still trees in an ancient forest.\"",
        primaryColorHex = "#10b981",
        bgColorHex = "#0b1712",
        cardColorHex = "#12261d",
        textColorHex = "#f0fdf4",
        mutedColorHex = "#86efac",
        subColorHex = "#a7f3d0"
    ),
    RAIN(
        key = "rain",
        displayName = "🌧️ Rain",
        quote = "\"Like raindrops falling steadily, small efforts accumulate into greatness.\"",
        primaryColorHex = "#38bdf8",
        bgColorHex = "#0c121e",
        cardColorHex = "#152033",
        textColorHex = "#f0f9ff",
        mutedColorHex = "#93c5fd",
        subColorHex = "#bae6fd"
    ),
    SUNSET(
        key = "sunset",
        displayName = "🌅 Sunset",
        quote = "\"Honor the rhythm of work and rest. Each sunset brings peace.\"",
        primaryColorHex = "#fb923c",
        bgColorHex = "#1c0f16",
        cardColorHex = "#2b141f",
        textColorHex = "#fff7ed",
        mutedColorHex = "#fdba74",
        subColorHex = "#fed7aa"
    ),
    MIDNIGHT(
        key = "midnight",
        displayName = "🌌 Midnight",
        quote = "\"The stars shine brightest in the stillness of deep night.\"",
        primaryColorHex = "#a855f7",
        bgColorHex = "#070714",
        cardColorHex = "#181230",
        textColorHex = "#faf5ff",
        mutedColorHex = "#d8b4fe",
        subColorHex = "#e9d5ff"
    ),
    MINIMAL(
        key = "minimal",
        displayName = "⚡ Minimal",
        quote = "\"Eliminate the unnecessary so that the essential may speak.\"",
        primaryColorHex = "#e5e5e5",
        bgColorHex = "#111111",
        cardColorHex = "#1c1c1c",
        textColorHex = "#ffffff",
        mutedColorHex = "#a3a3a3",
        subColorHex = "#d4d4d4"
    );

    val primaryColor: Int get() = Color.parseColor(primaryColorHex)
    val bgColor: Int get() = Color.parseColor(bgColorHex)
    val cardColor: Int get() = Color.parseColor(cardColorHex)
    val textColor: Int get() = Color.parseColor(textColorHex)
    val mutedColor: Int get() = Color.parseColor(mutedColorHex)
    val subColor: Int get() = Color.parseColor(subColorHex)

    companion object {
        fun fromKey(key: String?): Mood {
            return values().firstOrNull { it.key.equals(key, ignoreCase = true) } ?: FOREST
        }
    }
}
