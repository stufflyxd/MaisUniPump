package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class TelaInformacaoPessoal_funcionario : BaseActivity() {

    // Views
    private lateinit var nomeUsuarioEt: EditText
    private lateinit var enderecoEt: EditText
    private lateinit var generoEt: EditText
    private lateinit var telefoneEt: EditText
    private lateinit var primeiroNomeTv: TextView
    private lateinit var sobrenomeTv: TextView
    private lateinit var idadeTv: TextView
    private lateinit var btnVoltar: ImageButton
    private lateinit var bottomNav: BottomNavigationView

    // Firestore
    private val db by lazy {
        FirebaseFirestore.getInstance()
    }

    // SharedPreferences onde guardamos o funcionarioDocId no login
    private val prefs by lazy {
        getSharedPreferences("funcionarioPrefs", MODE_PRIVATE)
    }

    // CORREÇÃO: Usar a chave correta salva no login
    private val funcionarioDocId: String?
        get() = prefs.getString("funcionarioDocId", null) // Esta é a chave que você usa no login

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_informacao_pessoal_funcionario)

        try {
            // Bind das views usando os IDs corretos do layout
            nomeUsuarioEt = findViewById(R.id.campo_nome_usuario)
            enderecoEt = findViewById(R.id.campo_endereco_usuario)
            generoEt = findViewById(R.id.genero)
            telefoneEt = findViewById(R.id.numero_contato)
            primeiroNomeTv = findViewById(R.id.primeiro_nome_usuario)
            sobrenomeTv = findViewById(R.id.sobrenome_usuario)
            idadeTv = findViewById(R.id.campo_idade_usuario)
            btnVoltar = findViewById(R.id.SetaVoltarTelaGerenciamentoAluno)
            bottomNav = findViewById(R.id.bottom_navigation)



            // Verificar se todos os componentes foram encontrados
            if (nomeUsuarioEt == null || btnVoltar == null) {
                Log.e("TelaInfoFuncionario", "Erro: Componentes não encontrados no layout")
                Toast.makeText(this, "Erro ao carregar tela", Toast.LENGTH_LONG).show()
                finish()
                return

            }

            // Configurar botão voltar
            btnVoltar.setOnClickListener {
                Log.d("TelaInfoFuncionario", "Botão voltar clicado")
                salvarDados()
                finish()
            }

            // Bottom navigation
            bottomNav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_inicio -> {
                        startActivity(Intent(this, TelaFuncionario::class.java))
                        salvarDados()
                        true
                    }
                    R.id.nav_chat -> {
                        startActivity(Intent(this, TelaChat::class.java))
                        salvarDados()
                        true
                    }
                    R.id.nav_config -> {
                        startActivity(Intent(this, TelaConfiguracao_Funcionario::class.java))
                        salvarDados()
                        true
                    }
                    else -> false
                }
            }

            // Buscar dados do funcionário
            funcionarioDocId?.let { docId ->
                Log.d("TelaInfoFuncionario", "Buscando dados com docId: $docId")
                buscarDadosFuncionario(docId)
            } ?: run {
                Log.e("TelaInfoFuncionario", "funcionarioDocId não encontrado nos SharedPreferences")

                // Debug: Mostrar todas as chaves salvas
                val allPrefs = prefs.all
                Log.d("TelaInfoFuncionario", "Chaves disponíveis: ${allPrefs.keys}")

                Toast.makeText(
                    this,
                    "Não encontrei seu perfil. Faça login novamente.",
                    Toast.LENGTH_LONG
                ).show()

                // Ir para TelaInicial em vez de TelaLogin
                startActivity(Intent(this, TelaInicial::class.java))
                finish()
            }

        } catch (e: Exception) {
            Log.e("TelaInfoFuncionario", "Erro no onCreate", e)
            Toast.makeText(this, "Erro ao inicializar tela: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("TelaInfoFuncionario", "onResume chamado")
        // Recarrega sempre que a Activity volta ao foco
        funcionarioDocId?.let { buscarDadosFuncionario(it) }
    }

    override fun onBackPressed() {
        // Garante que salve antes de sair
        salvarDados()
        super.onBackPressed()
    }

    private fun buscarDadosFuncionario(docId: String) {
        Log.d("TelaInfoFuncionario", "Buscando dados em funcionarios/$docId")
        db.collection("funcionarios").document(docId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    Log.d("TelaInfoFuncionario", "Documento encontrado, preenchendo campos")
                    nomeUsuarioEt.setText(doc.getString("nome_usuario") ?: "")
                    enderecoEt.setText(doc.getString("endereco") ?: "")
                    generoEt.setText(doc.getString("genero") ?: "")
                    telefoneEt.setText(doc.getString("telefone") ?: "")
                    primeiroNomeTv.text = doc.getString("nome") ?: ""
                    sobrenomeTv.text = doc.getString("sobrenome") ?: ""
                    idadeTv.text = doc.getString("idade") ?: ""
                } else {
                    Log.w("TelaInfoFuncionario", "Documento não existe para docId: $docId")
                    Toast.makeText(this, "Seus dados não foram encontrados", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("TelaInfoFuncionario", "Erro ao buscar dados", e)
                Toast.makeText(this, "Falha ao carregar seus dados: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun salvarDados() {
        val docId = funcionarioDocId ?: return

        Log.d("TelaInfoFuncionario", "Salvando dados para docId: $docId")

        val mapa = mapOf(
            "nome_usuario" to nomeUsuarioEt.text.toString(),
            "endereco" to enderecoEt.text.toString(),
            "genero" to generoEt.text.toString(),
            "telefone" to telefoneEt.text.toString()
        )

        // Atualizar SharedPreferences local
        val edit = prefs.edit()
        edit.putString("nome_usuario", nomeUsuarioEt.text.toString())
        edit.putString("endereco", enderecoEt.text.toString())
        edit.putString("genero", generoEt.text.toString())
        edit.putString("telefone", telefoneEt.text.toString())
        edit.apply()

        // CORREÇÃO: Salvar na coleção correta "funcionarios"
        db.collection("funcionarios").document(docId)
            .update(mapa)
            .addOnSuccessListener {
                Log.d("TelaInfoFuncionario", "Dados atualizados com sucesso")
            }
            .addOnFailureListener { e ->
                Log.e("TelaInfoFuncionario", "Erro ao salvar dados", e)
                Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStop() {
        super.onStop()
        salvarDados()
        Log.d("TelaInfoFuncionario", "onStop chamado")
    }
}