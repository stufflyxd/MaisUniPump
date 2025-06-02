package com.example.unipump

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            // Aplica o tema da fonte ANTES de super.onCreate()
            val themeResourceId = FontPreferenceManager.getCurrentThemeResourceId(this)

            Log.d("BaseActivity", "Aplicando tema: $themeResourceId")
            setTheme(themeResourceId)

        } catch (e: Exception) {
            Log.e("BaseActivity", "Erro ao aplicar tema personalizado", e)

            // Se der erro, usar o tema padrão
            try {
                setTheme(R.style.Theme_Fonte_Default)
                FontPreferenceManager.resetToDefault(this)
                Log.d("BaseActivity", "Aplicado tema padrão como fallback")
            } catch (fallbackError: Exception) {
                Log.e("BaseActivity", "Erro até no fallback, usando base theme", fallbackError)
                setTheme(R.style.Base_Theme_UniPump)
            }
        }

        super.onCreate(savedInstanceState)
    }
}






/*
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
}*/
