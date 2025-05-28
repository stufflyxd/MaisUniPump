package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.adapters.ExerciciosGrupoAdapter
import com.example.unipump.adapters.ExercicioGrupo
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class TelaCriarFichaTreino_Funcionario : AppCompatActivity() {

    private lateinit var btnNavegacao: BottomNavigationView
    private lateinit var btnSetaVoltar: ImageButton
    private lateinit var spinnerGrupoMuscular: Spinner
    private lateinit var textLetraFicha: TextView
    private lateinit var textNomeFicha: TextView
    private lateinit var tvStatusExercicios: TextView
    private lateinit var recyclerViewExercicios: RecyclerView

    // Firestore
    private lateinit var db: FirebaseFirestore

    // Adapter do RecyclerView
    private lateinit var exerciciosAdapter: ExerciciosGrupoAdapter
    private val exerciciosList = mutableListOf<ExercicioGrupo>()

    // Dados da ficha (recebidos via Intent)
    private var documentId: String = ""
    private var alunoDocId: String = ""
    private var fichaLetra: String = ""
    private var fichaNome: String = ""
    private var fichaDescricao: String = ""
    private var modoEdicao: Boolean = false
    private var action: String = ""

    // Dados antigos (compatibilidade)
    private var tipoFicha: String = ""
    private var nomeFichaAntigo: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_criar_ficha_treino_funcionario)

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance()

        // Recuperar dados do Intent
        recuperarDadosIntent()

        // Inicializar views
        initViews()

        // Configurar RecyclerView
        setupRecyclerView()

        // Configurar Spinner
        setupSpinner()

        // Configurar eventos
        configurarEventos()

        // Atualizar UI com dados da ficha
        atualizarUIComDadosFicha()
    }

    private fun recuperarDadosIntent() {
        try {
            // NOVOS dados (modo edição)
            documentId = intent.getStringExtra("documentId") ?: ""
            alunoDocId = intent.getStringExtra("alunoDocId") ?: ""
            fichaLetra = intent.getStringExtra("ficha_letra") ?: ""
            fichaNome = intent.getStringExtra("ficha_nome") ?: ""
            fichaDescricao = intent.getStringExtra("ficha_descricao") ?: ""
            modoEdicao = intent.getBooleanExtra("modo_edicao", false)
            action = intent.getStringExtra("action") ?: ""

            // Dados antigos (compatibilidade com código existente)
            tipoFicha = intent.getStringExtra("ficha") ?: fichaLetra
            nomeFichaAntigo = intent.getStringExtra("nomeFicha") ?: fichaNome

            Log.d("CRIAR_FICHA", "=== DADOS RECUPERADOS ===")
            Log.d("CRIAR_FICHA", "Document ID: '$documentId'")
            Log.d("CRIAR_FICHA", "Aluno ID: '$alunoDocId'")
            Log.d("CRIAR_FICHA", "Ficha Letra: '$fichaLetra'")
            Log.d("CRIAR_FICHA", "Ficha Nome: '$fichaNome'")
            Log.d("CRIAR_FICHA", "Modo Edição: $modoEdicao")
            Log.d("CRIAR_FICHA", "Action: '$action'")

            // CRÍTICO: Se alunoDocId estiver vazio, tentar recuperar do SharedPreferences
            if (alunoDocId.isEmpty()) {
                Log.w("CRIAR_FICHA", "⚠️ AlunoDocId vazio, tentando recuperar do SharedPreferences...")
                val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
                alunoDocId = prefs.getString("alunoDocId", "") ?: ""
                Log.d("CRIAR_FICHA", "AlunoDocId recuperado do SharedPreferences: '$alunoDocId'")

                if (alunoDocId.isEmpty()) {
                    Log.e("CRIAR_FICHA", "❌ ERRO CRÍTICO: AlunoDocId ainda está vazio após tentar SharedPreferences!")
                    Toast.makeText(this, "ERRO: ID do aluno não encontrado. Não será possível adicionar exercícios.", Toast.LENGTH_LONG).show()
                }
            }

        } catch (e: Exception) {
            Log.e("CRIAR_FICHA_ERROR", "Erro ao recuperar dados do intent", e)
        }
    }

    private fun initViews() {
        btnNavegacao = findViewById(R.id.bottom_navigation)
        btnSetaVoltar = findViewById(R.id.SetaVoltarTelaCriarFicha)
        spinnerGrupoMuscular = findViewById(R.id.spinnerGrupoMuscular)
        textLetraFicha = findViewById(R.id.letraFicha)
        textNomeFicha = findViewById(R.id.nomeFicha)
        tvStatusExercicios = findViewById(R.id.tvStatusExercicios)
        recyclerViewExercicios = findViewById(R.id.recyclerViewExercicios)
    }

    private fun setupRecyclerView() {
        exerciciosAdapter = ExerciciosGrupoAdapter(exerciciosList) { exercicio ->
            // Callback quando um exercício for selecionado
            onExercicioSelecionado(exercicio)
        }

        recyclerViewExercicios.apply {
            layoutManager = LinearLayoutManager(this@TelaCriarFichaTreino_Funcionario)
            adapter = exerciciosAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupSpinner() {
        // Lista dos grupos musculares (deve corresponder aos documentos no Firestore)
        val gruposMusculares = listOf("Selecione...", "biceps", "triceps", "pernas", "peito", "costas")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, gruposMusculares)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGrupoMuscular.adapter = adapter

        // Listener para mudanças no Spinner
        spinnerGrupoMuscular.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val grupoSelecionado = gruposMusculares[position]

                if (position == 0) { // "Selecione..."
                    // Limpar RecyclerView e mostrar mensagem
                    exerciciosAdapter.clearExercicios()
                    tvStatusExercicios.text = "Selecione um grupo muscular"
                    tvStatusExercicios.visibility = View.VISIBLE
                    recyclerViewExercicios.visibility = View.GONE
                } else {
                    // Carregar exercícios do grupo selecionado
                    carregarExerciciosDoGrupo(grupoSelecionado)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Não fazer nada
            }
        }
    }

    private fun carregarExerciciosDoGrupo(grupoMuscular: String) {
        Log.d("CARREGAR_EXERCICIOS", "=== CARREGANDO EXERCÍCIOS ===")
        Log.d("CARREGAR_EXERCICIOS", "Grupo muscular: '$grupoMuscular'")

        // Mostrar loading
        tvStatusExercicios.text = "Carregando exercícios..."
        tvStatusExercicios.visibility = View.VISIBLE
        recyclerViewExercicios.visibility = View.GONE

        // Buscar o documento e ler o array 'exercicios' dentro dele
        db.collection("grupoMuscular")
            .document(grupoMuscular)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    try {
                        Log.d("CARREGAR_EXERCICIOS", "✅ Documento '$grupoMuscular' encontrado!")

                        // Pegar o array 'exercicios' do documento
                        val exerciciosArray = documentSnapshot.get("exercicios") as? List<Map<String, Any>>

                        Log.d("CARREGAR_EXERCICIOS", "Array de exercícios: $exerciciosArray")
                        Log.d("CARREGAR_EXERCICIOS", "Tamanho do array: ${exerciciosArray?.size ?: 0}")

                        if (exerciciosArray != null && exerciciosArray.isNotEmpty()) {
                            val exercicios = mutableListOf<ExercicioGrupo>()

                            exerciciosArray.forEachIndexed { index, exercicioMap ->
                                try {
                                    Log.d("CARREGAR_EXERCICIOS", "=== PROCESSANDO EXERCÍCIO $index ===")
                                    Log.d("CARREGAR_EXERCICIOS", "Dados: $exercicioMap")

                                    val nome = exercicioMap["nome"]?.toString() ?: ""
                                    val frame = exercicioMap["frame"]?.toString() ?: ""

                                    Log.d("CARREGAR_EXERCICIOS", "Nome: '$nome'")
                                    Log.d("CARREGAR_EXERCICIOS", "Frame: '$frame'")

                                    val exercicio = ExercicioGrupo(
                                        id = "exercicio_${index}_${System.currentTimeMillis()}", // ID único
                                        nome = nome,
                                        frame = frame,
                                        grupoMuscular = grupoMuscular
                                    )

                                    exercicios.add(exercicio)
                                    Log.d("CARREGAR_EXERCICIOS", "✅ Exercício adicionado: '${exercicio.nome}'")

                                } catch (e: Exception) {
                                    Log.e("CARREGAR_EXERCICIOS", "❌ Erro ao processar exercício $index", e)
                                }
                            }

                            Log.d("CARREGAR_EXERCICIOS", "=== RESULTADO FINAL ===")
                            Log.d("CARREGAR_EXERCICIOS", "Total de exercícios processados: ${exercicios.size}")

                            runOnUiThread {
                                if (exercicios.isEmpty()) {
                                    tvStatusExercicios.text = "Nenhum exercício válido encontrado para $grupoMuscular"
                                    tvStatusExercicios.visibility = View.VISIBLE
                                    recyclerViewExercicios.visibility = View.GONE
                                } else {
                                    // Atualizar RecyclerView
                                    exerciciosAdapter.updateExercicios(exercicios)

                                    tvStatusExercicios.visibility = View.GONE
                                    recyclerViewExercicios.visibility = View.VISIBLE

                                    Log.d("CARREGAR_EXERCICIOS", "✅ ${exercicios.size} exercícios carregados para $grupoMuscular")
                                }
                            }

                        } else {
                            Log.w("CARREGAR_EXERCICIOS", "⚠️ Array 'exercicios' vazio ou não encontrado no documento '$grupoMuscular'")
                            runOnUiThread {
                                tvStatusExercicios.text = "Nenhum exercício encontrado para $grupoMuscular"
                                tvStatusExercicios.visibility = View.VISIBLE
                                recyclerViewExercicios.visibility = View.GONE
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("CARREGAR_EXERCICIOS", "❌ Erro ao processar documento '$grupoMuscular'", e)
                        runOnUiThread {
                            tvStatusExercicios.text = "Erro ao processar exercícios: ${e.message}"
                            tvStatusExercicios.visibility = View.VISIBLE
                            recyclerViewExercicios.visibility = View.GONE
                        }
                    }

                } else {
                    Log.w("CARREGAR_EXERCICIOS", "❌ Documento '$grupoMuscular' não existe")
                    runOnUiThread {
                        tvStatusExercicios.text = "Grupo muscular '$grupoMuscular' não encontrado"
                        tvStatusExercicios.visibility = View.VISIBLE
                        recyclerViewExercicios.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("CARREGAR_EXERCICIOS", "❌ Erro ao buscar documento '$grupoMuscular'", exception)

                runOnUiThread {
                    tvStatusExercicios.text = "Erro ao carregar exercícios: ${exception.localizedMessage}"
                    tvStatusExercicios.visibility = View.VISIBLE
                    recyclerViewExercicios.visibility = View.GONE

                    Toast.makeText(
                        this@TelaCriarFichaTreino_Funcionario,
                        "Erro ao carregar exercícios: ${exception.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun onExercicioSelecionado(exercicio: ExercicioGrupo) {
        Log.d("EXERCICIO_SELECIONADO", "=== EXERCÍCIO SELECIONADO ===")
        Log.d("EXERCICIO_SELECIONADO", "Nome: ${exercicio.nome}")
        Log.d("EXERCICIO_SELECIONADO", "Grupo: ${exercicio.grupoMuscular}")
        Log.d("EXERCICIO_SELECIONADO", "Modo Edição: $modoEdicao")
        Log.d("EXERCICIO_SELECIONADO", "Action: $action")

        if (modoEdicao && action == "add_exercicio") {
            // MODO EDIÇÃO: Adicionar exercício à ficha existente
            Log.d("EXERCICIO_SELECIONADO", "🎯 Adicionando exercício à ficha existente...")
            adicionarExercicioAFichaExistente(exercicio)
        } else {
            // MODO NORMAL: Navegar para próxima tela (compatibilidade com fluxo antigo)
            Log.d("EXERCICIO_SELECIONADO", "📱 Navegando para próxima tela...")
            navegarParaProximaTela(exercicio)
        }
    }

    private fun adicionarExercicioAFichaExistente(exercicio: ExercicioGrupo) {
        Log.d("ADD_EXERCICIO_FICHA", "=== ADICIONANDO EXERCÍCIO À FICHA ===")
        Log.d("ADD_EXERCICIO_FICHA", "Exercício: '${exercicio.nome}'")
        Log.d("ADD_EXERCICIO_FICHA", "Document ID: '$documentId'")
        Log.d("ADD_EXERCICIO_FICHA", "Aluno ID: '$alunoDocId'")

        // VALIDAÇÃO MELHORADA
        if (documentId.isEmpty()) {
            Log.e("ADD_EXERCICIO_FICHA", "❌ Document ID está vazio!")
            Toast.makeText(this, "ERRO: ID do documento não encontrado!", Toast.LENGTH_LONG).show()
            return
        }

        if (alunoDocId.isEmpty()) {
            Log.e("ADD_EXERCICIO_FICHA", "❌ Aluno ID está vazio!")

            // ÚLTIMA TENTATIVA: Buscar alunoDocId em mais locais
            Log.d("ADD_EXERCICIO_FICHA", "🔍 Última tentativa: buscando alunoDocId em mais locais...")

            // Tentar pegar do Intent novamente com chaves diferentes
            val alunoIdTentativas = listOf(
                intent.getStringExtra("alunoDocId"),
                intent.getStringExtra("aluno_id"),
                intent.getStringExtra("alunoId"),
                intent.getStringExtra("userId"),
                intent.getStringExtra("user_id")
            )

            alunoIdTentativas.forEachIndexed { index, tentativa ->
                Log.d("ADD_EXERCICIO_FICHA", "Tentativa $index: '$tentativa'")
                if (!tentativa.isNullOrEmpty()) {
                    alunoDocId = tentativa
                    Log.d("ADD_EXERCICIO_FICHA", "✅ AlunoDocId encontrado na tentativa $index: '$alunoDocId'")
                }
            }

            if (alunoDocId.isEmpty()) {
                Toast.makeText(this, "ERRO CRÍTICO: ID do aluno não encontrado!\nNão é possível adicionar exercícios.", Toast.LENGTH_LONG).show()
                return
            }
        }

        Log.d("ADD_EXERCICIO_FICHA", "✅ Validação OK - Document ID: '$documentId', Aluno ID: '$alunoDocId'")

        // Mostrar loading
        tvStatusExercicios.text = "Adicionando '${exercicio.nome}'..."
        tvStatusExercicios.visibility = View.VISIBLE
        recyclerViewExercicios.visibility = View.GONE

        // Caminho completo no Firestore
        val caminhoFirestore = "alunos/$alunoDocId/treino/$documentId"
        Log.d("ADD_EXERCICIO_FICHA", "📍 Caminho no Firestore: $caminhoFirestore")

        // Buscar a ficha atual no Firestore
        db.collection("alunos")
            .document(alunoDocId)
            .collection("treino")
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        Log.d("ADD_EXERCICIO_FICHA", "✅ Ficha encontrada no Firestore")
                        Log.d("ADD_EXERCICIO_FICHA", "Dados da ficha: ${document.data}")

                        // Obter exercícios existentes
                        val exerciciosExistentes = document.get("exercicios") as? List<Map<String, Any>> ?: emptyList()
                        val exerciciosAtualizados = exerciciosExistentes.toMutableList()

                        Log.d("ADD_EXERCICIO_FICHA", "Exercícios existentes: ${exerciciosExistentes.size}")

                        // Criar novo exercício com uma série padrão
                        val novoExercicio = mapOf(
                            "nome" to exercicio.nome,
                            "execucao" to "Execução normal",
                            "frame" to exercicio.frame,
                            "series" to listOf(
                                mapOf(
                                    "ordem" to "1",
                                    "reps" to "12",
                                    "peso" to "",
                                    "descanso" to "60"
                                )
                            )
                        )

                        // Adicionar à lista
                        exerciciosAtualizados.add(novoExercicio)

                        Log.d("ADD_EXERCICIO_FICHA", "Novo exercício criado: $novoExercicio")
                        Log.d("ADD_EXERCICIO_FICHA", "Total de exercícios agora: ${exerciciosAtualizados.size}")

                        // Atualizar no Firestore
                        val dadosAtualizados = hashMapOf(
                            "exercicios" to exerciciosAtualizados,
                            "quantidadeExercicios" to exerciciosAtualizados.size,
                            "dataModificacao" to com.google.firebase.Timestamp.now()
                        )

                        Log.d("ADD_EXERCICIO_FICHA", "💾 Salvando no Firestore...")

                        db.collection("alunos")
                            .document(alunoDocId)
                            .collection("treino")
                            .document(documentId)
                            .update(dadosAtualizados as Map<String, Any>)
                            .addOnSuccessListener {
                                Log.d("ADD_EXERCICIO_FICHA", "✅ Exercício salvo com sucesso no Firestore!")

                                runOnUiThread {
                                    Toast.makeText(
                                        this@TelaCriarFichaTreino_Funcionario,
                                        "✅ '${exercicio.nome}' adicionado com sucesso!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Preparar dados para retorno
                                    Log.d("ADD_EXERCICIO_FICHA", "🔙 Preparando retorno para TelaEdicaoFichaTreino_funcionario")

                                    val resultIntent = Intent().apply {
                                        putExtra("exercicio_adicionado", exercicio.nome)
                                        putExtra("exercicio_grupo", exercicio.grupoMuscular)
                                        putExtra("success", true)
                                        putExtra("message", "Exercício '${exercicio.nome}' adicionado com sucesso!")
                                    }

                                    setResult(RESULT_OK, resultIntent)

                                    // Pequeno delay para o usuário ver o toast
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        finish()
                                    }, 500)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("ADD_EXERCICIO_FICHA", "❌ Erro ao salvar exercício no Firestore", e)

                                runOnUiThread {
                                    // Restaurar estado anterior
                                    tvStatusExercicios.visibility = View.GONE
                                    recyclerViewExercicios.visibility = View.VISIBLE

                                    Toast.makeText(
                                        this@TelaCriarFichaTreino_Funcionario,
                                        "❌ Erro ao adicionar exercício: ${e.localizedMessage}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                    } catch (e: Exception) {
                        Log.e("ADD_EXERCICIO_FICHA", "❌ Erro ao processar exercícios existentes", e)

                        runOnUiThread {
                            // Restaurar estado anterior
                            tvStatusExercicios.visibility = View.GONE
                            recyclerViewExercicios.visibility = View.VISIBLE

                            Toast.makeText(
                                this@TelaCriarFichaTreino_Funcionario,
                                "❌ Erro ao processar exercícios: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Log.e("ADD_EXERCICIO_FICHA", "❌ Documento da ficha não encontrado no caminho: $caminhoFirestore")

                    runOnUiThread {
                        // Restaurar estado anterior
                        tvStatusExercicios.visibility = View.GONE
                        recyclerViewExercicios.visibility = View.VISIBLE

                        Toast.makeText(
                            this@TelaCriarFichaTreino_Funcionario,
                            "❌ Ficha não encontrada no banco de dados",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ADD_EXERCICIO_FICHA", "❌ Erro ao buscar ficha no Firestore no caminho: $caminhoFirestore", e)

                runOnUiThread {
                    // Restaurar estado anterior
                    tvStatusExercicios.visibility = View.GONE
                    recyclerViewExercicios.visibility = View.VISIBLE

                    Toast.makeText(
                        this@TelaCriarFichaTreino_Funcionario,
                        "❌ Erro ao buscar ficha: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun navegarParaProximaTela(exercicio: ExercicioGrupo) {
        // Compatibilidade com o código existente - navegar para TelaCriarFichaTreino2_Funcionario
        val intent = Intent(this, TelaCriarFichaTreino2_Funcionario::class.java)

        intent.putExtra("nomeExercicio", exercicio.nome)

        // Usar dados atualizados se disponíveis, senão usar os antigos
        val fichaLetraFinal = if (fichaLetra.isNotEmpty()) fichaLetra else tipoFicha
        val fichaNomeFinal = if (fichaNome.isNotEmpty()) fichaNome else nomeFichaAntigo

        intent.putExtra("ficha", fichaLetraFinal)
        intent.putExtra("nomeFicha", fichaNomeFinal)

        // Se estiver em modo edição, passar dados adicionais
        if (modoEdicao) {
            intent.putExtra("documentId", documentId)
            intent.putExtra("alunoDocId", alunoDocId)
            intent.putExtra("modo_edicao", true)
        }

        startActivity(intent)
    }

    private fun atualizarUIComDadosFicha() {
        // Usar dados novos se disponíveis, senão usar os antigos
        val letraParaExibir = if (fichaLetra.isNotEmpty()) fichaLetra else tipoFicha
        val nomeParaExibir = if (fichaNome.isNotEmpty()) fichaNome else nomeFichaAntigo

        textLetraFicha.text = letraParaExibir
        textNomeFicha.text = if (nomeParaExibir.isNotEmpty()) "($nomeParaExibir)" else "(nome da ficha)"

        Log.d("ATUALIZAR_UI", "Letra exibida: $letraParaExibir")
        Log.d("ATUALIZAR_UI", "Nome exibido: $nomeParaExibir")
    }

    private fun configurarEventos() {
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

        btnSetaVoltar.setOnClickListener {
            onBackPressed()
        }
    }
}