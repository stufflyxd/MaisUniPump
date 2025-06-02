package com.example.unipump

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.RadioButton
import android.widget.RadioGroup

class TelaAcessibilidade : BaseActivity() {

    private lateinit var fontRadioGroup: RadioGroup
    private lateinit var radioDefaultFont: RadioButton
    private lateinit var radioHelvetica: RadioButton
    private lateinit var radioVerdana: RadioButton
    private lateinit var radioMinecraft: RadioButton
    private lateinit var radioMonsieur: RadioButton
    private lateinit var radioNabla: RadioButton
    private lateinit var radioRedacted: RadioButton
    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_acessibilidade)

        // Configura o padding para edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar todas as views
        initializeViews()

        // Configurar listeners
        setupClickListeners()

        // Configura o estado inicial da seleção de fonte
        setupInitialFontSelection()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.SetaVoltar)
        fontRadioGroup = findViewById(R.id.font_selection_radio_group)
        radioDefaultFont = findViewById(R.id.radio_default_font)
        radioHelvetica = findViewById(R.id.radio_helvetica)
        radioVerdana = findViewById(R.id.radio_verdana)
        radioMinecraft = findViewById(R.id.radio_minecraft)
        radioMonsieur = findViewById(R.id.radio_monsieur)
        radioNabla = findViewById(R.id.radio_nabla)
        radioRedacted = findViewById(R.id.radio_redacted)
    }

    private fun setupClickListeners() {
        // Configurar o listener do back button
        backButton.setOnClickListener {
            finish()
        }

        // Configura o listener para mudanças no RadioGroup
        fontRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedThemeResId = when (checkedId) {
                R.id.radio_default_font -> R.style.Fonte_Default
                R.id.radio_helvetica -> R.style.Theme_Fonte_Helvetica
                R.id.radio_verdana -> R.style.Theme_Fonte_Verdana
                R.id.radio_minecraft -> R.style.Theme_Fonte_Minecraft
                R.id.radio_monsieur -> R.style.Theme_Fonte_Monsieur
                R.id.radio_nabla -> R.style.Theme_Fonte_Nabla
                R.id.radio_redacted -> R.style.Theme_Fonte_Redacted
                else -> R.style.Base_Theme_UniPump
            }

            // Salvar a preferência
            FontPreferenceManager.saveSelectedFontTheme(this, selectedThemeResId)

            // Recriar a activity para aplicar a nova fonte
            recreate()
        }
    }

    private fun setupInitialFontSelection() {
        val currentFontTheme = FontPreferenceManager.getSelectedFontTheme(this)
        val radioButtonId = when (currentFontTheme) {
            R.style.Base_Theme_UniPump -> R.id.radio_default_font
            R.style.Fonte_Default -> R.id.radio_default_font
            R.style.Theme_Fonte_Helvetica -> R.id.radio_helvetica
            R.style.Theme_Fonte_Verdana -> R.id.radio_verdana
            R.style.Theme_Fonte_Minecraft -> R.id.radio_minecraft
            R.style.Theme_Fonte_Monsieur -> R.id.radio_monsieur
            R.style.Theme_Fonte_Nabla -> R.id.radio_nabla
            R.style.Theme_Fonte_Redacted -> R.id.radio_redacted
            else -> R.id.radio_default_font
        }

        // Temporariamente remover o listener para evitar chamadas desnecessárias
        fontRadioGroup.setOnCheckedChangeListener(null)
        fontRadioGroup.check(radioButtonId)

        fontRadioGroup.check(radioButtonId)
    }
}