package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class TelaInicial : AppCompatActivity() {

    private lateinit var btnAluno: Button
    private lateinit var btnFuncionario: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CicloDeVida", "onCreate chamado")
        setContentView(R.layout.activity_tela_inicial)

        // Inicializa os componentes da UI
        btnAluno = findViewById(R.id.btnAluno)
        btnFuncionario = findViewById(R.id.btnFuncionario)

        auth = Firebase.auth

        // Configura os eventos
        configurarEventos()
    }

    override fun onStart() {
        super.onStart()
        Log.d("CicloDeVida", "onStart chamado")

        // Verifica se há usuário logado e redireciona diretamente
        verificarUsuarioLogado()
    }

    override fun onResume() {
        super.onResume()
        Log.d("CicloDeVida", "onResume chamado")
    }

    override fun onPause() {
        super.onPause()
        Log.d("CicloDeVida", "onPause chamado")
    }

    override fun onStop() {
        super.onStop()
        Log.d("CicloDeVida", "onStop chamado")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CicloDeVida", "onDestroy chamado")
    }

    private fun verificarUsuarioLogado() {
        val usuarioAtual = auth.currentUser

        if (usuarioAtual != null) {
            // Verifica nas duas SharedPreferences possíveis
            val prefsAluno = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            val prefsFuncionario = getSharedPreferences("funcionarioPrefs", MODE_PRIVATE)

            val tipoAluno = prefsAluno.getString("tipo", null)
            val tipoFuncionario = prefsFuncionario.getString("tipo", null)

            when {
                tipoAluno == "aluno" -> {
                    Log.d("AutoLogin", "Redirecionando para TelaPrincipalAluno")
                    val intent = Intent(this, TelaPrincipalAluno::class.java)
                    startActivity(intent)
                    finish() // Encerra esta activity
                }
                tipoFuncionario == "funcionario" -> {
                    Log.d("AutoLogin", "Redirecionando para TelaFuncionario")
                    val intent = Intent(this, TelaFuncionario::class.java)
                    startActivity(intent)
                    finish() // Encerra esta activity
                }
                else -> {
                    // Usuário logado mas sem dados salvos - força logout
                    Log.d("AutoLogin", "Usuário sem dados salvos - fazendo logout")
                    auth.signOut()
                    limparPreferencias()
                }
            }
        }
        // Se não há usuário logado, permanece na tela inicial normalmente
    }

    private fun limparPreferencias() {
        val prefsAluno = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val prefsFuncionario = getSharedPreferences("funcionarioPrefs", MODE_PRIVATE)

        prefsAluno.edit().clear().apply()
        prefsFuncionario.edit().clear().apply()
    }

    private fun configurarEventos() {
        btnAluno.setOnClickListener {
            val intent = Intent(this, TelaLogin::class.java)
            intent.putExtra("tipo", "aluno")
            startActivity(intent)
        }

        btnFuncionario.setOnClickListener {
            val intent = Intent(this, TelaLogin::class.java)
            intent.putExtra("tipo", "funcionario")
            startActivity(intent)
        }
    }
}