package com.example.unipump

import android.content.Context
import android.content.SharedPreferences

object FontPreferenceManager {
    private const val PREFS_NAME = "unipump_font_settings"
    private const val KEY_SELECTED_FONT_THEME_ID = "selected_font_theme_id"

    val DEFAULT_FONT_THEME_ID = R.style.Base_Theme_UniPump

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveSelectedFontTheme(context: Context, themeResId: Int) {
        getPreferences(context).edit().putInt(KEY_SELECTED_FONT_THEME_ID, themeResId).apply()
    }

    fun getSelectedFontTheme(context: Context): Int {
        return getPreferences(context).getInt(KEY_SELECTED_FONT_THEME_ID, DEFAULT_FONT_THEME_ID)
    }

    fun resetToDefault(context: Context) {
        getPreferences(context).edit().clear().apply()
    }
}