package com.example.unipump

import FichaTreinoAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.unipump.models.FichaTreino
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class GerenciamentoDoAluno_Funcionario : BaseActivity(),
    FichaTreinoAdapter.OnFichaTreinoClickListener {


    private lateinit var profileImage: ImageView
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

        Log.d("GERENCIAMENTO_LIFECYCLE", "onCreate iniciado")

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

        Log.d("GERENCIAMENTO_LIFECYCLE", "onCreate concluído")
    }

    override fun onResume() {
        super.onResume()
        Log.d("GERENCIAMENTO_LIFECYCLE", "onResume - Recarregando fichas do Firestore")

        // Verificar se o adapter ainda está válido
        if (::adapter.isInitialized) {
            Log.d("GERENCIAMENTO_LIFECYCLE", "Adapter está inicializado, recarregando dados")
            carregarFichasDoFirestore()
        } else {
            Log.w("GERENCIAMENTO_LIFECYCLE", "Adapter não inicializado, reconfigurando RecyclerView")
            setupRecyclerView()
            carregarFichasDoFirestore()
        }
    }

    private fun configurarDadosUsuario() {
        val db = FirebaseFirestore.getInstance()

        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val nome = prefs.getString("nome", "")
        val sobrenome = prefs.getString("sobrenome", "")

        titulo.text = "Sobre $nome"
        tvNome.text = "$nome"
        tvSobrenome.text = "$sobrenome"


        val alunoDocId = prefs.getString("alunoDocId", null)

        if (alunoDocId == null) {
            Log.e("FUNCIONARIO_CONFIG", "ID do funcionário não encontrado")
            profileImage.setImageResource(R.drawable.ic_person)
            return
        }

        Log.d("FUNCIONARIO_CONFIG", "Carregando dados do funcionário: $alunoDocId")

        db.collection("alunos").document(alunoDocId)
            .get()
            .addOnSuccessListener { doc ->
                Log.d("FUNCIONARIO_CONFIG", "Documento encontrado: ${doc.exists()}")

                if (doc.exists()) {
                    // Tentar carregar foto local
                    val path = doc.getString("uri_foto")
                    Log.d("FUNCIONARIO_CONFIG", "Caminho da foto: $path")

                    if (!path.isNullOrBlank()) {
                        val file = File(path)
                        Log.d("FUNCIONARIO_CONFIG", "Arquivo existe: ${file.exists()}")

                        if (file.exists()) {
                            Glide.with(this)
                                .load(file)
                                .circleCrop()
                                .skipMemoryCache(true)
                                .into(profileImage)
                            Log.d("FUNCIONARIO_CONFIG", "Foto carregada com sucesso")
                        } else {
                            profileImage.setImageResource(R.drawable.ic_person)
                            Log.w("FUNCIONARIO_CONFIG", "Arquivo não encontrado: $path")
                        }
                    } else {
                        profileImage.setImageResource(R.drawable.ic_person)
                        Log.d("FUNCIONARIO_CONFIG", "Nenhuma foto salva")
                    }
                } else {
                    profileImage.setImageResource(R.drawable.ic_person)
                    Log.w("FUNCIONARIO_CONFIG", "Documento não encontrado no Firestore")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FUNCIONARIO_CONFIG", "Erro ao carregar perfil", exception)
                profileImage.setImageResource(R.drawable.ic_person)
            }
    }

    private fun recuperarIdAluno() {
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        alunoDocId = prefs.getString("alunoDocId", "") ?: ""

        Log.d("GERENCIAMENTO_ALUNO", "Tentando recuperar ID do aluno...")
        Log.d("GERENCIAMENTO_ALUNO", "ID recuperado: '$alunoDocId'")

        if (alunoDocId.isEmpty()) {
            Toast.makeText(this, "Erro: ID do aluno não encontrado", Toast.LENGTH_SHORT).show()
            Log.e("GERENCIAMENTO_ALUNO", "ID do aluno não encontrado no SharedPreferences")
            finish()
        } else {
            Log.d("GERENCIAMENTO_ALUNO", "✅ ID do aluno recuperado com sucesso: $alunoDocId")
        }
    }

    private fun initViews() {
        try {
            profileImage = findViewById(R.id.profileImage)
            linkMaisDetalhes = findViewById(R.id.link_mais_detalhes)
            linkAdicionar = findViewById(R.id.link_adicionar)
            btnSetaVoltar = findViewById(R.id.SetaVoltarTelaGerenciamentoAluno)
            btnNavegacao = findViewById(R.id.bottom_navigation)
            rvFichasTreino = findViewById(R.id.rvFichasTreino)
            titulo = findViewById(R.id.titulo)
            tvNome = findViewById(R.id.tvNome)
            tvSobrenome = findViewById(R.id.tvSobrenome)

            Log.d("GERENCIAMENTO_ALUNO", "✅ Views inicializadas com sucesso")
        } catch (e: Exception) {
            Log.e("GERENCIAMENTO_ALUNO", "❌ Erro ao inicializar views", e)
            throw e
        }
    }



    private fun setupRecyclerView() {
        try {
            Log.d("RECYCLERVIEW_SETUP", "Configurando RecyclerView...")

            // Criar novo adapter
            adapter = FichaTreinoAdapter(fichasTreino, this)

            rvFichasTreino.apply {
                layoutManager = LinearLayoutManager(this@GerenciamentoDoAluno_Funcionario)
                adapter = this@GerenciamentoDoAluno_Funcionario.adapter
                setHasFixedSize(true)
            }

            Log.d("RECYCLERVIEW_SETUP", "✅ RecyclerView configurado com sucesso")
            Log.d("RECYCLERVIEW_SETUP", "Adapter: ${adapter.javaClass.simpleName}")
            Log.d("RECYCLERVIEW_SETUP", "Listener: ${this.javaClass.simpleName}")

        } catch (e: Exception) {
            Log.e("RECYCLERVIEW_SETUP", "❌ Erro ao configurar RecyclerView", e)
            throw e
        }
    }

    private fun carregarFichasDoFirestore() {
        Log.d("FIRESTORE_LOAD", "=== INICIANDO CARREGAMENTO DAS FICHAS ===")
        Log.d("FIRESTORE_LOAD", "Aluno ID: '$alunoDocId'")

        if (alunoDocId.isEmpty()) {
            Log.e("FIRESTORE_LOAD", "❌ ID do aluno está vazio!")
            Toast.makeText(this, "ID do aluno não disponível", Toast.LENGTH_SHORT).show()
            return
        }

        val tamanhoAnterior = fichasTreino.size
        fichasTreino.clear()
        Log.d("FIRESTORE_LOAD", "Lista limpa. Tamanho anterior: $tamanhoAnterior")

        // Notificar que a lista foi limpa
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }

        db.collection("alunos")
            .document(alunoDocId)
            .collection("treino")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("FIRESTORE_LOAD", "✅ Consulta bem-sucedida")
                Log.d("FIRESTORE_LOAD", "Documentos encontrados: ${documents.size()}")

                try {
                    if (documents.isEmpty) {
                        Log.d("FIRESTORE_LOAD", "Nenhuma ficha de treino encontrada")
                        runOnUiThread {
                            if (::adapter.isInitialized) {
                                adapter.notifyDataSetChanged()
                            }
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

                            // VALIDAÇÃO: Verificar se dados essenciais estão presentes
                            if (documentId.isEmpty()) {
                                Log.w("FIRESTORE_LOAD", "Document ID vazio, pulando ficha")
                                continue
                            }

                            val fichaTreino = FichaTreino(
                                documentId = documentId,
                                letra = letra,
                                nome = nome,
                                descricao = descricao,
                                quantidadeExercicios = quantidadeExercicios
                            )

                            novasFichas.add(fichaTreino)

                            Log.d("FIRESTORE_LOAD", "✅ Ficha processada:")
                            Log.d("FIRESTORE_LOAD", "  - DocID: '$documentId'")
                            Log.d("FIRESTORE_LOAD", "  - Letra: '$letra'")
                            Log.d("FIRESTORE_LOAD", "  - Nome: '$nome'")
                            Log.d("FIRESTORE_LOAD", "  - Exercícios: $quantidadeExercicios")

                        } catch (e: Exception) {
                            Log.e("FIRESTORE_LOAD", "❌ Erro ao processar documento ${document.id}", e)
                        }
                    }

                    // Ordenar por letra
                    novasFichas.sortBy { it.letra }
                    Log.d("FIRESTORE_LOAD", "Fichas ordenadas. Total: ${novasFichas.size}")

                    // Atualizar UI na thread principal
                    runOnUiThread {
                        try {
                            fichasTreino.clear()
                            fichasTreino.addAll(novasFichas)

                            // Verificar se adapter ainda existe
                            if (::adapter.isInitialized) {
                                adapter.notifyDataSetChanged()
                                Log.d("FIRESTORE_LOAD", "✅ Adapter atualizado")
                            } else {
                                Log.w("FIRESTORE_LOAD", "⚠️ Adapter não inicializado, recriando...")
                                setupRecyclerView()
                            }

                            Log.d("FIRESTORE_LOAD", "=== RESUMO FINAL ===")
                            Log.d("FIRESTORE_LOAD", "Fichas na lista: ${fichasTreino.size}")
                            Log.d("FIRESTORE_LOAD", "Adapter item count: ${if (::adapter.isInitialized) adapter.itemCount else "N/A"}")

                            fichasTreino.forEachIndexed { index, ficha ->
                                Log.d("FIRESTORE_LOAD", "Ficha $index: DocID='${ficha.documentId}', ${ficha.letra} - ${ficha.nome}")
                            }

                            if (fichasTreino.isNotEmpty()) {
                                Toast.makeText(
                                    this@GerenciamentoDoAluno_Funcionario,
                                    "${fichasTreino.size} fichas carregadas",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        } catch (e: Exception) {
                            Log.e("FIRESTORE_LOAD", "❌ Erro ao atualizar UI", e)
                        }
                    }

                } catch (e: Exception) {
                    Log.e("FIRESTORE_LOAD", "❌ Erro geral ao processar fichas", e)
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
                Log.e("FIRESTORE_LOAD", "❌ Erro ao carregar fichas de treino", exception)
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
            "quantidadeExercicios" to 0,
            "exercicios" to emptyList<Map<String, Any>>() // Adicionar campo exercicios vazio
        )

        Log.d("ADICIONAR_FICHA", "Criando ficha: $novaFicha")

        db.collection("alunos")
            .document(alunoDocId)
            .collection("treino")
            .add(novaFicha)
            .addOnSuccessListener { documentReference ->
                Log.d("ADICIONAR_FICHA", "✅ Nova ficha adicionada com ID: ${documentReference.id}")
                Toast.makeText(this, "Nova ficha '$proximaLetra' adicionada com sucesso!", Toast.LENGTH_SHORT).show()

                // Recarregar as fichas para mostrar a nova
                carregarFichasDoFirestore()
            }
            .addOnFailureListener { e ->
                Log.e("ADICIONAR_FICHA", "❌ Erro ao adicionar nova ficha", e)
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

    // MÉTODO CRÍTICO: Click da ficha com logs detalhados
    override fun onFichaTreinoClick(fichaTreino: FichaTreino, position: Int) {
        Log.d("FICHA_CLICK", "=== FICHA SELECIONADA ===")
        Log.d("FICHA_CLICK", "Método onFichaTreinoClick chamado")
        Log.d("FICHA_CLICK", "Position: $position")
        Log.d("FICHA_CLICK", "Ficha: ${fichaTreino.letra} - ${fichaTreino.nome}")
        Log.d("FICHA_CLICK", "Document ID: '${fichaTreino.documentId}'")
        Log.d("FICHA_CLICK", "Aluno ID: '$alunoDocId'")

        // VALIDAÇÕES CRÍTICAS
        if (fichaTreino.documentId.isEmpty()) {
            Log.e("FICHA_CLICK", "❌ ERRO: Document ID está vazio!")
            Toast.makeText(this, "Erro: ID do documento não encontrado!", Toast.LENGTH_LONG).show()
            return
        }

        if (alunoDocId.isEmpty()) {
            Log.e("FICHA_CLICK", "❌ ERRO: Aluno ID está vazio!")
            Toast.makeText(this, "Erro: ID do aluno não encontrado!", Toast.LENGTH_LONG).show()
            return
        }

        try {
            Log.d("FICHA_CLICK", "Criando Intent para TelaEdicaoFichaTreino_funcionario...")

            val intent = Intent(this, TelaEdicaoFichaTreino_funcionario::class.java)

            // Adicionar todos os dados necessários
            intent.putExtra("alunoDocId", alunoDocId)
            intent.putExtra("documentId", fichaTreino.documentId)
            intent.putExtra("ficha_letra", fichaTreino.letra)
            intent.putExtra("ficha_nome", fichaTreino.nome)
            intent.putExtra("ficha_descricao", fichaTreino.descricao)
            intent.putExtra("ficha_quantidade_exercicios", fichaTreino.quantidadeExercicios)

            Log.d("FICHA_CLICK", "Dados do Intent:")
            Log.d("FICHA_CLICK", "  - alunoDocId: '$alunoDocId'")
            Log.d("FICHA_CLICK", "  - documentId: '${fichaTreino.documentId}'")
            Log.d("FICHA_CLICK", "  - ficha_letra: '${fichaTreino.letra}'")
            Log.d("FICHA_CLICK", "  - ficha_nome: '${fichaTreino.nome}'")

            // Iniciar activity
            startActivity(intent)
            Log.d("FICHA_CLICK", "✅ Intent enviado com sucesso!")

        } catch (e: Exception) {
            Log.e("FICHA_CLICK", "❌ Erro ao criar ou enviar Intent", e)
            Toast.makeText(this, "Erro ao abrir ficha: ${e.message}", Toast.LENGTH_LONG).show()
        }

        Log.d("FICHA_CLICK", "=== FIM DO CLICK ===")
    }

    fun forcarRecarregamento() {
        Log.d("GERENCIAMENTO_ALUNO", "Recarregamento forçado solicitado")
        carregarFichasDoFirestore()
    }
}








/*
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

class GerenciamentoDoAluno_Funcionario : ,
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
}*/