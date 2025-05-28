package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class TelaDetalhesUsuario_Funcionario : AppCompatActivity() {

    // Views
    private var btnSetaVoltar: ImageButton? = null
    private var nomeUsuarioTv: TextView? = null
    private var sobrenomeTv: TextView? = null
    private var idadeTv: TextView? = null
    private var generoTv: TextView? = null
    private var alturaEt: EditText? = null
    private var pesoEt: EditText? = null
    private var contusaoEt: EditText? = null
    private var bottomNav: BottomNavigationView? = null

    // Firestore
    private lateinit var db: FirebaseFirestore

    // ID do aluno que será passado via Intent
    private var alunoDocId: String? = null

    // Flags para controlar salvamento automático
    private var dadosCarregados = false
    private var alteracoesPendentes = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_tela_detalhes_usuario_funcionario)

            // Inicializar Firestore
            db = FirebaseFirestore.getInstance()

            // 1) Inicializar views
            if (!initViews()) {
                Log.e("DETALHES_USUARIO", "Erro ao inicializar views")
                Toast.makeText(this, "Erro ao carregar a tela", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // 2) Recuperar o ID do aluno
            alunoDocId = intent.getStringExtra("alunoDocId")
            Log.d("DETALHES_USUARIO", "ID do aluno recebido: $alunoDocId")

            // 3) Configurar eventos
            configurarEventos()

            // 4) Configurar salvamento automático nos EditTexts
            configurarSalvamentoAutomatico()

            // 5) Buscar dados do aluno
            alunoDocId?.let { docId ->
                if (docId.isNotEmpty()) {
                    buscarDadosAluno(docId)
                } else {
                    Toast.makeText(this, "ID do aluno está vazio.", Toast.LENGTH_LONG).show()
                    Log.e("DETALHES_USUARIO", "ID do aluno está vazio")
                    finish()
                }
            } ?: run {
                Toast.makeText(this, "ID do aluno não foi fornecido.", Toast.LENGTH_LONG).show()
                Log.e("DETALHES_USUARIO", "ID do aluno não fornecido")
                finish()
            }

        } catch (e: Exception) {
            Log.e("DETALHES_USUARIO", "Erro no onCreate", e)
            Toast.makeText(this, "Erro ao inicializar a tela: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initViews(): Boolean {
        return try {
            nomeUsuarioTv = findViewById(R.id.primeiro_nome_usuario)
            sobrenomeTv = findViewById(R.id.sobrenome_usuario)
            idadeTv = findViewById(R.id.campo_idade_usuario)
            generoTv = findViewById(R.id.genero)
            alturaEt = findViewById(R.id.altura_do_usuario)
            pesoEt = findViewById(R.id.peso_do_aluno)
            contusaoEt = findViewById(R.id.contusao_do_usuario)
            btnSetaVoltar = findViewById(R.id.SetaVoltarTelaGerenciamentoAluno)
            bottomNav = findViewById(R.id.bottom_navigation)

            // Verificar se todas as views foram encontradas
            val viewsNaoEncontradas = mutableListOf<String>()

            if (nomeUsuarioTv == null) viewsNaoEncontradas.add("primeiro_nome_usuario")
            if (sobrenomeTv == null) viewsNaoEncontradas.add("sobrenome_usuario")
            if (idadeTv == null) viewsNaoEncontradas.add("campo_idade_usuario")
            if (generoTv == null) viewsNaoEncontradas.add("genero")
            if (alturaEt == null) viewsNaoEncontradas.add("altura_do_usuario")
            if (pesoEt == null) viewsNaoEncontradas.add("peso_do_aluno")
            if (contusaoEt == null) viewsNaoEncontradas.add("contusao_do_usuario")
            if (btnSetaVoltar == null) viewsNaoEncontradas.add("SetaVoltarTelaGerenciamentoAluno")
            if (bottomNav == null) viewsNaoEncontradas.add("bottom_navigation")

            if (viewsNaoEncontradas.isNotEmpty()) {
                Log.e("DETALHES_USUARIO", "Views não encontradas: ${viewsNaoEncontradas.joinToString()}")
                return false
            }

            true
        } catch (e: Exception) {
            Log.e("DETALHES_USUARIO", "Erro ao inicializar views", e)
            false
        }
    }

    private fun configurarEventos() {
        try {
            // Botão voltar
            btnSetaVoltar?.setOnClickListener {
                salvarDadosSeNecessario()
                val intent = Intent(this, GerenciamentoDoAluno_Funcionario::class.java)
                startActivity(intent)
                finish()
            }

            // Bottom navigation
            bottomNav?.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_inicio -> {
                        salvarDadosSeNecessario()
                        val intent = Intent(this, TelaFuncionario::class.java)
                        startActivity(intent)
                        finish()
                        true
                    }
                    R.id.nav_chat -> {
                        salvarDadosSeNecessario()
                        val intent = Intent(this, TelaChat::class.java)
                        startActivity(intent)
                        finish()
                        true
                    }
                    R.id.nav_config -> {
                        salvarDadosSeNecessario()
                        val intent = Intent(this, TelaConfiguracao_Funcionario::class.java)
                        startActivity(intent)
                        finish()
                        true
                    }
                    else -> false
                }
            }
        } catch (e: Exception) {
            Log.e("DETALHES_USUARIO", "Erro ao configurar eventos", e)
        }
    }

    private fun configurarSalvamentoAutomatico() {
        try {
            val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    // Só marcar como alterado se os dados já foram carregados
                    if (dadosCarregados) {
                        alteracoesPendentes = true
                        Log.d("DETALHES_USUARIO", "Alteração detectada nos campos editáveis")
                    }
                }
            }

            // Adicionar o TextWatcher apenas nos campos editáveis
            alturaEt?.addTextChangedListener(textWatcher)
            pesoEt?.addTextChangedListener(textWatcher)
            contusaoEt?.addTextChangedListener(textWatcher)
        } catch (e: Exception) {
            Log.e("DETALHES_USUARIO", "Erro ao configurar salvamento automático", e)
        }
    }

    private fun buscarDadosAluno(docId: String) {
        Log.d("DETALHES_USUARIO", "Buscando dados do aluno: $docId")

        try {
            db.collection("alunos").document(docId)
                .get()
                .addOnSuccessListener { document ->
                    try {
                        if (document != null && document.exists()) {
                            Log.d("DETALHES_USUARIO", "Documento encontrado. Dados: ${document.data}")

                            // Dados básicos (não editáveis) - usando get() para debug
                            val nome = document.get("nome")?.toString() ?: ""
                            val sobrenome = document.get("sobrenome")?.toString() ?: ""
                            val idade = document.get("idade")?.toString() ?: ""
                            val genero = document.get("genero")?.toString() ?: ""

                            // Dados editáveis
                            val altura = document.get("altura")?.toString() ?: ""
                            val peso = document.get("peso")?.toString() ?: ""
                            val contusao = document.get("contusao")?.toString() ?: ""

                            Log.d("DETALHES_USUARIO", "Dados extraídos: nome=$nome, sobrenome=$sobrenome, idade=$idade, genero=$genero")

                            // Atualizar interface na UI thread
                            runOnUiThread {
                                try {
                                    nomeUsuarioTv?.text = nome
                                    sobrenomeTv?.text = sobrenome
                                    idadeTv?.text = idade
                                    generoTv?.text = genero

                                    alturaEt?.setText(altura)
                                    pesoEt?.setText(peso)
                                    contusaoEt?.setText(contusao)

                                    dadosCarregados = true
                                    alteracoesPendentes = false

                                    Log.d("DETALHES_USUARIO", "Dados carregados com sucesso para: $nome $sobrenome")
                                    Toast.makeText(this@TelaDetalhesUsuario_Funcionario, "Dados carregados com sucesso", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Log.e("DETALHES_USUARIO", "Erro ao atualizar UI com dados do aluno", e)
                                    Toast.makeText(this@TelaDetalhesUsuario_Funcionario, "Erro ao atualizar interface", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Log.w("DETALHES_USUARIO", "Documento não encontrado ou é nulo: $docId")
                            runOnUiThread {
                                Toast.makeText(this@TelaDetalhesUsuario_Funcionario, "Aluno não encontrado no banco de dados.", Toast.LENGTH_LONG).show()
                                finish()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DETALHES_USUARIO", "Erro ao processar dados do Firestore", e)
                        runOnUiThread {
                            Toast.makeText(this@TelaDetalhesUsuario_Funcionario, "Erro ao processar dados: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("DETALHES_USUARIO", "Erro ao buscar dados do aluno", exception)
                    runOnUiThread {
                        when (exception) {
                            is com.google.firebase.firestore.FirebaseFirestoreException -> {
                                Toast.makeText(this@TelaDetalhesUsuario_Funcionario, "Erro de conexão com o banco de dados", Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(this@TelaDetalhesUsuario_Funcionario, "Erro ao carregar dados: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("DETALHES_USUARIO", "Erro ao iniciar busca no Firestore", e)
            Toast.makeText(this, "Erro ao conectar com o banco de dados: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun salvarDadosSeNecessario() {
        if (!alteracoesPendentes || alunoDocId == null) {
            Log.d("DETALHES_USUARIO", "Nenhuma alteração para salvar")
            return
        }

        salvarDados()
    }

    private fun salvarDados() {
        val docId = alunoDocId ?: return

        try {
            // Obter valores dos campos editáveis
            val altura = alturaEt?.text?.toString()?.trim() ?: ""
            val peso = pesoEt?.text?.toString()?.trim() ?: ""
            val contusao = contusaoEt?.text?.toString()?.trim() ?: ""

            // Preparar dados para atualização
            val dadosParaAtualizar = hashMapOf<String, Any>(
                "altura" to altura,
                "peso" to peso,
                "contusao" to contusao
            )

            Log.d("DETALHES_USUARIO", "Salvando dados: altura=$altura, peso=$peso, contusao=$contusao")

            // Atualizar no Firestore
            db.collection("alunos").document(docId)
                .update(dadosParaAtualizar)
                .addOnSuccessListener {
                    alteracoesPendentes = false
                    Log.d("DETALHES_USUARIO", "Dados salvos com sucesso no Firestore")

                    // Toast para feedback
                    runOnUiThread {
                        Toast.makeText(this@TelaDetalhesUsuario_Funcionario, "Alterações salvas com sucesso", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("DETALHES_USUARIO", "Erro ao salvar dados", exception)
                    runOnUiThread {
                        Toast.makeText(this@TelaDetalhesUsuario_Funcionario, "Erro ao salvar alterações: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
        } catch (e: Exception) {
            Log.e("DETALHES_USUARIO", "Erro ao preparar dados para salvamento", e)
            Toast.makeText(this, "Erro ao salvar dados: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Lifecycle methods para garantir que os dados sejam salvos
    override fun onPause() {
        super.onPause()
        try {
            salvarDadosSeNecessario()
            Log.d("DETALHES_USUARIO", "onPause - dados salvos se necessário")
        } catch (e: Exception) {
            Log.e("DETALHES_USUARIO", "Erro no onPause", e)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            salvarDadosSeNecessario()
            Log.d("DETALHES_USUARIO", "onStop - dados salvos se necessário")
        } catch (e: Exception) {
            Log.e("DETALHES_USUARIO", "Erro no onStop", e)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        try {
            salvarDadosSeNecessario()
            super.onBackPressed()
        } catch (e: Exception) {
            Log.e("DETALHES_USUARIO", "Erro no onBackPressed", e)
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("DETALHES_USUARIO", "onDestroy chamado")
    }

    // Método público para forçar salvamento (se necessário)
    fun forcarSalvamento() {
        try {
            if (dadosCarregados) {
                alteracoesPendentes = true
                salvarDados()
            }
        } catch (e: Exception) {
            Log.e("DETALHES_USUARIO", "Erro ao forçar salvamento", e)
        }
    }
}