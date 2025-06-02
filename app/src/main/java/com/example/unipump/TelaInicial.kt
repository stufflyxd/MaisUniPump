package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class TelaInicial : BaseActivity() {

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
        Log.d("AutoLogin", "Usuário atual: ${usuarioAtual?.email}")

        if (usuarioAtual != null) {
            // Verifica nas duas SharedPreferences possíveis
            val prefsAluno = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            val prefsFuncionario = getSharedPreferences("funcionarioPrefs", MODE_PRIVATE)

            // Log para debug das preferências
            val tipoAluno = prefsAluno.getString("tipo", null)
            val tipoFuncionario = prefsFuncionario.getString("tipo", null)
            val uidAluno = prefsAluno.getString("uid", null)
            val uidFuncionario = prefsFuncionario.getString("uid", null)
            val alunoDocId = prefsAluno.getString("alunoDocId", null)
            val funcionarioDocId = prefsFuncionario.getString("funcionarioDocId", null)

            Log.d("AutoLogin", "=== DEBUG DAS PREFERÊNCIAS ===")
            Log.d("AutoLogin", "tipoAluno: $tipoAluno")
            Log.d("AutoLogin", "tipoFuncionario: $tipoFuncionario")
            Log.d("AutoLogin", "uidAluno: $uidAluno")
            Log.d("AutoLogin", "uidFuncionario: $uidFuncionario")
            Log.d("AutoLogin", "alunoDocId: $alunoDocId")
            Log.d("AutoLogin", "funcionarioDocId: $funcionarioDocId")

            // Verifica se é aluno
            if (tipoAluno == "aluno" && !uidAluno.isNullOrEmpty() && !alunoDocId.isNullOrEmpty()) {
                Log.d("AutoLogin", "Redirecionando para TelaPrincipalAluno")
                val intent = Intent(this, TelaPrincipalAluno::class.java)
                startActivity(intent)
                finish()
                return
            }

            // Verifica se é funcionário
            if (tipoFuncionario == "funcionario" && !uidFuncionario.isNullOrEmpty() && !funcionarioDocId.isNullOrEmpty()) {
                Log.d("AutoLogin", "Redirecionando para TelaFuncionario")
                val intent = Intent(this, TelaFuncionario::class.java)
                startActivity(intent)
                finish()
                return
            }

            // VERIFICAÇÃO ADICIONAL: Talvez os dados estejam salvos na preferência "errada"
            // Verifica se tem dados de funcionário em alunoPrefs (possível erro de salvamento)
            val tipoEmAlunoPrefs = prefsAluno.getString("tipo", null)
            val funcionarioDocIdEmAlunoPrefs = prefsAluno.getString("funcionarioDocId", null)

            if (tipoEmAlunoPrefs == "funcionario" && !funcionarioDocIdEmAlunoPrefs.isNullOrEmpty()) {
                Log.d("AutoLogin", "Encontrados dados de funcionário em alunoPrefs - Redirecionando")
                val intent = Intent(this, TelaFuncionario::class.java)
                startActivity(intent)
                finish()
                return
            }

            // Se chegou até aqui, há usuário logado mas dados incompletos/corrompidos
            Log.d("AutoLogin", "Usuário logado mas dados incompletos - fazendo logout")
            auth.signOut()
            limparPreferencias()
        } else {
            Log.d("AutoLogin", "Nenhum usuário logado")
        }
        // Se não há usuário logado, permanece na tela inicial normalmente
    }

    private fun limparPreferencias() {
        Log.d("AutoLogin", "Limpando todas as preferências")
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