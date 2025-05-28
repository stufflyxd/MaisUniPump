package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.adapters.FichaTreinoFunAdapter
import com.example.unipump.models.exercicioFun
import com.example.unipump.models.fichaTreinoFun
import com.example.unipump.models.serieFun
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Timer
import java.util.TimerTask

class TelaEdicaoFichaTreino_funcionario : AppCompatActivity() {

    private lateinit var btnNavegacao: BottomNavigationView
    private lateinit var btnSetaVoltar: ImageButton
    private lateinit var recyclerViewFichas: RecyclerView
    private lateinit var scrollView: ScrollView

    // Firestore
    private lateinit var db: FirebaseFirestore
    private var alunoDocId: String = ""
    private var documentId: String = ""
    private var fichaLetra: String = ""
    private var fichaNome: String = ""
    private var fichaDescricao: String = ""

    @Deprecated("Use documentId instead")
    private var fichaId: String = ""

    // Adapter
    private lateinit var fichaTreinoAdapter: FichaTreinoFunAdapter
    private val fichasList = mutableListOf<fichaTreinoFun>()

    // Flag para controlar se há alterações pendentes
    private var hasUnsavedChanges = false
    private var saveDebounceTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_tela_edicao_ficha_treino_funcionario)

            // Inicializar Firestore
            db = FirebaseFirestore.getInstance()

            // Recuperar dados da ficha específica
            if (!recuperarDadosIntent()) {
                return
            }

            // Inicializar views
            initViews()

            // Configurar título da ficha
            configurarTituloFicha()

            // Configurar RecyclerView
            setupRecyclerView()

            // Configurar eventos
            configurarEventos()

            // Carregar dados do Firestore
            carregarFichaEspecificaDoFirestore()

        } catch (e: Exception) {
            Log.e("EDICAO_FICHA_ERROR", "Erro no onCreate", e)
            Toast.makeText(this, "Erro ao inicializar tela: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // Método chamado quando a activity volta ao foco
    override fun onResume() {
        super.onResume()
        Log.d("LIFECYCLE", "onResume - Recarregando dados do Firestore")

        // Sempre recarregar dados quando voltar para a tela
        recarregarDadosDoFirestore()
    }

    // Método para recarregar dados do Firestore
    private fun recarregarDadosDoFirestore() {
        Log.d("RELOAD_DATA", "=== RECARREGANDO DADOS DO FIRESTORE ===")

        // Limpar lista atual
        fichasList.clear()

        // Notificar adapter que os dados foram limpos
        if (::fichaTreinoAdapter.isInitialized) {
            fichaTreinoAdapter.notifyDataSetChanged()
        }

        // Carregar dados atualizados
        carregarFichaEspecificaDoFirestore()
    }

    // Método para tratar retorno da TelaCriarFichaTreino_Funcionario
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("ACTIVITY_RESULT", "=== ON ACTIVITY RESULT ===")
        Log.d("ACTIVITY_RESULT", "Request Code: $requestCode")
        Log.d("ACTIVITY_RESULT", "Result Code: $resultCode")

        if (requestCode == REQUEST_CODE_ADD_EXERCICIO) {
            when (resultCode) {
                RESULT_OK -> {
                    Log.d("ACTIVITY_RESULT", "✅ Exercício adicionado com sucesso!")

                    // Extrair dados do resultado
                    data?.let { intent ->
                        val exercicioAdicionado = intent.getStringExtra("exercicio_adicionado")
                        val success = intent.getBooleanExtra("success", false)

                        if (success && !exercicioAdicionado.isNullOrEmpty()) {
                            Toast.makeText(
                                this,
                                "✅ '$exercicioAdicionado' foi adicionado à ficha!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    // IMPORTANTE: Recarregar dados do Firestore
                    Log.d("ACTIVITY_RESULT", "🔄 Recarregando dados do Firestore...")

                    // Delay para garantir que o Firestore foi atualizado
                    Handler(Looper.getMainLooper()).postDelayed({
                        recarregarDadosDoFirestore()
                    }, 500) // 500ms de delay
                }

                RESULT_CANCELED -> {
                    Log.d("ACTIVITY_RESULT", "❌ Operação cancelada pelo usuário")
                }

                else -> {
                    Log.w("ACTIVITY_RESULT", "⚠️ Resultado inesperado: $resultCode")
                }
            }
        }
    }

    private fun recuperarDadosIntent(): Boolean {
        return try {
            documentId = intent.getStringExtra("documentId") ?: ""
            alunoDocId = intent.getStringExtra("alunoDocId") ?: ""
            fichaLetra = intent.getStringExtra("ficha_letra") ?: ""
            fichaNome = intent.getStringExtra("ficha_nome") ?: ""
            fichaDescricao = intent.getStringExtra("ficha_descricao") ?: ""

            fichaId = documentId

            Log.d("EDICAO_FICHA", "=== DADOS RECUPERADOS ===")
            Log.d("EDICAO_FICHA", "Document ID: $documentId")
            Log.d("EDICAO_FICHA", "Aluno ID: $alunoDocId")
            Log.d("EDICAO_FICHA", "Ficha Letra: $fichaLetra")
            Log.d("EDICAO_FICHA", "Ficha Nome: $fichaNome")

            // Se não veio por intent, tentar recuperar do SharedPreferences
            if (alunoDocId.isEmpty()) {
                val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
                alunoDocId = prefs.getString("alunoDocId", "") ?: ""
                Log.d("EDICAO_FICHA", "Aluno ID recuperado do SharedPreferences: $alunoDocId")
            }

            // Verificações de segurança
            when {
                documentId.isEmpty() -> {
                    Toast.makeText(this, "ERRO: ID do documento não encontrado!", Toast.LENGTH_LONG).show()
                    Log.e("EDICAO_FICHA", "Document ID está vazio!")
                    finish()
                    false
                }
                alunoDocId.isEmpty() -> {
                    Toast.makeText(this, "ERRO: ID do aluno não encontrado!", Toast.LENGTH_LONG).show()
                    Log.e("EDICAO_FICHA", "Aluno ID está vazio!")
                    finish()
                    false
                }
                fichaLetra.isEmpty() -> {
                    Toast.makeText(this, "ERRO: Letra da ficha não encontrada!", Toast.LENGTH_SHORT).show()
                    Log.e("EDICAO_FICHA", "Ficha letra está vazia!")
                    finish()
                    false
                }
                else -> {
                    Log.d("EDICAO_FICHA", "✅ Todos os dados necessários foram recuperados com sucesso")
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("EDICAO_FICHA_ERROR", "Erro ao recuperar dados do intent", e)
            Toast.makeText(this, "Erro ao recuperar dados", Toast.LENGTH_SHORT).show()
            finish()
            false
        }
    }

    private fun initViews() {
        try {
            btnNavegacao = findViewById(R.id.bottom_navigation)
            btnSetaVoltar = findViewById(R.id.SetaVoltarTelaEdicaoFicha)
            recyclerViewFichas = findViewById(R.id.recyclerViewFichas)
            scrollView = findViewById(R.id.scrollView)

            Log.d("EDICAO_FICHA", "Views inicializadas com sucesso")
        } catch (e: Exception) {
            Log.e("EDICAO_FICHA_ERROR", "Erro ao inicializar views", e)
            throw e
        }
    }

    private fun configurarTituloFicha() {
        Log.d("EDICAO_FICHA", "Título configurado para: Ficha $fichaLetra - $fichaNome")
    }

    private fun setupRecyclerView() {
        // Configurar adapter com callbacks
        fichaTreinoAdapter = FichaTreinoFunAdapter(
            fichas = fichasList,
            onExcluirFicha = { fichaIdCallback: String, position: Int ->
                excluirFichaCompleta(fichaIdCallback, position)
            },
            onFichaAlterada = { fichaAlterada: fichaTreinoFun, position: Int ->
                Log.d("FICHA_ALTERADA", "Ficha alterada: ${fichaAlterada.letra} - ${fichaAlterada.nome}")
                Log.d("FICHA_ALTERADA", "Total de exercícios: ${fichaAlterada.exercicios.size}")

                onFichaAlterada(fichaAlterada, position)
            }
        )

        recyclerViewFichas.apply {
            layoutManager = LinearLayoutManager(this@TelaEdicaoFichaTreino_funcionario)
            adapter = fichaTreinoAdapter
            isNestedScrollingEnabled = false
            setHasFixedSize(false)
            itemAnimator = null
        }

        Log.d("EDICAO_FICHA", "RecyclerView configurado com sucesso")
    }

    // Método chamado quando uma ficha é alterada
    private fun onFichaAlterada(fichaAlterada: fichaTreinoFun, position: Int) {
        Log.d("FICHA_ALTERADA", "Ficha alterada: ${fichaAlterada.letra} - ${fichaAlterada.nome}")

        hasUnsavedChanges = true

        // Cancelar timer anterior
        saveDebounceTimer?.cancel()

        // Criar novo timer com delay de 1 segundo (debounce)
        saveDebounceTimer = Timer()
        saveDebounceTimer?.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    Log.d("DEBOUNCE_SAVE", "Executando salvamento com debounce")
                    salvarFichaAlteradaNoFirestore(fichaAlterada)
                }
            }
        }, 1000)
    }

    private fun salvarFichaAlteradaNoFirestore(ficha: fichaTreinoFun) {
        Log.d("FIRESTORE_SAVE_IMMEDIATE", "=== SALVANDO ALTERAÇÕES ===")
        Log.d("FIRESTORE_SAVE_IMMEDIATE", "Document ID: $documentId")
        Log.d("FIRESTORE_SAVE_IMMEDIATE", "Ficha: ${ficha.letra} - ${ficha.nome}")

        if (documentId.isEmpty() || alunoDocId.isEmpty()) {
            Log.e("FIRESTORE_SAVE_IMMEDIATE", "❌ IDs inválidos")
            return
        }

        val letraAtualizada = ficha.letra
        val nomeAtualizado = ficha.nome

        // MODIFICADO: Converter exercícios para o formato do Firestore COM FRAME
        val exerciciosArray = ficha.exercicios.map { exercicio ->
            mapOf(
                "nome" to exercicio.nome,
                "frame" to (exercicio.frame ?: ""), // NOVO: Incluir frame real
                "execucao" to "Execução normal",
                "series" to exercicio.series.map { serie ->
                    mapOf(
                        "ordem" to serie.numero.toString(),
                        "reps" to serie.repeticoes.toString(),
                        "peso" to serie.peso,
                        "descanso" to serie.tempo
                    )
                }
            )
        }

        val fichaData = hashMapOf(
            "letra" to letraAtualizada,
            "nome" to nomeAtualizado,
            "descricao" to fichaDescricao,
            "quantidadeExercicios" to ficha.exercicios.size,
            "exercicios" to exerciciosArray,
            "dataModificacao" to com.google.firebase.Timestamp.now()
        )

        db.collection("alunos")
            .document(alunoDocId)
            .collection("treino")
            .document(documentId)
            .update(fichaData as Map<String, Any>)
            .addOnSuccessListener {
                Log.d("FIRESTORE_SAVE_IMMEDIATE", "✅ Ficha '$letraAtualizada' salva com sucesso!")
                hasUnsavedChanges = false

                fichaLetra = letraAtualizada
                fichaNome = nomeAtualizado
            }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE_SAVE_IMMEDIATE", "❌ Erro ao salvar ficha", e)
                hasUnsavedChanges = true

                runOnUiThread {
                    Toast.makeText(this@TelaEdicaoFichaTreino_funcionario,
                        "Erro ao salvar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }

                // Retry após 5 segundos
                Handler(Looper.getMainLooper()).postDelayed({
                    if (hasUnsavedChanges) {
                        Log.d("RETRY_SAVE", "Tentando salvar novamente após erro")
                        salvarFichaAlteradaNoFirestore(ficha)
                    }
                }, 5000)
            }
    }

    private fun excluirFichaCompleta(fichaIdCallback: String, position: Int) {
        Log.d("EXCLUIR_FICHA", "Iniciando exclusão da ficha ID: $fichaIdCallback, posição: $position")

        excluirFichaDoFirestore(fichaIdCallback) { sucesso ->
            runOnUiThread {
                if (sucesso) {
                    fichaTreinoAdapter.removeFicha(position)

                    Toast.makeText(
                        this@TelaEdicaoFichaTreino_funcionario,
                        "Ficha excluída com sucesso",
                        Toast.LENGTH_SHORT
                    ).show()

                    if (fichasList.isEmpty()) {
                        Toast.makeText(
                            this@TelaEdicaoFichaTreino_funcionario,
                            "Não há mais fichas para editar",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                } else {
                    Toast.makeText(
                        this@TelaEdicaoFichaTreino_funcionario,
                        "Erro ao excluir ficha",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun excluirFichaDoFirestore(fichaIdToDelete: String, callback: (Boolean) -> Unit) {
        Log.d("FIRESTORE_DELETE", "Excluindo ficha do Firestore - ID: $fichaIdToDelete")

        db.collection("alunos")
            .document(alunoDocId)
            .collection("treino")
            .document(fichaIdToDelete)
            .delete()
            .addOnSuccessListener {
                Log.d("FIRESTORE_DELETE", "Ficha excluída do Firestore com sucesso")
                callback(true)
            }
            .addOnFailureListener { exception ->
                Log.e("FIRESTORE_DELETE", "Erro ao excluir ficha do Firestore", exception)
                callback(false)
            }
    }

    // MÉTODO PRINCIPAL: Carrega dados do Firestore e atualiza RecyclerView COM FRAME
    private fun carregarFichaEspecificaDoFirestore() {
        try {
            Log.d("FIRESTORE_EDICAO", "=== CARREGANDO FICHA ESPECÍFICA COM GLIDE ===")
            Log.d("FIRESTORE_EDICAO", "Document ID: $documentId")
            Log.d("FIRESTORE_EDICAO", "Aluno ID: $alunoDocId")

            db.collection("alunos")
                .document(alunoDocId)
                .collection("treino")
                .document(documentId)
                .get()
                .addOnSuccessListener { document ->
                    try {
                        Log.d("FIRESTORE_EDICAO", "Documento encontrado: ${document.exists()}")

                        if (!document.exists()) {
                            Log.w("FIRESTORE_EDICAO", "Documento não encontrado: $documentId")
                            runOnUiThread {
                                Toast.makeText(
                                    this@TelaEdicaoFichaTreino_funcionario,
                                    "Ficha não encontrada", Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                            return@addOnSuccessListener
                        }

                        // Processar dados do documento
                        val letra = document.getString("letra") ?: ""
                        val nome = document.getString("nome") ?: ""

                        Log.d("FIRESTORE_EDICAO", "Processando ficha: $letra - $nome")

                        val exerciciosArray = document.get("exercicios") as? List<Map<String, Any>> ?: emptyList()
                        Log.d("FIRESTORE_EDICAO", "Exercícios encontrados: ${exerciciosArray.size}")

                        val exerciciosList = mutableListOf<exercicioFun>()

                        // MODIFICADO: Processar cada exercício COM FRAME
                        exerciciosArray.forEachIndexed { exercicioIndex, exercicioMap ->
                            try {
                                val nomeExercicio = exercicioMap["nome"]?.toString() ?: ""

                                // NOVO: Incluir campo frame
                                val frameExercicio = exercicioMap["frame"]?.toString() ?: ""

                                val seriesArray = exercicioMap["series"] as? List<Map<String, Any>> ?: emptyList()

                                val seriesList = seriesArray.mapIndexed { serieIndex, serieMap ->
                                    serieFun(
                                        id = "${documentId}_ex${exercicioIndex}_serie${serieIndex}",
                                        numero = serieMap["ordem"]?.toString()?.toIntOrNull() ?: (serieIndex + 1),
                                        repeticoes = serieMap["reps"]?.toString()?.toIntOrNull() ?: 0,
                                        peso = serieMap["peso"]?.toString() ?: "",
                                        tempo = serieMap["descanso"]?.toString() ?: ""
                                    )
                                }.toMutableList()

                                val exercicio = exercicioFun(
                                    id = "${documentId}_exercicio_$exercicioIndex",
                                    nome = nomeExercicio,
                                    frame = frameExercicio, // NOVO: Incluir frame no objeto
                                    series = seriesList
                                )

                                exerciciosList.add(exercicio)
                                Log.d("FIRESTORE_EDICAO", "Exercício processado: $nomeExercicio (Frame: '$frameExercicio') (${seriesList.size} séries)")

                            } catch (e: Exception) {
                                Log.e("FIRESTORE_EDICAO", "Erro ao processar exercício $exercicioIndex", e)
                            }
                        }

                        // Criar ficha com dados carregados
                        val ficha = fichaTreinoFun(
                            id = documentId,
                            letra = letra,
                            nome = nome,
                            exercicios = exerciciosList
                        )

                        // IMPORTANTE: Limpar lista e adicionar nova ficha
                        fichasList.clear()
                        fichasList.add(ficha)

                        Log.d("FIRESTORE_EDICAO", "Ficha carregada: $letra - $nome com ${exerciciosList.size} exercícios")

                        // Atualizar UI na thread principal
                        runOnUiThread {
                            try {
                                if (::fichaTreinoAdapter.isInitialized) {
                                    Log.d("FIRESTORE_EDICAO", "Atualizando RecyclerView com Glide...")

                                    // Notificar que os dados mudaram
                                    fichaTreinoAdapter.notifyDataSetChanged()

                                    // Atualizar contadores após um pequeno delay
                                    recyclerViewFichas.post {
                                        fichaTreinoAdapter.atualizarContadores()

                                        Log.d("FIRESTORE_EDICAO", "RecyclerView atualizado:")
                                        Log.d("FIRESTORE_EDICAO", "- Adapter item count: ${fichaTreinoAdapter.itemCount}")
                                        Log.d("FIRESTORE_EDICAO", "- Exercícios na ficha: ${ficha.exercicios.size}")
                                    }
                                }

                                Toast.makeText(
                                    this@TelaEdicaoFichaTreino_funcionario,
                                    "Ficha $letra carregada com ${exerciciosList.size} exercícios",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } catch (e: Exception) {
                                Log.e("FIRESTORE_EDICAO", "Erro ao atualizar UI", e)
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("FIRESTORE_EDICAO", "Erro ao processar documento", e)
                        runOnUiThread {
                            Toast.makeText(
                                this@TelaEdicaoFichaTreino_funcionario,
                                "Erro ao processar ficha: ${e.message}", Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("FIRESTORE_EDICAO", "Erro ao carregar ficha", exception)
                    runOnUiThread {
                        Toast.makeText(
                            this@TelaEdicaoFichaTreino_funcionario,
                            "Erro ao carregar ficha: ${exception.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

        } catch (e: Exception) {
            Log.e("FIRESTORE_EDICAO", "Erro ao iniciar carregamento", e)
            Toast.makeText(this, "Erro ao carregar ficha: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun configurarEventos() {
        try {
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
                Log.d("LIFECYCLE", "Botão voltar pressionado - Salvando alterações")
                salvarTodasAlteracoesPendentes()
                finish()
            }

            Log.d("EDICAO_FICHA", "Eventos configurados com sucesso")

        } catch (e: Exception) {
            Log.e("EDICAO_FICHA_ERROR", "Erro ao configurar eventos", e)
            throw e
        }
    }

    // Métodos do ciclo de vida para salvar alterações
    override fun onPause() {
        super.onPause()
        Log.d("LIFECYCLE", "onPause - Salvando alterações pendentes")
        salvarTodasAlteracoesPendentes()
    }

    override fun onStop() {
        super.onStop()
        Log.d("LIFECYCLE", "onStop - Salvando alterações pendentes")
        salvarTodasAlteracoesPendentes()
    }

    override fun onDestroy() {
        Log.d("LIFECYCLE", "onDestroy - Limpando recursos")
        saveDebounceTimer?.cancel()
        salvarTodasAlteracoesPendentes()
        super.onDestroy()
    }

    override fun onBackPressed() {
        Log.d("LIFECYCLE", "onBackPressed - Salvando alterações")
        salvarTodasAlteracoesPendentes()
        super.onBackPressed()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        Log.d("LIFECYCLE", "onUserLeaveHint - Salvando alterações")
        salvarTodasAlteracoesPendentes()
    }

    private fun salvarTodasAlteracoesPendentes() {
        try {
            if (::fichaTreinoAdapter.isInitialized) {
                fichaTreinoAdapter.salvarTodasAlteracoesPendentes()
            }

            if (hasUnsavedChanges) {
                Log.d("SAVE_PENDING", "Salvando alterações pendentes...")
                salvarAlteracoesNoFirestore()
            }
        } catch (e: Exception) {
            Log.e("SAVE_PENDING_ERROR", "Erro ao salvar alterações pendentes", e)
        }
    }

    private fun salvarAlteracoesNoFirestore() {
        if (fichasList.isNotEmpty()) {
            val ficha = fichasList.first()
            salvarFichaAlteradaNoFirestore(ficha)
        }
    }

    // MÉTODO PÚBLICO: Para forçar recarregamento quando necessário
    fun recarregarFicha() {
        Log.d("PUBLIC_RELOAD", "Método público de recarregamento chamado")
        recarregarDadosDoFirestore()
    }

    companion object {
        private const val REQUEST_CODE_ADD_EXERCICIO = 1001
    }
}



/*
package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.adapters.FichaTreinoFunAdapter
import com.example.unipump.models.exercicioFun
import com.example.unipump.models.fichaTreinoFun
import com.example.unipump.models.serieFun
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Timer
import java.util.TimerTask

class TelaEdicaoFichaTreino_funcionario : AppCompatActivity() {

    private lateinit var btnNavegacao: BottomNavigationView
    private lateinit var btnSetaVoltar: ImageButton
    private lateinit var recyclerViewFichas: RecyclerView
    private lateinit var scrollView: ScrollView

    // Firestore
    private lateinit var db: FirebaseFirestore
    private var alunoDocId: String = ""
    private var documentId: String = ""
    private var fichaLetra: String = ""
    private var fichaNome: String = ""
    private var fichaDescricao: String = ""

    @Deprecated("Use documentId instead")
    private var fichaId: String = ""

    // Adapter
    private lateinit var fichaTreinoAdapter: FichaTreinoFunAdapter
    private val fichasList = mutableListOf<fichaTreinoFun>()

    // Flag para controlar se há alterações pendentes
    private var hasUnsavedChanges = false
    private var saveDebounceTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_tela_edicao_ficha_treino_funcionario)

            // Inicializar Firestore
            db = FirebaseFirestore.getInstance()

            // Recuperar dados da ficha específica
            if (!recuperarDadosIntent()) {
                return
            }


            // Inicializar views
            initViews()

            // Configurar título da ficha
            configurarTituloFicha()

            // Configurar RecyclerView
            setupRecyclerView()

            // Configurar eventos
            configurarEventos()

            // Carregar dados do Firestore
            carregarFichaEspecificaDoFirestore()

        } catch (e: Exception) {
            Log.e("EDICAO_FICHA_ERROR", "Erro no onCreate", e)
            Toast.makeText(this, "Erro ao inicializar tela: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // NOVO: Método chamado quando a activity volta ao foco
    override fun onResume() {
        super.onResume()
        Log.d("LIFECYCLE", "onResume - Recarregando dados do Firestore")

        // Sempre recarregar dados quando voltar para a tela
        recarregarDadosDoFirestore()
    }

    // NOVO: Método para recarregar dados do Firestore
    private fun recarregarDadosDoFirestore() {
        Log.d("RELOAD_DATA", "=== RECARREGANDO DADOS DO FIRESTORE ===")

        // Limpar lista atual
        fichasList.clear()

        // Notificar adapter que os dados foram limpos
        if (::fichaTreinoAdapter.isInitialized) {
            fichaTreinoAdapter.notifyDataSetChanged()
        }

        // Carregar dados atualizados
        carregarFichaEspecificaDoFirestore()
    }

    // Método para tratar retorno da TelaCriarFichaTreino_Funcionario
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("ACTIVITY_RESULT", "=== ON ACTIVITY RESULT ===")
        Log.d("ACTIVITY_RESULT", "Request Code: $requestCode")
        Log.d("ACTIVITY_RESULT", "Result Code: $resultCode")

        if (requestCode == REQUEST_CODE_ADD_EXERCICIO) {
            when (resultCode) {
                RESULT_OK -> {
                    Log.d("ACTIVITY_RESULT", "✅ Exercício adicionado com sucesso!")

                    // Extrair dados do resultado
                    data?.let { intent ->
                        val exercicioAdicionado = intent.getStringExtra("exercicio_adicionado")
                        val success = intent.getBooleanExtra("success", false)

                        if (success && !exercicioAdicionado.isNullOrEmpty()) {
                            Toast.makeText(
                                this,
                                "✅ '$exercicioAdicionado' foi adicionado à ficha!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    // IMPORTANTE: Recarregar dados do Firestore
                    Log.d("ACTIVITY_RESULT", "🔄 Recarregando dados do Firestore...")

                    // Delay para garantir que o Firestore foi atualizado
                    Handler(Looper.getMainLooper()).postDelayed({
                        recarregarDadosDoFirestore()
                    }, 500) // 500ms de delay
                }

                RESULT_CANCELED -> {
                    Log.d("ACTIVITY_RESULT", "❌ Operação cancelada pelo usuário")
                }

                else -> {
                    Log.w("ACTIVITY_RESULT", "⚠️ Resultado inesperado: $resultCode")
                }
            }
        }
    }

    private fun recuperarDadosIntent(): Boolean {
        return try {
            documentId = intent.getStringExtra("documentId") ?: ""
            alunoDocId = intent.getStringExtra("alunoDocId") ?: ""
            fichaLetra = intent.getStringExtra("ficha_letra") ?: ""
            fichaNome = intent.getStringExtra("ficha_nome") ?: ""
            fichaDescricao = intent.getStringExtra("ficha_descricao") ?: ""

            fichaId = documentId

            Log.d("EDICAO_FICHA", "=== DADOS RECUPERADOS ===")
            Log.d("EDICAO_FICHA", "Document ID: $documentId")
            Log.d("EDICAO_FICHA", "Aluno ID: $alunoDocId")
            Log.d("EDICAO_FICHA", "Ficha Letra: $fichaLetra")
            Log.d("EDICAO_FICHA", "Ficha Nome: $fichaNome")

            // Se não veio por intent, tentar recuperar do SharedPreferences
            if (alunoDocId.isEmpty()) {
                val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
                alunoDocId = prefs.getString("alunoDocId", "") ?: ""
                Log.d("EDICAO_FICHA", "Aluno ID recuperado do SharedPreferences: $alunoDocId")
            }

            // Verificações de segurança
            when {
                documentId.isEmpty() -> {
                    Toast.makeText(this, "ERRO: ID do documento não encontrado!", Toast.LENGTH_LONG).show()
                    Log.e("EDICAO_FICHA", "Document ID está vazio!")
                    finish()
                    false
                }
                alunoDocId.isEmpty() -> {
                    Toast.makeText(this, "ERRO: ID do aluno não encontrado!", Toast.LENGTH_LONG).show()
                    Log.e("EDICAO_FICHA", "Aluno ID está vazio!")
                    finish()
                    false
                }
                fichaLetra.isEmpty() -> {
                    Toast.makeText(this, "ERRO: Letra da ficha não encontrada!", Toast.LENGTH_SHORT).show()
                    Log.e("EDICAO_FICHA", "Ficha letra está vazia!")
                    finish()
                    false
                }
                else -> {
                    Log.d("EDICAO_FICHA", "✅ Todos os dados necessários foram recuperados com sucesso")
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("EDICAO_FICHA_ERROR", "Erro ao recuperar dados do intent", e)
            Toast.makeText(this, "Erro ao recuperar dados", Toast.LENGTH_SHORT).show()
            finish()
            false
        }
    }

    private fun initViews() {
        try {
            btnNavegacao = findViewById(R.id.bottom_navigation)
            btnSetaVoltar = findViewById(R.id.SetaVoltarTelaEdicaoFicha)
            recyclerViewFichas = findViewById(R.id.recyclerViewFichas)
            scrollView = findViewById(R.id.scrollView)

            Log.d("EDICAO_FICHA", "Views inicializadas com sucesso")
        } catch (e: Exception) {
            Log.e("EDICAO_FICHA_ERROR", "Erro ao inicializar views", e)
            throw e
        }
    }

    private fun configurarTituloFicha() {
        Log.d("EDICAO_FICHA", "Título configurado para: Ficha $fichaLetra - $fichaNome")
    }

    private fun setupRecyclerView() {
        // Configurar adapter com callbacks
        fichaTreinoAdapter = FichaTreinoFunAdapter(
            fichas = fichasList,
            onExcluirFicha = { fichaIdCallback: String, position: Int ->
                excluirFichaCompleta(fichaIdCallback, position)
            },
            onFichaAlterada = { fichaAlterada: fichaTreinoFun, position: Int ->
                Log.d("FICHA_ALTERADA", "Ficha alterada: ${fichaAlterada.letra} - ${fichaAlterada.nome}")
                Log.d("FICHA_ALTERADA", "Total de exercícios: ${fichaAlterada.exercicios.size}")

                onFichaAlterada(fichaAlterada, position)
            }
        )

        recyclerViewFichas.apply {
            layoutManager = LinearLayoutManager(this@TelaEdicaoFichaTreino_funcionario)
            adapter = fichaTreinoAdapter
            isNestedScrollingEnabled = false
            setHasFixedSize(false)
            itemAnimator = null
        }

        Log.d("EDICAO_FICHA", "RecyclerView configurado com sucesso")
    }

    // Método chamado quando uma ficha é alterada
    private fun onFichaAlterada(fichaAlterada: fichaTreinoFun, position: Int) {
        Log.d("FICHA_ALTERADA", "Ficha alterada: ${fichaAlterada.letra} - ${fichaAlterada.nome}")

        hasUnsavedChanges = true

        // Cancelar timer anterior
        saveDebounceTimer?.cancel()

        // Criar novo timer com delay de 1 segundo (debounce)
        saveDebounceTimer = Timer()
        saveDebounceTimer?.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    Log.d("DEBOUNCE_SAVE", "Executando salvamento com debounce")
                    salvarFichaAlteradaNoFirestore(fichaAlterada)
                }
            }
        }, 1000)
    }

    private fun salvarFichaAlteradaNoFirestore(ficha: fichaTreinoFun) {
        Log.d("FIRESTORE_SAVE_IMMEDIATE", "=== SALVANDO ALTERAÇÕES ===")
        Log.d("FIRESTORE_SAVE_IMMEDIATE", "Document ID: $documentId")
        Log.d("FIRESTORE_SAVE_IMMEDIATE", "Ficha: ${ficha.letra} - ${ficha.nome}")

        if (documentId.isEmpty() || alunoDocId.isEmpty()) {
            Log.e("FIRESTORE_SAVE_IMMEDIATE", "❌ IDs inválidos")
            return
        }

        val letraAtualizada = ficha.letra
        val nomeAtualizado = ficha.nome

        // Converter exercícios para o formato do Firestore
        val exerciciosArray = ficha.exercicios.map { exercicio ->
            mapOf(
                "nome" to exercicio.nome,
                "execucao" to "Execução normal",
                "frame" to "URL...",
                "series" to exercicio.series.map { serie ->
                    mapOf(
                        "ordem" to serie.numero.toString(),
                        "reps" to serie.repeticoes.toString(),
                        "peso" to serie.peso,
                        "descanso" to serie.tempo
                    )
                }
            )
        }

        val fichaData = hashMapOf(
            "letra" to letraAtualizada,
            "nome" to nomeAtualizado,
            "descricao" to fichaDescricao,
            "quantidadeExercicios" to ficha.exercicios.size,
            "exercicios" to exerciciosArray,
            "dataModificacao" to com.google.firebase.Timestamp.now()
        )

        db.collection("alunos")
            .document(alunoDocId)
            .collection("treino")
            .document(documentId)
            .update(fichaData as Map<String, Any>)
            .addOnSuccessListener {
                Log.d("FIRESTORE_SAVE_IMMEDIATE", "✅ Ficha '$letraAtualizada' salva com sucesso!")
                hasUnsavedChanges = false

                fichaLetra = letraAtualizada
                fichaNome = nomeAtualizado
            }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE_SAVE_IMMEDIATE", "❌ Erro ao salvar ficha", e)
                hasUnsavedChanges = true

                runOnUiThread {
                    Toast.makeText(this@TelaEdicaoFichaTreino_funcionario,
                        "Erro ao salvar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }

                // Retry após 5 segundos
                Handler(Looper.getMainLooper()).postDelayed({
                    if (hasUnsavedChanges) {
                        Log.d("RETRY_SAVE", "Tentando salvar novamente após erro")
                        salvarFichaAlteradaNoFirestore(ficha)
                    }
                }, 5000)
            }
    }

    private fun excluirFichaCompleta(fichaIdCallback: String, position: Int) {
        Log.d("EXCLUIR_FICHA", "Iniciando exclusão da ficha ID: $fichaIdCallback, posição: $position")

        excluirFichaDoFirestore(fichaIdCallback) { sucesso ->
            runOnUiThread {
                if (sucesso) {
                    fichaTreinoAdapter.removeFicha(position)

                    Toast.makeText(
                        this@TelaEdicaoFichaTreino_funcionario,
                        "Ficha excluída com sucesso",
                        Toast.LENGTH_SHORT
                    ).show()

                    if (fichasList.isEmpty()) {
                        Toast.makeText(
                            this@TelaEdicaoFichaTreino_funcionario,
                            "Não há mais fichas para editar",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                } else {
                    Toast.makeText(
                        this@TelaEdicaoFichaTreino_funcionario,
                        "Erro ao excluir ficha",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun excluirFichaDoFirestore(fichaIdToDelete: String, callback: (Boolean) -> Unit) {
        Log.d("FIRESTORE_DELETE", "Excluindo ficha do Firestore - ID: $fichaIdToDelete")

        db.collection("alunos")
            .document(alunoDocId)
            .collection("treino")
            .document(fichaIdToDelete)
            .delete()
            .addOnSuccessListener {
                Log.d("FIRESTORE_DELETE", "Ficha excluída do Firestore com sucesso")
                callback(true)
            }
            .addOnFailureListener { exception ->
                Log.e("FIRESTORE_DELETE", "Erro ao excluir ficha do Firestore", exception)
                callback(false)
            }
    }

    // MÉTODO PRINCIPAL: Carrega dados do Firestore e atualiza RecyclerView
    private fun carregarFichaEspecificaDoFirestore() {
        try {
            Log.d("FIRESTORE_EDICAO", "=== CARREGANDO FICHA ESPECÍFICA ===")
            Log.d("FIRESTORE_EDICAO", "Document ID: $documentId")
            Log.d("FIRESTORE_EDICAO", "Aluno ID: $alunoDocId")

            // Mostrar loading se necessário
            // progressBar?.visibility = View.VISIBLE

            db.collection("alunos")
                .document(alunoDocId)
                .collection("treino")
                .document(documentId)
                .get()
                .addOnSuccessListener { document ->
                    try {
                        Log.d("FIRESTORE_EDICAO", "Documento encontrado: ${document.exists()}")

                        if (!document.exists()) {
                            Log.w("FIRESTORE_EDICAO", "Documento não encontrado: $documentId")
                            runOnUiThread {
                                Toast.makeText(
                                    this@TelaEdicaoFichaTreino_funcionario,
                                    "Ficha não encontrada", Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                            return@addOnSuccessListener
                        }

                        // Processar dados do documento
                        val letra = document.getString("letra") ?: ""
                        val nome = document.getString("nome") ?: ""

                        Log.d("FIRESTORE_EDICAO", "Processando ficha: $letra - $nome")

                        val exerciciosArray = document.get("exercicios") as? List<Map<String, Any>> ?: emptyList()
                        Log.d("FIRESTORE_EDICAO", "Exercícios encontrados: ${exerciciosArray.size}")

                        val exerciciosList = mutableListOf<exercicioFun>()

                        // Processar cada exercício
                        exerciciosArray.forEachIndexed { exercicioIndex, exercicioMap ->
                            try {
                                val nomeExercicio = exercicioMap["nome"]?.toString() ?: ""
                                val seriesArray = exercicioMap["series"] as? List<Map<String, Any>> ?: emptyList()

                                val seriesList = seriesArray.mapIndexed { serieIndex, serieMap ->
                                    serieFun(
                                        id = "${documentId}_ex${exercicioIndex}_serie${serieIndex}",
                                        numero = serieMap["ordem"]?.toString()?.toIntOrNull() ?: (serieIndex + 1),
                                        repeticoes = serieMap["reps"]?.toString()?.toIntOrNull() ?: 0,
                                        peso = serieMap["peso"]?.toString() ?: "",
                                        tempo = serieMap["descanso"]?.toString() ?: ""
                                    )
                                }.toMutableList()

                                val exercicio = exercicioFun(
                                    id = "${documentId}_exercicio_$exercicioIndex",
                                    nome = nomeExercicio,
                                    series = seriesList
                                )

                                exerciciosList.add(exercicio)
                                Log.d("FIRESTORE_EDICAO", "Exercício processado: $nomeExercicio (${seriesList.size} séries)")

                            } catch (e: Exception) {
                                Log.e("FIRESTORE_EDICAO", "Erro ao processar exercício $exercicioIndex", e)
                            }
                        }

                        // Criar ficha com dados carregados
                        val ficha = fichaTreinoFun(
                            id = documentId,
                            letra = letra,
                            nome = nome,
                            exercicios = exerciciosList
                        )

                        // IMPORTANTE: Limpar lista e adicionar nova ficha
                        fichasList.clear()
                        fichasList.add(ficha)

                        Log.d("FIRESTORE_EDICAO", "Ficha carregada: $letra - $nome com ${exerciciosList.size} exercícios")

                        // Atualizar UI na thread principal
                        runOnUiThread {
                            try {
                                // Ocultar loading
                                // progressBar?.visibility = View.GONE

                                if (::fichaTreinoAdapter.isInitialized) {
                                    Log.d("FIRESTORE_EDICAO", "Atualizando RecyclerView...")

                                    // Notificar que os dados mudaram
                                    fichaTreinoAdapter.notifyDataSetChanged()

                                    // Atualizar contadores após um pequeno delay
                                    recyclerViewFichas.post {
                                        fichaTreinoAdapter.atualizarContadores()

                                        Log.d("FIRESTORE_EDICAO", "RecyclerView atualizado:")
                                        Log.d("FIRESTORE_EDICAO", "- Adapter item count: ${fichaTreinoAdapter.itemCount}")
                                        Log.d("FIRESTORE_EDICAO", "- Exercícios na ficha: ${ficha.exercicios.size}")
                                    }
                                }

                                Toast.makeText(
                                    this@TelaEdicaoFichaTreino_funcionario,
                                    "Ficha $letra carregada com ${exerciciosList.size} exercícios",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } catch (e: Exception) {
                                Log.e("FIRESTORE_EDICAO", "Erro ao atualizar UI", e)
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("FIRESTORE_EDICAO", "Erro ao processar documento", e)
                        runOnUiThread {
                            Toast.makeText(
                                this@TelaEdicaoFichaTreino_funcionario,
                                "Erro ao processar ficha: ${e.message}", Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("FIRESTORE_EDICAO", "Erro ao carregar ficha", exception)
                    runOnUiThread {
                        // progressBar?.visibility = View.GONE
                        Toast.makeText(
                            this@TelaEdicaoFichaTreino_funcionario,
                            "Erro ao carregar ficha: ${exception.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

        } catch (e: Exception) {
            Log.e("FIRESTORE_EDICAO", "Erro ao iniciar carregamento", e)
            Toast.makeText(this, "Erro ao carregar ficha: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun configurarEventos() {
        try {
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
                Log.d("LIFECYCLE", "Botão voltar pressionado - Salvando alterações")
                salvarTodasAlteracoesPendentes()
                finish()
            }

            Log.d("EDICAO_FICHA", "Eventos configurados com sucesso")

        } catch (e: Exception) {
            Log.e("EDICAO_FICHA_ERROR", "Erro ao configurar eventos", e)
            throw e
        }
    }

    // Métodos do ciclo de vida para salvar alterações
    override fun onPause() {
        super.onPause()
        Log.d("LIFECYCLE", "onPause - Salvando alterações pendentes")
        salvarTodasAlteracoesPendentes()
    }

    override fun onStop() {
        super.onStop()
        Log.d("LIFECYCLE", "onStop - Salvando alterações pendentes")
        salvarTodasAlteracoesPendentes()
    }

    override fun onDestroy() {
        Log.d("LIFECYCLE", "onDestroy - Limpando recursos")
        saveDebounceTimer?.cancel()
        salvarTodasAlteracoesPendentes()
        super.onDestroy()
    }

    override fun onBackPressed() {
        Log.d("LIFECYCLE", "onBackPressed - Salvando alterações")
        salvarTodasAlteracoesPendentes()
        super.onBackPressed()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        Log.d("LIFECYCLE", "onUserLeaveHint - Salvando alterações")
        salvarTodasAlteracoesPendentes()
    }

    private fun salvarTodasAlteracoesPendentes() {
        try {
            if (::fichaTreinoAdapter.isInitialized) {
                fichaTreinoAdapter.salvarTodasAlteracoesPendentes()
            }

            if (hasUnsavedChanges) {
                Log.d("SAVE_PENDING", "Salvando alterações pendentes...")
                salvarAlteracoesNoFirestore()
            }
        } catch (e: Exception) {
            Log.e("SAVE_PENDING_ERROR", "Erro ao salvar alterações pendentes", e)
        }
    }

    private fun salvarAlteracoesNoFirestore() {
        if (fichasList.isNotEmpty()) {
            val ficha = fichasList.first()
            salvarFichaAlteradaNoFirestore(ficha)
        }
    }

    // MÉTODO PÚBLICO: Para forçar recarregamento quando necessário
    fun recarregarFicha() {
        Log.d("PUBLIC_RELOAD", "Método público de recarregamento chamado")
        recarregarDadosDoFirestore()
    }

    companion object {
        private const val REQUEST_CODE_ADD_EXERCICIO = 1001
    }
}





*/ //51651651
