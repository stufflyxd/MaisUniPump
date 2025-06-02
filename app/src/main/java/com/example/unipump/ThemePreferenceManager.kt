package com.example.unipump // Ou com.example.unipump.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemePreferenceManager : BaseActivity() {

    private const val PREFS_NAME = "unipump_theme_settings" // Nome único para as preferências
    private const val KEY_THEME_MODE = "selected_theme_mode"

    // app começa escuro por padrão:
    const val DEFAULT_THEME_MODE = AppCompatDelegate.MODE_NIGHT_YES
    // app começa claro por padrão:
    // const val DEFAULT_THEME_MODE = AppCompatDelegate.MODE_NIGHT_NO

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveThemeMode(context: Context, mode: Int) {
        val editor = getPreferences(context).edit()
        editor.putInt(KEY_THEME_MODE, mode)
        editor.apply()
    }

    fun getThemeMode(context: Context): Int {
        return getPreferences(context).getInt(KEY_THEME_MODE, DEFAULT_THEME_MODE)
    }
}