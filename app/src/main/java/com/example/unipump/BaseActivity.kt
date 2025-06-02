package com.example.unipump

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            // Aplica o tema da fonte ANTES de super.onCreate()
            val selectedFontTheme = FontPreferenceManager.getSelectedFontTheme(this)
            setTheme(selectedFontTheme)
        } catch (e: Exception) {
            // Se der erro, usar o tema padrão e resetar preferências
            setTheme(R.style.Base_Theme_UniPump)
            FontPreferenceManager.resetToDefault(this)
        }
        super.onCreate(savedInstanceState)
    }
}