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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.util.*

class TelaConfig : BaseActivity() {
    private lateinit var profileImage: ImageView
    private lateinit var perfilNome: TextView
    private lateinit var personalInfo: TextView
    private lateinit var solicitarFicha: TextView
    private lateinit var preferences: TextView
    private lateinit var acessibilidade: TextView
    private lateinit var support: TextView
    private lateinit var logoutButton: TextView
    private lateinit var bottomNavigationView: BottomNavigationView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Variável para controlar o tema atual
    private var currentFontTheme: Int = -1

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { onImagePicked(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Guardar o tema atual
        currentFontTheme = FontPreferenceManager.getSelectedFontTheme(this)

        setContentView(R.layout.activity_tela_config)

        profileImage = findViewById(R.id.profile_image)
        perfilNome = findViewById(R.id.perfil_nome)
        personalInfo = findViewById(R.id.personal_info)
        solicitarFicha = findViewById(R.id.solicitar_ficha)
        preferences = findViewById(R.id.preferences)
        acessibilidade = findViewById(R.id.acessibilidade)
        support = findViewById(R.id.support)
        logoutButton = findViewById(R.id.deslogar)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        carregarDadosUsuario()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
        }

        profileImage.setOnClickListener { pickImageLauncher.launch("image/*") }

        personalInfo.setOnClickListener {
            startActivity(Intent(this, TelaInformacoesPessoaisAluno::class.java))
        }

        solicitarFicha.setOnClickListener {
            mostrarDialogSolicitarFicha()
        }

        preferences.setOnClickListener {
            startActivity(Intent(this, TelaPref::class.java))
        }

        acessibilidade.setOnClickListener {
            startActivity(Intent(this, TelaAcessibilidade::class.java))
        }

        support.setOnClickListener {
            startActivity(Intent(this, TelaChat::class.java))
        }

        logoutButton.setOnClickListener {
            mostrarDialogLogout()
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    startActivity(Intent(this, TelaPrincipalAluno::class.java))
                    true
                }
                R.id.nav_treinos -> {
                    startActivity(Intent(this, TelaTreinoAluno::class.java))
                    true
                }
                R.id.nav_chat -> {
                    startActivity(Intent(this, TelaChat::class.java))
                    true
                }
                R.id.nav_config -> true
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

    private fun carregarDadosUsuario() {
        val alunoDocId = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            .getString("alunoDocId", null) ?: return

        Log.d("TelaConfig", "Carregando dados do aluno: $alunoDocId")

        db.collection("alunos").document(alunoDocId)
            .get()
            .addOnSuccessListener { doc ->
                val nome = doc.getString("nome") ?: "Usuário"
                val nomeUsuario = doc.getString("nome_usuario") ?: ""

                // Prioridade: nome_usuario se não estiver vazio, senão nome
                perfilNome.text = if (nomeUsuario.isNotBlank()) {
                    nomeUsuario
                } else {
                    nome
                }

                val path = doc.getString("uri_foto")
                Log.d("TelaConfig", "Caminho da foto no banco: $path")

                if (!path.isNullOrBlank()) {
                    val file = File(path)
                    Log.d("TelaConfig", "Arquivo existe? ${file.exists()}")
                    Log.d("TelaConfig", "Caminho completo: ${file.absolutePath}")

                    if (file.exists()) {
                        Log.d("TelaConfig", "Carregando foto com Glide...")

                        // Limpar qualquer imagem anterior
                        profileImage.setImageDrawable(null)

                        Glide.with(this)
                            .load(file)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .skipMemoryCache(true)
                            .into(profileImage)

                        Log.d("TelaConfig", "Comando Glide executado!")
                    } else {
                        Log.w("TelaConfig", "Arquivo da foto não encontrado! Usando placeholder")
                        profileImage.setImageResource(R.drawable.ic_person)
                    }
                } else {
                    Log.d("TelaConfig", "Nenhuma foto salva no banco. Usando placeholder")
                    profileImage.setImageResource(R.drawable.ic_person)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("TelaConfig", "Erro ao carregar perfil", exception)
                Toast.makeText(this, "Erro ao carregar perfil", Toast.LENGTH_SHORT).show()
                profileImage.setImageResource(R.drawable.ic_person)
            }
    }

    private fun onImagePicked(uri: Uri) {
        val alunoDocId = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            .getString("alunoDocId", null) ?: return

        val destFile = File(filesDir, "profile_${UUID.randomUUID()}_$alunoDocId.jpg")

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

        db.collection("alunos").document(alunoDocId)
            .get()
            .addOnSuccessListener { doc ->
                val oldPath = doc.getString("uri_foto")
                oldPath?.let {
                    val oldFile = File(it)
                    if (oldFile.exists()) oldFile.delete()
                }

                db.collection("alunos").document(alunoDocId)
                    .update("uri_foto", destFile.absolutePath)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Foto salva no perfil", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { ex ->
                        Toast.makeText(this, "Erro ao salvar perfil: ${ex.message}", Toast.LENGTH_LONG).show()
                    }
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

    private fun mostrarDialogSolicitarFicha() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_solicitar_ficha, null)
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
            enviarSolicitacaoFicha()
        }
    }

    private fun enviarSolicitacaoFicha() {
        val alunoDocId = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            .getString("alunoDocId", null) ?: return

        db.collection("alunos").document(alunoDocId)
            .get()
            .addOnSuccessListener { alunoDoc ->
                val nomeAluno = alunoDoc.getString("nome") ?: "Usuário"

                val notificacao = hashMapOf(
                    "tipo" to "solicitacao_ficha",
                    "alunoId" to alunoDocId,
                    "nomeAluno" to nomeAluno,
                    "mensagem" to "$nomeAluno solicitou uma nova ficha de treino",
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "lida" to false
                )

                db.collection("notificacoes_funcionario")
                    .add(notificacao)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Solicitação enviada com sucesso! Aguarde o retorno.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .addOnFailureListener { ex ->
                        Toast.makeText(
                            this,
                            "Erro ao enviar solicitação: ${ex.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { ex ->
                Toast.makeText(
                    this,
                    "Erro ao buscar dados do aluno: ${ex.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun mostrarDialogLogout() {
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

            auth.signOut()

            val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            prefs.edit()
                .remove("uid")
                .remove("alunoDocId")
                .apply()

            val intent = Intent(this@TelaConfig, TelaLogin::class.java).apply {
                putExtra("tipo", "aluno")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finishAffinity()
        }
    }
}