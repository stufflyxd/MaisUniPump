package com.example.unipump

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.util.*

class TelaConfiguracao_Funcionario : BaseActivity() {

    // Variáveis para componentes
    private lateinit var profileImage: ImageView
    private lateinit var perfilNome: TextView
    private lateinit var personalInfo: TextView
    private lateinit var preferences: TextView
    private lateinit var acessibilidade: TextView
    private lateinit var support: TextView
    private lateinit var logoutButton: TextView
    private lateinit var bottomNavigationView: BottomNavigationView

    // Firebase
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Variável para controlar o tema atual
    private var currentFontTheme: Int = -1

    // Launcher para seleção de imagem
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { onImagePicked(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Guardar o tema atual
        currentFontTheme = FontPreferenceManager.getSelectedFontTheme(this)

        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_configuracao_funcionario)

        // Configurar edge-to-edge (se você usa)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar componentes
        profileImage = findViewById(R.id.profile_image)
        perfilNome = findViewById(R.id.perfil_nome)
        personalInfo = findViewById(R.id.personal_info)
        preferences = findViewById(R.id.preferences)
        acessibilidade = findViewById(R.id.acessibilidade)
        support = findViewById(R.id.support)
        logoutButton = findViewById(R.id.deslogar)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Verificar autenticação
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Carregar dados do funcionário
        carregarDadosFuncionario()

        // Verificar permissões para imagem
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
        }

