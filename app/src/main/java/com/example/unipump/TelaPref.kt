package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView // Mantido para backButton
import android.widget.Switch    // Mantido para themeSwitch
import android.widget.TextView  // Mantido para as outras TextViews
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate // Import necessário
import com.google.android.material.bottomnavigation.BottomNavigationView

// Importe o ThemePreferenceManager se estiver em outro pacote
// import com.example.unipump.util.ThemePreferenceManager
// Removido import não utilizado: com.google.android.material.bottomnavigation.BottomNavigationView

class TelaPref : BaseActivity() {

    private lateinit var backButton: ImageView
    private lateinit var languageOption: TextView
    private lateinit var themeSwitch: Switch
    private lateinit var themeValueText: TextView // TextView para "(Tema Escuro)" etc.
    private lateinit var aboutOption: TextView
/*    private lateinit var clearCacheOption: TextView*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_pref)

        backButton = findViewById(R.id.SetaVoltarTelaCriarFicha)
        languageOption = findViewById(R.id.language_option)
        themeSwitch = findViewById(R.id.theme_switch)
        themeValueText = findViewById(R.id.theme_value) // Certifique-se que este ID existe no XML
        aboutOption = findViewById(R.id.about_option)
        /*clearCacheOption = findViewById(R.id.clear_cache_option)*/

        // --- NOVO: Configura o estado inicial do Switch e do texto do tema ---
        val currentThemeMode = ThemePreferenceManager.getThemeMode(this)
        themeSwitch.isChecked = (currentThemeMode == AppCompatDelegate.MODE_NIGHT_YES)
        updateThemeValueText(themeSwitch.isChecked)
        // --- FIM NOVO ---

        backButton.setOnClickListener {
            finish()
        }

        languageOption.setOnClickListener {
            val intent = Intent(this, TelaIdioma::class.java)
            startActivity(intent)
        }

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val newNightMode = if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }

            // --- NOVO: Salva a escolha do usuário ---
            ThemePreferenceManager.saveThemeMode(this, newNightMode)
            // --- FIM NOVO ---

            // Aplica a mudança de tema para a sessão atual
            AppCompatDelegate.setDefaultNightMode(newNightMode)

            // Atualiza o texto e mostra o Toast
            updateThemeValueText(isChecked) // Atualiza o texto "(Tema Escuro)/(Tema Claro)"
            val themeUserText = if (isChecked) "Escuro" else "Claro"
            Toast.makeText(this, "Tema $themeUserText ativado", Toast.LENGTH_SHORT).show()
        }

        aboutOption.setOnClickListener {
            val intent = Intent(this, TelaSobre::class.java)
            startActivity(intent)
        }

       /* clearCacheOption.setOnClickListener {
            Toast.makeText(this, "Cache limpo", Toast.LENGTH_SHORT).show()
        }*/
    }

    // Função auxiliar para atualizar o texto "(Tema Escuro)" ou "(Tema Claro)"
    private fun updateThemeValueText(isDarkMode: Boolean) {
        themeValueText.text = if (isDarkMode) "(Tema Escuro)" else "(Tema Claro)"
    }
}