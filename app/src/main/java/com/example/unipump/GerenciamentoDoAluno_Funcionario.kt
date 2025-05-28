package com.example.unipump

import FichaTreinoAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.models.FichaTreino
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class GerenciamentoDoAluno_Funcionario : AppCompatActivity(),
    FichaTreinoAdapter.OnFichaTreinoClickListener {

    private lateinit var linkAdicionar: TextView
    private lateinit var linkMaisDetalhes: TextView
    private lateinit var btnSetaVoltar: ImageButton
    private lateinit var btnNavegacao: BottomNavigationView
    private lateinit var titulo: TextView
    private lateinit var tvNome: TextView
    private lateinit var tvSobrenome: TextView

    // RecyclerView components
    private lateinit var rvFichasTreino: RecyclerView
    private lateinit var adapter: FichaTreinoAdapter
    private val fichasTreino = mutableListOf<FichaTreino>()

    // Firestore
    private lateinit var db: FirebaseFirestore
    private var alunoDocId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_gerenciamento_do_aluno_antigo_funcionario)

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance()

        // Recuperar o ID do aluno
        recuperarIdAluno()

        // Inicializar views
        initViews()

        // Configurar dados do usuário
        configurarDadosUsuario()

        // Configurar RecyclerView
        setupRecyclerView()

        // Configurar eventos
        configurarEventos()

        // Carregar dados das fichas do Firestore
        carregarFichasDoFirestore()
    }

    override fun onResume() {
        super.onResume()
        Log.d("LIFECYCLE_GERENCIAMENTO", "onResume - Recarregando fichas do Firestore")
        // SEMPRE recarregar as fichas quando a tela volta ao foco
        carregarFichasDoFirestore()
    }

    private fun recuperarIdAluno() {
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        alunoDocId = prefs.getString("alunoDocId", "") ?: ""

        if (alunoDocId.isEmpty()) {
            Toast.makeText(this, "Erro: ID do aluno não encontrado", Toast.LENGTH_SHORT).show()
            Log.e("GERENCIAMENTO_ALUNO", "ID do aluno não encontrado no SharedPreferences")
            finish()
        } else {
            Log.d("GERENCIAMENTO_ALUNO", "ID do aluno recuperado: $alunoDocId")
        }
    }

    private fun initViews() {
        linkMaisDetalhes = findViewById(R.id.link_mais_detalhes)
        linkAdicionar = findViewById(R.id.link_adicionar)
        btnSetaVoltar = findViewById(R.id.SetaVoltarTelaGerenciamentoAluno)
        btnNavegacao = findViewById(R.id.bottom_navigation)
        rvFichasTreino = findViewById(R.id.rvFichasTreino)
        titulo = findViewById(R.id.titulo)
        tvNome = findViewById(R.id.tvNome)
        tvSobrenome = findViewById(R.id.tvSobrenome)
    }

    private fun configurarDadosUsuario() {
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val nome = prefs.getString("nome", "")
        val sobrenome = prefs.getString("sobrenome", "")
        titulo.text = "Sobre $nome"
        tvNome.text = "$nome"
        tvSobrenome.text = "$sobrenome"
    }

    private fun setupRecyclerView() {
        adapter = FichaTreinoAdapter(fichasTreino, this)

        rvFichasTreino.apply {
            layoutManager = LinearLayoutManager(this@GerenciamentoDoAluno_Funcionario)
            adapter = this@GerenciamentoDoAluno_Funcionario.adapter
            setHasFixedSize(true)
        }
    }

    // MÉTODO CORRIGIDO: Agora captura o documentId do Firestore
    private fun carregarFichasDoFirestore() {
        Log.d("FIRESTORE_LOAD", "=== INICIANDO CARREGAMENTO DAS FICHAS ===")
        Log.d("FIRESTORE_LOAD", "Aluno ID: $alunoDocId")

        if (alunoDocId.isEmpty()) {
            Toast.makeText(this, "ID do aluno não disponível", Toast.LENGTH_SHORT).show()
            return
        }

        val tamanhoAnterior = fichasTreino.size
        fichasTreino.clear()
        Log.d("FIRESTORE_LOAD", "Lista limpa. Tamanho anterior: $tamanhoAnterior")
        adapter.notifyDataSetChanged()

        db.collection("alunos")
            .document(alunoDocId)
            .collection("treino")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("FIRESTORE_LOAD", "Documentos de treino encontrados: ${documents.size()}")

                try {
                    if (documents.isEmpty) {
                        Log.d("FIRESTORE_LOAD", "Nenhuma ficha de treino encontrada para este aluno")
                        runOnUiThread {
                            adapter.notifyDataSetChanged()
                            Toast.makeText(
                                this@GerenciamentoDoAluno_Funcionario,
                                "Nenhuma ficha de treino encontrada",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@addOnSuccessListener
                    }

                    val novasFichas = mutableListOf<FichaTreino>()

                    for (document in documents) {
                        try {
                            // CRÍTICO: Capturar o ID único do documento gerado pelo Firestore
                            val documentId = document.id
                            val letra = document.getString("letra") ?: ""
                            val nome = document.getString("nome") ?: "Treino sem nome"
                            val descricao = document.getString("descricao") ?: ""

                            val quantidadeExercicios = when (val quantidade = document.get("quantidadeExercicios")) {
                                is Long -> quantidade.toInt()
                                is Double -> quantidade.toInt()
                                is String -> quantidade.toIntOrNull() ?: 0
                                is Int -> quantidade
                                else -> {
                                    Log.w("FIRESTORE_LOAD", "Tipo não reconhecido para quantidadeExercicios: ${quantidade?.javaClass}")
                                    0
                                }
                            }

                            // CORREÇÃO: Criar FichaTreino incluindo o documentId
                            val fichaTreino = FichaTreino(
                                documentId = documentId, // NOVO: ID único do documento
                                letra = letra,
                                nome = nome,
                                descricao = descricao,
                                quantidadeExercicios = quantidadeExercicios
                            )

                            novasFichas.add(fichaTreino)

                            Log.d("FIRESTORE_LOAD",
                                "Ficha processada: DocID=$documentId, Letra=$letra, Nome=$nome, Exercícios=$quantidadeExercicios")

                        } catch (e: Exception) {
                            Log.e("FIRESTORE_LOAD", "Erro ao processar documento ${document.id}", e)
                        }
                    }

                    novasFichas.sortBy { it.letra }
                    Log.d("FIRESTORE_LOAD", "Total de fichas processadas: ${novasFichas.size}")

                    runOnUiThread {
                        fichasTreino.clear()
                        fichasTreino.addAll(novasFichas)
                        adapter.notifyDataSetChanged()

                        Log.d("FIRESTORE_LOAD", "RecyclerView atualizado com ${fichasTreino.size} fichas")

                        fichasTreino.forEachIndexed { index, ficha ->
                            Log.d("FIRESTORE_LOAD", "Ficha $index: DocID=${ficha.documentId}, ${ficha.letra} - ${ficha.nome}")
                        }

                        if (fichasTreino.isNotEmpty()) {
                            Toast.makeText(
                                this@GerenciamentoDoAluno_Funcionario,
                                "${fichasTreino.size} fichas carregadas",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                } catch (e: Exception) {
                    Log.e("FIRESTORE_LOAD", "Erro geral ao processar fichas", e)
                    runOnUiThread {
                        Toast.makeText(
                            this@GerenciamentoDoAluno_Funcionario,
                            "Erro ao processar fichas: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                Log.d("FIRESTORE_LOAD", "=== CARREGAMENTO CONCLUÍDO ===")
            }
            .addOnFailureListener { exception ->
                Log.e("FIRESTORE_LOAD", "Erro ao carregar fichas de treino", exception)
                runOnUiThread {
                    Toast.makeText(
                        this@GerenciamentoDoAluno_Funcionario,
                        "Erro ao carregar fichas: ${exception.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun adicionarNovaFicha() {
        if (alunoDocId.isEmpty()) {
            Toast.makeText(this, "Erro: ID do aluno não encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("ADICIONAR_FICHA", "Iniciando adição de nova ficha para aluno: $alunoDocId")

        val proximaLetra = determinarProximaLetra()

        val novaFicha = hashMapOf(
            "letra" to proximaLetra,
            "nome" to "Nova Ficha $proximaLetra",
            "descricao" to "Descrição da ficha $proximaLetra",
            "quantidadeExercicios" to 0
        )

        Log.d("ADICIONAR_FICHA", "Criando ficha: $novaFicha")

        db.collection("alunos")
            .document(alunoDocId)
            .collection("treino")
            .add(novaFicha)
            .addOnSuccessListener { documentReference ->
                Log.d("ADICIONAR_FICHA", "Nova ficha adicionada com ID: ${documentReference.id}")
                Toast.makeText(this, "Nova ficha '$proximaLetra' adicionada com sucesso!", Toast.LENGTH_SHORT).show()

                // Recarregar as fichas para mostrar a nova
                carregarFichasDoFirestore()
            }
            .addOnFailureListener { e ->
                Log.e("ADICIONAR_FICHA", "Erro ao adicionar nova ficha", e)
                Toast.makeText(this, "Erro ao adicionar ficha: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun determinarProximaLetra(): String {
        val letrasUsadas = fichasTreino.map { it.letra.uppercase() }.toSet()
        val alfabeto = ('A'..'Z').map { it.toString() }

        Log.d("ADICIONAR_FICHA", "Letras já usadas: $letrasUsadas")

        for (letra in alfabeto) {
            if (letra !in letrasUsadas) {
                Log.d("ADICIONAR_FICHA", "Próxima letra disponível: $letra")
                return letra
            }
        }

        val proximaLetra = "A${fichasTreino.size + 1}"
        Log.d("ADICIONAR_FICHA", "Todas as letras ocupadas, usando: $proximaLetra")
        return proximaLetra
    }

    private fun configurarEventos() {
        linkMaisDetalhes.setOnClickListener {
            val intent = Intent(this, TelaDetalhesUsuario_Funcionario::class.java)
            if (alunoDocId.isNotEmpty()) {
                intent.putExtra("alunoDocId", alunoDocId)
                Log.d("GERENCIAMENTO_ALUNO", "Enviando ID do aluno: $alunoDocId para tela de detalhes")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Erro: ID do aluno não encontrado", Toast.LENGTH_SHORT).show()
            }
        }

        linkAdicionar.setOnClickListener {
            Log.d("ADICIONAR_FICHA", "Botão adicionar clicado para aluno: $alunoDocId")
            adicionarNovaFicha()
        }

        btnSetaVoltar.setOnClickListener {
            val intent = Intent(this, TelaFuncionario::class.java)
            startActivity(intent)
        }

        btnNavegacao.setOnNavigationItemSelectedListener { item ->
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
                    val intent = Intent(this, TelaConfiguracao_Funcionario::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    // MÉTODO CORRIGIDO: Agora passa o documentId para a tela de edição
    override fun onFichaTreinoClick(fichaTreino: FichaTreino, position: Int) {
        Log.d("FICHA_CLICK", "=== FICHA SELECIONADA ===")
        Log.d("FICHA_CLICK", "Ficha: ${fichaTreino.letra} - ${fichaTreino.nome}")
        Log.d("FICHA_CLICK", "Document ID: ${fichaTreino.documentId}") // NOVO LOG
        Log.d("FICHA_CLICK", "Aluno ID: $alunoDocId")

        val intent = Intent(this, TelaEdicaoFichaTreino_funcionario::class.java)

        // Passar o ID do aluno
        intent.putExtra("alunoDocId", alunoDocId)

        // CRÍTICO: Passar o ID único do documento para permitir atualizações precisas
        intent.putExtra("documentId", fichaTreino.documentId)

        // Passar todas as informações da ficha selecionada
        intent.putExtra("ficha_letra", fichaTreino.letra)
        intent.putExtra("ficha_nome", fichaTreino.nome)
        intent.putExtra("ficha_descricao", fichaTreino.descricao)
        intent.putExtra("ficha_quantidade_exercicios", fichaTreino.quantidadeExercicios)

        startActivity(intent)

        Log.d("FICHA_CLICK", "Navegando para TelaEdicaoFichaTreino_funcionario com DocumentID: ${fichaTreino.documentId}")
    }

    fun forcarRecarregamento() {
        Log.d("GERENCIAMENTO_ALUNO", "Recarregamento forçado solicitado")
        carregarFichasDoFirestore()
    }
}