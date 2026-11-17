package ru.ritm.planner.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Appearance(val lightTheme: Boolean = false, val accent: Int = DEFAULT_ACCENT) {
    companion object { const val DEFAULT_ACCENT: Int = -4263324 } // #BEF264
}

class AppearancePreferences(context: Context) {
    private val preferences = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(Appearance(
        preferences.getBoolean("light_theme", false),
        preferences.getInt("accent", Appearance.DEFAULT_ACCENT),
    ))
    val appearance = state.asStateFlow()

    fun setLightTheme(enabled: Boolean) {
        preferences.edit().putBoolean("light_theme", enabled).apply()
        state.value = state.value.copy(lightTheme = enabled)
    }

    fun setAccent(color: Int) {
        val opaque = color or (0xFF shl 24)
        preferences.edit().putInt("accent", opaque).apply()
        state.value = state.value.copy(accent = opaque)
    }
}
