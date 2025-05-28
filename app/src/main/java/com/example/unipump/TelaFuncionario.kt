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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.models.Aluno
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class TelaFuncionario : AppCompatActivity() {

    private lateinit var btnNavegacao: BottomNavigationView
    private lateinit var btnNotificacao: ImageButton
    private lateinit var nomeUser: TextView
    private lateinit var rvListaAlunos: RecyclerView
    private lateinit var editTextSearch: EditText
    private lateinit var alunoAdapter: AlunoAdapter

    // Lista de alunos
    private val listaAlunos = mutableListOf<Aluno>()
    private val listaAlunosFiltrada = mutableListOf<Aluno>()

    // Flag para controlar se os dados já foram carregados
    private var dadosCarregados = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_funcionario)

        inicializarViews()
        configurarRecyclerView()
        configurarDadosUsuario()
        configurarBusca()
        configurarEventos()
    }

    private fun inicializarViews() {
        btnNavegacao = findViewById(R.id.bottom_navigation)
        btnNotificacao = findViewById(R.id.btn_notificacao)
        nomeUser = findViewById(R.id.nomeUser)
        rvListaAlunos = findViewById(R.id.rvListaAlunos)
        editTextSearch = findViewById(R.id.search_edit_text)
    }

    private fun configurarRecyclerView() {
        // Configurar adapter primeiro (lista vazia inicialmente)
        alunoAdapter = AlunoAdapter(listaAlunosFiltrada) { aluno ->
            handleAlunoClick(aluno)
        }

        // Configurar RecyclerView com GridLayoutManager (4 colunas)
        rvListaAlunos.layoutManager = GridLayoutManager(this, 4)
        rvListaAlunos.adapter = alunoAdapter

        // Carregar alunos do Firestore apenas na primeira vez
        if (!dadosCarregados) {
            carregarAlunosFromFirestore()
        }
    }

    private fun carregarAlunosFromFirestore() {
        // Limpar AMBAS as listas existentes
        listaAlunos.clear()
        listaAlunosFiltrada.clear()

        // Inicializar Firestore
        val db = FirebaseFirestore.getInstance()

        // Buscar todos os alunos na coleção "alunos"
        db.collection("alunos")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("FIRESTORE", "=== INÍCIO DO CARREGAMENTO ===")
                Log.d("FIRESTORE", "Documentos encontrados: ${documents.size()}")

                // Log de todos os IDs dos documentos primeiro
                Log.d("FIRESTORE", "IDs dos documentos:")
                documents.forEach { doc ->
                    Log.d("FIRESTORE", "- ${doc.id}")
                }

                var contadorProcessados = 0
                var contadorAdicionados = 0

                for (document in documents) {
                    contadorProcessados++
                    Log.d("FIRESTORE", "--- Processando documento $contadorProcessados/${documents.size()} ---")
                    Log.d("FIRESTORE", "ID do documento: ${document.id}")

                    try {
                        // Log dos dados raw do documento
                        Log.d("FIRESTORE", "Dados do documento: ${document.data}")

                        // Extrair dados do documento conforme sua estrutura
                        val documentId = document.id
                        val nome = document.getString("nome") ?: "Nome"
                        val sobrenome = document.getString("sobrenome") ?: ""
                        val nomeUsuario = document.getString("nome_usuario") ?: ""
                        val email = document.getString("email") ?: ""
                        val telefone = document.getString("telefone") ?: ""
                        val idade = document.getString("idade") ?: ""
                        val genero = document.getString("genero") ?: ""
                        val endereco = document.getString("endereco") ?: ""
                        val contusao = document.getString("contusao") ?: ""

                        // Tratamento flexível para altura e peso (pode ser String ou Double)
                        val altura = try {
                            // Primeiro tenta como Double
                            document.getDouble("altura") ?: 0.0
                        } catch (e: Exception) {
                            // Se falhar, tenta como String e converte
                            try {
                                val alturaString = document.getString("altura")
                                if (alturaString.isNullOrBlank()) {
                                    0.0
                                } else {
                                    // Remove vírgulas e substitui por pontos, remove espaços
                                    alturaString.replace(",", ".").trim().toDoubleOrNull() ?: 0.0
                                }
                            } catch (e2: Exception) {
                                Log.w("FIRESTORE", "Erro ao processar altura do documento ${document.id}: ${e2.message}")
                                0.0
                            }
                        }

                        val peso = try {
                            // Primeiro tenta como Double
                            document.getDouble("peso") ?: 0.0
                        } catch (e: Exception) {
                            // Se falhar, tenta como String e converte
                            try {
                                val pesoString = document.getString("peso")
                                if (pesoString.isNullOrBlank()) {
                                    0.0
                                } else {
                                    // Remove vírgulas e substitui por pontos, remove espaços
                                    pesoString.replace(",", ".").trim().toDoubleOrNull() ?: 0.0
                                }
                            } catch (e2: Exception) {
                                Log.w("FIRESTORE", "Erro ao processar peso do documento ${document.id}: ${e2.message}")
                                0.0
                            }
                        }

                        Log.d("FIRESTORE", "Dados extraídos - Nome: '$nome', Email: '$email', Altura: $altura, Peso: $peso")

                        // Decidir qual nome mostrar (prioridade: nome_usuario, depois nome + sobrenome)
                        val nomeParaExibir = when {
                            sobrenome.isNotBlank() -> "$nome"
                            nomeUsuario.isNotBlank() -> nomeUsuario
                            else -> nome
                        }

                        Log.d("FIRESTORE", "Nome para exibir: '$nomeParaExibir'")

                        // Criar objeto Aluno
                        val aluno = Aluno(
                            id = documentId,
                            nome = nomeParaExibir,
                            sobrenome = sobrenome,
                            temDados = true, // Como está no banco, já tem dados
                            documentId = documentId, // Guardar o ID original do documento
                            email = email,
                            telefone = telefone,
                            idade = idade,
                            genero = genero,
                            endereco = endereco,
                            altura = altura,
                            peso = peso,
                            contusao = contusao
                        )

                        // Verificar se o aluno já existe na lista (evitar duplicatas)
                        val jaExiste = listaAlunos.any { it.documentId == documentId }
                        if (jaExiste) {
                            Log.w("FIRESTORE", "Aluno com ID $documentId já existe na lista!")
                        } else {
                            listaAlunos.add(aluno)
                            listaAlunosFiltrada.add(aluno)
                            contadorAdicionados++
                            Log.d("FIRESTORE", "✓ Aluno adicionado: ${aluno.nome} (${aluno.email}) - Total na lista: ${listaAlunos.size}")
                        }

                    } catch (e: Exception) {
                        Log.e("FIRESTORE", "❌ Erro ao processar documento ${document.id}", e)
                        Log.e("FIRESTORE", "Dados do documento problemático: ${document.data}")

                        // Mesmo com erro, vamos tentar criar um aluno básico para não perder o registro
                        try {
                            val documentId = document.id
                            val nome = document.getString("nome") ?: "Nome Desconhecido"
                            val email = document.getString("email") ?: ""

                            val alunoBasico = Aluno(
                                id = documentId,
                                nome = nome,
                                sobrenome = "",
                                temDados = false, // Marcar como sem dados completos
                                documentId = documentId,
                                email = email,
                                telefone = "",
                                idade = "",
                                genero = "",
                                endereco = "",
                                altura = 0.0,
                                peso = 0.0,
                                contusao = ""
                            )

                            listaAlunos.add(alunoBasico)
                            listaAlunosFiltrada.add(alunoBasico)
                            contadorAdicionados++
                            Log.d("FIRESTORE", "⚠️ Aluno básico criado para documento com erro: ${alunoBasico.nome}")

                        } catch (e2: Exception) {
                            Log.e("FIRESTORE", "❌ Falha total ao processar documento ${document.id}", e2)
                        }
                    }
                }

                Log.d("FIRESTORE", "=== RESUMO FINAL ===")
                Log.d("FIRESTORE", "Documentos no Firestore: ${documents.size()}")
                Log.d("FIRESTORE", "Documentos processados: $contadorProcessados")
                Log.d("FIRESTORE", "Alunos adicionados: $contadorAdicionados")
                Log.d("FIRESTORE", "Total na listaAlunos: ${listaAlunos.size}")
                Log.d("FIRESTORE", "Total na listaAlunosFiltrada: ${listaAlunosFiltrada.size}")

                // Log da lista final
                Log.d("FIRESTORE", "Lista final de alunos:")
                listaAlunos.forEachIndexed { index, aluno ->
                    Log.d("FIRESTORE", "${index + 1}. ${aluno.nome} (ID: ${aluno.documentId})")
                }

                // Atualizar RecyclerView na thread principal
                runOnUiThread {
                    alunoAdapter.notifyDataSetChanged()
                    Log.d("FIRESTORE", "RecyclerView atualizado")
                    dadosCarregados = true // Marcar que os dados foram carregados

                    // Debug do adapter
                    verificarAdapter()
                }

                // Mostrar mensagem se não houver alunos
                if (listaAlunos.isEmpty()) {
                    Toast.makeText(this@TelaFuncionario, "Nenhum aluno encontrado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@TelaFuncionario, "Carregados ${listaAlunos.size} alunos", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FIRESTORE", "❌ Erro ao carregar alunos", exception)
                Toast.makeText(
                    this@TelaFuncionario,
                    "Erro ao carregar alunos: ${exception.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // Método adicional para debug do adapter
    private fun verificarAdapter() {
        Log.d("ADAPTER_DEBUG", "=== VERIFICAÇÃO DO ADAPTER ===")
        Log.d("ADAPTER_DEBUG", "listaAlunos.size: ${listaAlunos.size}")
        Log.d("ADAPTER_DEBUG", "listaAlunosFiltrada.size: ${listaAlunosFiltrada.size}")
        Log.d("ADAPTER_DEBUG", "adapter.itemCount: ${alunoAdapter.itemCount}")

        // Verificar se a lista do adapter está correta
        if (::alunoAdapter.isInitialized) {
            Log.d("ADAPTER_DEBUG", "Adapter inicializado: SIM")
        } else {
            Log.d("ADAPTER_DEBUG", "Adapter inicializado: NÃO")
        }
    }

    // Método para recarregar os dados (usado apenas quando necessário)
    private fun recarregarDados() {
        dadosCarregados = false // Reset da flag
        carregarAlunosFromFirestore()
    }

    private fun configurarDadosUsuario() {
        // Recuperar os dados do usuário
        val prefs = getSharedPreferences("funcionarioPrefs", MODE_PRIVATE)
        val nome = prefs.getString("nome_usuario", "Usuário")
        nomeUser.text = "Bem Vindo, \n $nome!"
    }

    private fun configurarBusca() {
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarAlunos(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filtrarAlunos(query: String) {
        listaAlunosFiltrada.clear()

        if (query.isEmpty()) {
            listaAlunosFiltrada.addAll(listaAlunos)
        } else {
            val queryLowerCase = query.lowercase()
            listaAlunosFiltrada.addAll(
                listaAlunos.filter { aluno ->
                    aluno.nome.lowercase().contains(queryLowerCase) ||
                            aluno.sobrenome.lowercase().contains(queryLowerCase) ||
                            aluno.email.lowercase().contains(queryLowerCase) ||
                            aluno.telefone.contains(query)
                }
            )
        }
        alunoAdapter.notifyDataSetChanged()
    }

    private fun handleAlunoClick(aluno: Aluno) {
        // Verificar se o aluno tem dados completos (além do nome básico)
        val temDadosCompletos = aluno.email.isNotBlank() ||
                aluno.telefone.isNotBlank() ||
                aluno.idade.isNotBlank() ||
                aluno.genero.isNotBlank() ||
                aluno.endereco.isNotBlank() ||
                aluno.altura > 0 ||
                aluno.peso > 0

        if (temDadosCompletos) {
            // Aluno com dados completos - ir para tela de gerenciamento normal
            val intent = Intent(this, GerenciamentoDoAluno_Funcionario::class.java)

            val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            val edit = prefs.edit()

            edit.putString("alunoDocId", aluno.documentId)
            edit.putString("nome", aluno.nome)
            edit.putString("sobrenome", aluno.sobrenome)
            edit.putString("idade", aluno.idade)
            edit.putString("genero", aluno.genero)
            edit.putString("altura", aluno.altura.toString())
            edit.putString("peso", aluno.peso.toString())
            edit.putString("contusao", aluno.contusao)
            edit.putString("endereco", aluno.endereco)
            edit.putString("telefone", aluno.telefone)
            edit.putString("email", aluno.email)
            edit.apply() // aplica as mudanças

            startActivity(intent)
        } else {
            // Aluno sem dados completos - ir para tela de cadastro completo
            val intent = Intent(this, TelaGerenciamentoAlunoNovo_Funcionario::class.java)
            intent.putExtra("document_id", aluno.documentId) // ID do documento no Firestore
            intent.putExtra("nome_aluno", aluno.nome) // Nome básico já cadastrado
            startActivity(intent)
        }
    }

    private fun configurarEventos() {
        btnNotificacao.setOnClickListener {
            val intent = Intent(this, TelaNotificacao_funcionario::class.java)
            startActivity(intent)
        }

        btnNavegacao.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    // Já está na tela inicial, não faz nada
                    true
                }

                R.id.nav_chat -> {
                    try {
                        val intent = Intent(this@TelaFuncionario, TelaChat::class.java)
                        startActivity(intent)
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        true
                    } catch (e: Exception) {
                        Toast.makeText(this@TelaFuncionario, "Erro ao abrir o chat", Toast.LENGTH_SHORT).show()
                        Log.e("NAVEGACAO", "Erro ao abrir TelaChat", e)
                        false
                    }
                }

                R.id.nav_config -> {
                    val intent = Intent(this@TelaFuncionario, TelaConfiguracao_Funcionario::class.java)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }
    }

    // Método para atualizar a lista quando retornar de outras telas
    override fun onResume() {
        super.onResume()
        // Apenas recarregar se necessário (por exemplo, se dados foram alterados)
        // recarregarDados()
    }

    // Método público para forçar recarregamento quando necessário
    fun forcarRecarregamento() {
        recarregarDados()
    }
}