        // Configurar clique na imagem de perfil
        profileImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Configurar outros cliques
        personalInfo.setOnClickListener {
            try {
                val intent = Intent(this, TelaInformacaoPessoal_funcionario::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Tela não disponível", Toast.LENGTH_SHORT).show()
            }
        }

        preferences.setOnClickListener {
            try {
                val intent = Intent(this, TelaPref::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Tela não disponível", Toast.LENGTH_SHORT).show()
            }
        }

        acessibilidade.setOnClickListener {
            startActivity(Intent(this, TelaAcessibilidade::class.java))
        }

        support.setOnClickListener {
            try {
                val intent = Intent(this, TelaChat::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Tela não disponível", Toast.LENGTH_SHORT).show()
            }
        }

        logoutButton.setOnClickListener {
            mostrarDialogLogout()
        }

        // Configurar bottom navigation
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    val intent = Intent(this, TelaFuncionario::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_chat -> {
                    val intent = Intent(this, TelaChat::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_config -> {
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Verifica se o tema da fonte mudou enquanto estava em outra tela
        val savedFontTheme = FontPreferenceManager.getSelectedFontTheme(this)
        if (savedFontTheme != currentFontTheme) {
            recreate()
        }
    }

    private fun carregarDadosFuncionario() {
        val prefs = getSharedPreferences("funcionarioPrefs", MODE_PRIVATE)
        val funcionarioDocId = prefs.getString("funcionarioDocId", null)

        Log.d("TelaConfigFuncionario", "ID do funcionário: $funcionarioDocId")

        if (funcionarioDocId == null) {
            Log.e("TelaConfigFuncionario", "ID do funcionário não encontrado nas preferências")
            perfilNome.text = "Funcionário"
            profileImage.setImageResource(R.drawable.ic_person)
            return
        }

        db.collection("funcionarios").document(funcionarioDocId)
            .get()
            .addOnSuccessListener { doc ->
                Log.d("TelaConfigFuncionario", "Documento existe: ${doc.exists()}")

                if (doc.exists()) {
                    /*perfilNome.text = doc.getString("nome") ?: "Funcionário"*/
                    val nome = doc.getString("nome") ?: "Funcionário"
                    val nomeUsuario = doc.getString("nome_usuario") ?: ""

                    // Prioridade: nome_usuario se não estiver vazio, senão nome
                    perfilNome.text = if (nomeUsuario.isNotBlank()) {
                        nomeUsuario
                    } else {
                        nome
                    }


                    val path = doc.getString("uri_foto")
                    Log.d("TelaConfigFuncionario", "Caminho da foto no banco: $path")

                    if (!path.isNullOrBlank()) {
                        val file = File(path)
                        Log.d("TelaConfigFuncionario", "Arquivo existe? ${file.exists()}")
                        Log.d("TelaConfigFuncionario", "Caminho completo: ${file.absolutePath}")

                        if (file.exists()) {
                            Log.d("TelaConfigFuncionario", "Carregando foto com Glide...")

                            // 🎯 NOVO: Limpar qualquer imagem anterior
                            profileImage.setImageDrawable(null)

                            Glide.with(this)
                                .load(file)
                                .placeholder(R.drawable.ic_person) // Placeholder enquanto carrega
                                .error(R.drawable.ic_person) // Imagem de erro
                                .circleCrop()
                                .skipMemoryCache(true)
                                .into(profileImage)

                            Log.d("TelaConfigFuncionario", "Comando Glide executado!")
                        } else {
                            Log.w("TelaConfigFuncionario", "Arquivo da foto não encontrado! Usando placeholder")
                            profileImage.setImageResource(R.drawable.ic_person)
                        }
                    } else {
                        Log.d("TelaConfigFuncionario", "Nenhuma foto salva no banco. Usando placeholder")
                        profileImage.setImageResource(R.drawable.ic_person)
                    }
                } else {
                    Log.e("TelaConfigFuncionario", "Documento do funcionário não encontrado!")
                    perfilNome.text = "Funcionário"
                    profileImage.setImageResource(R.drawable.ic_person)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("TelaConfigFuncionario", "Erro ao carregar perfil", exception)
                Toast.makeText(this, "Erro ao carregar perfil", Toast.LENGTH_SHORT).show()
                profileImage.setImageResource(R.drawable.ic_person)
            }
    }

    private fun onImagePicked(uri: Uri) {
        val prefs = getSharedPreferences("funcionarioPrefs", MODE_PRIVATE)
        val funcionarioDocId = prefs.getString("funcionarioDocId", null)

        if (funcionarioDocId == null) {
            Toast.makeText(this, "Erro: ID do funcionário não encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        val destFile = File(filesDir, "profile_${UUID.randomUUID()}_$funcionarioDocId.jpg")

        try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 🎯 NOVO: Limpar imagem anterior antes de carregar nova
            profileImage.setImageDrawable(null)

            Glide.with(this)
                .load(destFile)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .skipMemoryCache(true)
                .into(profileImage)

            // Apaga arquivo antigo se existir
            db.collection("funcionarios").document(funcionarioDocId)
                .get()
                .addOnSuccessListener { doc ->
                    val oldPath = doc.getString("uri_foto")
                    oldPath?.let {
                        val oldFile = File(it)
                        if (oldFile.exists()) oldFile.delete()
                    }

                    db.collection("funcionarios").document(funcionarioDocId)
                        .update("uri_foto", destFile.absolutePath)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Foto salva no perfil", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { ex ->
                            Toast.makeText(this, "Erro ao salvar perfil: ${ex.message}", Toast.LENGTH_LONG).show()
                        }
                }
        } catch (e: Exception) {
            Log.e("TelaConfigFuncionario", "Erro ao processar imagem", e)
            Toast.makeText(this, "Erro ao processar imagem", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() &&
            grantResults[0] != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permissão de fotos negada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogLogout() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_quit_layout, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()

            val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelar)
            val btnConfirmar = dialogView.findViewById<Button>(R.id.btnConfirmar)

            btnCancelar.setOnClickListener {
                dialog.dismiss()
            }

            btnConfirmar.setOnClickListener {
                dialog.dismiss()

                // Logout do Firebase
                auth.signOut()

                // Limpar dados corretos
                val prefs = getSharedPreferences("funcionarioPrefs", MODE_PRIVATE)
                prefs.edit()
                    .remove("uid")
                    .remove("funcionarioDocId")
                    .apply()

                // Navegar corretamente para TelaLogin
                val intent = Intent(this@TelaConfiguracao_Funcionario, TelaLogin::class.java).apply {
                    putExtra("tipo", "funcionario")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finishAffinity()
            }
        } catch (e: Exception) {
            Log.e("TelaConfigFuncionario", "Erro ao mostrar dialog de logout", e)
            Toast.makeText(this, "Erro ao processar logout", Toast.LENGTH_SHORT).show()
        }
    }
}