package com.example.unipump

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.util.*

class TelaConfig : AppCompatActivity() {
    private lateinit var profileImage: ImageView
    private lateinit var perfilNome: TextView
    private lateinit var personalInfo: TextView
    private lateinit var trainingData: TextView
    private lateinit var preferences: TextView
    private lateinit var support: TextView
    private lateinit var logoutButton: TextView
    private lateinit var bottomNavigationView: BottomNavigationView

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { onImagePicked(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_config)

        profileImage         = findViewById(R.id.profile_image)
        perfilNome           = findViewById(R.id.perfil_nome)
        personalInfo         = findViewById(R.id.personal_info)
        trainingData         = findViewById(R.id.training_data)
        preferences          = findViewById(R.id.preferences)
        support              = findViewById(R.id.support)
        logoutButton         = findViewById(R.id.deslogar)
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
        trainingData.setOnClickListener {
            startActivity(Intent(this, TelaDadosDeTreino::class.java))
        }
        preferences.setOnClickListener {
            startActivity(Intent(this, TelaPref::class.java))
        }
        support.setOnClickListener {
            startActivity(Intent(this, TelaChat::class.java))
        }
        logoutButton.setOnClickListener {
            mostrarDialogLogout()
        }
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio  -> {
                    startActivity(Intent(this, TelaPrincipalAluno::class.java))
                    true
                }
                R.id.nav_treinos -> {
                    startActivity(Intent(this, TelaTreinoAluno::class.java))
                    true
                }
                R.id.nav_chat    -> {
                    startActivity(Intent(this, TelaChat::class.java))
                    true
                }
                R.id.nav_config  -> true
                else             -> false
            }
        }
    }

    private fun carregarDadosUsuario() {
        val alunoDocId = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            .getString("alunoDocId", null) ?: return

        db.collection("alunos").document(alunoDocId)
            .get()
            .addOnSuccessListener { doc ->
                perfilNome.text = doc.getString("nome") ?: "Usuário"
                val path = doc.getString("uri_foto")
                if (!path.isNullOrBlank()) {
                    val file = File(path)
                    if (file.exists()) {
                        Glide.with(this)
                            .load(file)
                            .circleCrop()
                            .skipMemoryCache(true)
                            .into(profileImage)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar perfil", Toast.LENGTH_SHORT).show()
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

        Glide.with(this)
            .load(destFile)
            .circleCrop()
            .skipMemoryCache(true)
            .into(profileImage)

        // Apaga arquivo antigo se existir
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

            // Logout do Firebase
            auth.signOut()

            // Limpar apenas dados sensíveis (opcional - pode comentar se não quiser limpar)
            val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            prefs.edit()
                .remove("uid")
                .remove("alunoDocId")
                .apply()

            // Navegar corretamente para TelaLogin
            val intent = Intent(this@TelaConfig, TelaLogin::class.java).apply {
                putExtra("tipo", "aluno")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finishAffinity()
        }
    }
}