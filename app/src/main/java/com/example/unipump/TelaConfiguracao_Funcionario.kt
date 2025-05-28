package com.example.unipump

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth


class TelaConfiguracao_Funcionario : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_configuracao_funcionario)
        val profileImage: ImageView = findViewById(R.id.profile_image)

        val personalInfo: TextView = findViewById(R.id.personal_info)
        val preferences: TextView = findViewById(R.id.preferences)
        val support: TextView = findViewById(R.id.support)
        val logoutButton: TextView = findViewById(R.id.deslogar)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)


        personalInfo.setOnClickListener {
            // abrir tela de informaões pessoais
            val intent = Intent(this, TelaInformacaoPessoal_funcionario::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            mostrarDialogLogout(this)
        }

        preferences.setOnClickListener {
            // tela preferencias
            val intent = Intent(this, TelaPref::class.java)
            startActivity(intent)
        }

        support.setOnClickListener {
            // tela chat de suporte
            val intent = Intent(this, TelaChat::class.java)
            startActivity(intent)
        }

//        logoutButton.setOnClickListener {
//            Toast.makeText(this, "Você foi deslogado", Toast.LENGTH_SHORT).show()
//            val intent = Intent(this, TelaInicial::class.java)
//            startActivity(intent)
//            // adicionar lógica para voltar para a tela de login
//        }


        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    // O que acontece quando o item "Início" é clicado
                    val intent = Intent(this, TelaFuncionario::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_chat -> {
                    // Abre a tela de chat
                    val intent = Intent(this, TelaChat::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_config -> {
                    // Abre a tela de configurações
                    true
                }

                else -> false
            }
        }

    }

    private fun mostrarDialogLogout(context: Context) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_quit_layout, null)

        val dialog = AlertDialog.Builder(context)
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
            // Ação para deslogar
            Toast.makeText(context, "Deslogando...", Toast.LENGTH_SHORT).show()

            val prefs = getSharedPreferences("funcionarioPrefs", MODE_PRIVATE)
            prefs.edit().clear().apply()

            val prefs1 = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            prefs1.edit().clear().apply()

            FirebaseAuth.getInstance().signOut()
            val intent = Intent(context, TelaLogin::class.java)
            context.startActivity(intent)
            intent.putExtra("tipo", "funcionario") // Adicione isso
            startActivity(intent)
        }
    }
}