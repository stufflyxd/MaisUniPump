package com.example.unipump

import android.content.Context
import android.content.SharedPreferences

object FontPreferenceManager {
    private const val PREF_NAME = "font_preferences"
    private const val KEY_SELECTED_FONT = "selected_font_theme"

    // IDs dos temas (devem corresponder aos names dos styles)
    const val FONT_DEFAULT = 0
    const val FONT_HELVETICA = 1
    const val FONT_VERDANA = 2
    const val FONT_MINECRAFT = 3
    const val FONT_MONSIEUR = 4
    const val FONT_NABLA = 5
    const val FONT_REDACTED = 6

    // Mapeamento de ID para nome do tema
    private val fontThemeMap = mapOf(
        FONT_DEFAULT to "Theme.Fonte.Default",
        FONT_HELVETICA to "Theme.Fonte.Helvetica",
        FONT_VERDANA to "Theme.Fonte.Verdana",
        FONT_MINECRAFT to "Theme.Fonte.Minecraft",
        FONT_MONSIEUR to "Theme.Fonte.Monsieur",
        FONT_NABLA to "Theme.Fonte.Nabla",
        FONT_REDACTED to "Theme.Fonte.Redacted"
    )

    // Nomes amigáveis para exibir na UI
    private val fontDisplayNames = mapOf(
        FONT_DEFAULT to "Padrão",
        FONT_HELVETICA to "Helvetica",
        FONT_VERDANA to "Verdana",
        FONT_MINECRAFT to "Minecraft",
        FONT_MONSIEUR to "Monsieur",
        FONT_NABLA to "Nabla",
        FONT_REDACTED to "Redacted"
    )

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Salva a fonte selecionada
     */
    fun setSelectedFontTheme(context: Context, fontThemeId: Int) {
        getPreferences(context)
            .edit()
            .putInt(KEY_SELECTED_FONT, fontThemeId)
            .apply()
    }

    /**
     * Recupera a fonte selecionada (padrão = FONT_DEFAULT)
     */
    fun getSelectedFontTheme(context: Context): Int {
        return getPreferences(context).getInt(KEY_SELECTED_FONT, FONT_DEFAULT)
    }

    /**
     * Converte ID da fonte para resource ID do tema
     */
    fun getThemeResourceId(context: Context, fontThemeId: Int): Int {
        val themeName = fontThemeMap[fontThemeId] ?: fontThemeMap[FONT_DEFAULT]!!

        return try {
            val resources = context.resources
            resources.getIdentifier(themeName, "style", context.packageName)
        } catch (e: Exception) {
            // Fallback para tema padrão se houver erro
            context.resources.getIdentifier("Theme.Fonte.Default", "style", context.packageName)
        }
    }

    /**
     * Recupera o resource ID do tema atual
     */
    fun getCurrentThemeResourceId(context: Context): Int {
        val selectedFont = getSelectedFontTheme(context)
        return getThemeResourceId(context, selectedFont)
    }

    /**
     * Nome amigável da fonte para exibir na UI
     */
    fun getFontDisplayName(fontThemeId: Int): String {
        return fontDisplayNames[fontThemeId] ?: fontDisplayNames[FONT_DEFAULT]!!
    }

    /**
     * Lista de todas as fontes disponíveis
     */
    fun getAllFonts(): List<Pair<Int, String>> {
        return fontDisplayNames.map { (id, name) -> id to name }
    }

    /**
     * Verifica se uma fonte específica existe
     */
    fun fontExists(context: Context, fontThemeId: Int): Boolean {
        return try {
            getThemeResourceId(context, fontThemeId) != 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reseta para a fonte padrão (usado em caso de erro)
     */
    fun resetToDefault(context: Context) {
        setSelectedFontTheme(context, FONT_DEFAULT)
    }

    /**
     * Limpa todas as preferências de fonte
     */
    fun clearPreferences(context: Context) {
        getPreferences(context)
            .edit()
            .clear()
            .apply()
    }
}





/*
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
}*/
