package com.chardidathing.glint.data.preferences

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ScrollerPosition { Right, Left, Off }

object ThemePreferences {
    var useMaterialYou by mutableStateOf(false)
        private set

    var scrollerPosition by mutableStateOf(ScrollerPosition.Right)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("glint_settings", Context.MODE_PRIVATE)
        useMaterialYou = prefs.getBoolean("material_you", false)
        scrollerPosition = when (prefs.getString("scroller_position", "right")) {
            "left" -> ScrollerPosition.Left
            "off" -> ScrollerPosition.Off
            else -> ScrollerPosition.Right
        }
    }

    fun setMaterialYou(context: Context, enabled: Boolean) {
        useMaterialYou = enabled
        context.getSharedPreferences("glint_settings", Context.MODE_PRIVATE)
            .edit().putBoolean("material_you", enabled).apply()
    }

    fun setScrollerPosition(context: Context, position: ScrollerPosition) {
        scrollerPosition = position
        val value = when (position) {
            ScrollerPosition.Right -> "right"
            ScrollerPosition.Left -> "left"
            ScrollerPosition.Off -> "off"
        }
        context.getSharedPreferences("glint_settings", Context.MODE_PRIVATE)
            .edit().putString("scroller_position", value).apply()
    }
}
