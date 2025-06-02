package com.example.unipump

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.unipump.models.FichaTreinoAluno
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class TelaPrincipalAluno : BaseActivity() {

    private lateinit var imgAvatar: ImageView
    private lateinit var nomeUser: TextView
    private lateinit var btnVisualizar: Button
    private lateinit var linkRelatorio: TextView
    private lateinit var btnNotificacao: ImageButton

    private lateinit var tvFichaCard: TextView
    private lateinit var tvSequencia: TextView
    private lateinit var tvRecorde: TextView

    private lateinit var diasSemana: List<TextView>

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_principal_aluno)

        // --- bind de views ---
        imgAvatar      = findViewById(R.id.imgAvatar)
        nomeUser       = findViewById(R.id.nomeUser)
        btnVisualizar  = findViewById(R.id.btn_visualizar)
        linkRelatorio  = findViewById(R.id.link_relatorio)
        btnNotificacao = findViewById(R.id.btn_notificacao)
        tvFichaCard    = findViewById(R.id.text_ficha)
        tvSequencia    = findViewById(R.id.tvSequencia)
        tvRecorde      = findViewById(R.id.Recorde)

        diasSemana = listOf(
            findViewById(R.id.tvDia1),
            findViewById(R.id.tvDia2),
            findViewById(R.id.tvDia3),
            findViewById(R.id.tvDia4),
            findViewById(R.id.tvDia5),
            findViewById(R.id.tvDia6),
            findViewById(R.id.tvDia7)
        )

        // --- Saudação e foto de perfil ---
        carregarPerfil()

        // --- Botões ---
        btnVisualizar.setOnClickListener { abrirFichaSugerida() }
        linkRelatorio.setOnClickListener {
            startActivity(Intent(this, TelaRelatorioSemanal::class.java))
        }
        btnNotificacao.setOnClickListener {
            startActivity(Intent(this, TelaNotificacaoAluno::class.java))
        }

        findViewById<BottomNavigationView>(R.id.bottom_navigation)
            .setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_treinos ->
                        startActivity(Intent(this, TelaTreinoAluno::class.java)).run { true }
                    R.id.nav_config ->
                        startActivity(Intent(this, TelaConfig::class.java)).run { true }
                    R.id.nav_chat -> {
                        Toast.makeText(this, "Indo para o chat...", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, TelaChat::class.java))
                        true
                    }
                    else -> true
                }
            }

        // --- Inicialização dos dados ---
        carregarSequencia()
        loadFichaSugerida()
        carregarPresencaSemanal()
    }

    override fun onResume() {
        super.onResume()
        carregarPerfil()
        carregarSequencia()
        loadFichaSugerida()
        carregarPresencaSemanal()
    }

    /** Carrega nome e avatar via Glide, com circleCrop */
    private fun carregarPerfil() {
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val uid = prefs.getString("alunoDocId", null) ?: return


        val nome = prefs.getString("nome", "Usuario") ?: "Usuario"
        val nomeUsuario = prefs.getString("nome_usuario", "") ?: ""

        // Prioridade: nome_usuario se não estiver vazio, senão nome
        nomeUser.text = if (nomeUsuario.isNotBlank()) {
            "Olá, $nomeUsuario!"
        } else {
            "Olá, $nome!"
        }


        db.collection("alunos").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val path = doc.getString("uri_foto")
                if (!path.isNullOrBlank()) {
                    // path local no storage interno
                    Glide.with(this)
                        .load(Uri.parse(path))
                        .circleCrop()
                        .skipMemoryCache(true)
                        .into(imgAvatar)
                } else {
                    // placeholder padrão
                    imgAvatar.setImageResource(R.drawable.ic_person)
                }
            }
            .addOnFailureListener {
                Log.e("TelaPrincipalAluno", "falha ao carregar perfil", it)
            }
    }

    /** Carrega a ficha sugerida com base em suggestionIndex */
    private fun loadFichaSugerida() {
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val uid = prefs.getString("alunoDocId", null) ?: run {
            tvFichaCard.text = "Aluno não encontrado"
            return
        }
        val alunoRef = db.collection("alunos").document(uid)

        // 1) Lê suggestionIndex no documento do aluno
        alunoRef.get().addOnSuccessListener { alunoSnap ->
            val idx = (alunoSnap.getLong("suggestionIndex") ?: 0L).toInt()

            // 2) Busca e ordena todas as fichas (coleção "treino")
            db.collection("alunos").document(uid)
                .collection("treino")
                .get()
                .addOnSuccessListener { snaps ->
                    val docsOrdenados = snaps.documents
                        .sortedBy { it.getString("letra")?.uppercase(Locale.getDefault()) }

                    if (docsOrdenados.isEmpty()) {
                        tvFichaCard.text = "Nenhuma ficha"
                        return@addOnSuccessListener
                    }

                    // 3) Ajusta índice seguro
                    val safeIdx = idx.coerceIn(0, docsOrdenados.lastIndex)
                    val doc = docsOrdenados[safeIdx]
                    val letra = doc.getString("letra").orEmpty()
                    val titulo = doc.getString("nome").orEmpty()
                    tvFichaCard.text = "Sugestão:\nFicha $letra: $titulo"
                }
                .addOnFailureListener { e ->
                    Log.e("TelaPrincipalAluno", "Erro ao carregar fichas", e)
                    tvFichaCard.text = "Erro ao carregar sugestão"
                }
        }.addOnFailureListener { e ->
            Log.e("TelaPrincipalAluno", "Erro ao ler suggestionIndex", e)
            tvFichaCard.text = "Erro ao carregar sugestão"
        }
    }

    /** Abre a ficha que está sugerida (com base em suggestionIndex) */
    private fun abrirFichaSugerida() {
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val uid = prefs.getString("alunoDocId", null)
        if (uid.isNullOrBlank()) {
            Toast.makeText(this, "Usuário não encontrado.", Toast.LENGTH_LONG).show()
            return
        }
        val alunoRef = db.collection("alunos").document(uid)

        // 1) Lê suggestionIndex
        alunoRef.get().addOnSuccessListener { alunoSnap ->
            val idx = (alunoSnap.getLong("suggestionIndex") ?: 0L).toInt()

            // 2) Busca fichas e ordena
            db.collection("alunos").document(uid)
                .collection("treino")
                .get()
                .addOnSuccessListener { snaps ->
                    val docsOrdenados = snaps.documents
                        .sortedBy { it.getString("letra")?.uppercase(Locale.getDefault()) }

                    if (docsOrdenados.isEmpty()) {
                        Toast.makeText(this, "Nenhuma ficha disponível", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val safeIdx = idx.coerceIn(0, docsOrdenados.lastIndex)
                    val doc = docsOrdenados[safeIdx]

                    // 3) Abre TelaFichaTreinoAluno com docId / letra / título
                    Intent(this, TelaFichaTreinoAluno::class.java).apply {
                        putExtra("docIdTreino", doc.id)
                        putExtra("letra", doc.getString("letra").orEmpty())
                        putExtra("titulo", doc.getString("nome").orEmpty())
                        startActivity(this)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("TelaPrincipalAluno", "Erro ao buscar fichas", e)
                    Toast.makeText(this, "Erro ao carregar ficha", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener { e ->
            Log.e("TelaPrincipalAluno", "Erro ao ler suggestionIndex", e)
            Toast.makeText(this, "Erro ao carregar sugestão", Toast.LENGTH_SHORT).show()
        }
    }

    /** Carrega sequência e recorde do aluno */
    private fun carregarSequencia() {
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val uid = prefs.getString("alunoDocId", null) ?: return

        db.collection("alunos").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val seq = doc.getLong("sequenciaDias") ?: 0L
                val rec = doc.getLong("recordeDias")   ?: 0L
                tvSequencia.text = "$seq 🔥"
                tvRecorde.text   = "$rec dias"
            }
            .addOnFailureListener {
                Log.e("TelaPrincipalAluno", "erro ao ler sequência", it)
                tvSequencia.text = "0 🔥"
                tvRecorde.text   = "0 dias"
            }
    }

    /**
     * Marca cada dia da semana verde/vermelho com base em datasSemana.
     * Usa comparação de semana do ano para garantir sexta não vire sábado.
     */
    private fun carregarPresencaSemanal() {
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val uid = prefs.getString("alunoDocId", null) ?: return
        val alunoRef = db.collection("alunos").document(uid)

        alunoRef.get()
            .addOnSuccessListener { doc ->
                // 1) Cria "hoje" usando explicitamente o fuso do Brasil (Fortaleza)
                val tzBR = TimeZone.getTimeZone("America/Fortaleza")
                val hojeCal = Calendar.getInstance(tzBR).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val semanaHoje = hojeCal.get(Calendar.WEEK_OF_YEAR)
                val anoHoje = hojeCal.get(Calendar.YEAR)

                // 2) Lê lista de timestamps
                val rawList = doc.get("datasSemana") as? List<*>
                val timestamps = rawList
                    ?.filterIsInstance<Timestamp>()
                    ?: emptyList()

                // 3) Para cada timestamp, converte no fuso certo e valida semana/ano e pega dia da semana
                val feitos = timestamps.mapNotNull { ts ->
                    val d = ts.toDate()  // Date com instante UTC, mas abaixo converto para fuso BR
                    val calTs = Calendar.getInstance(tzBR).apply {
                        time = d
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val semanaTs = calTs.get(Calendar.WEEK_OF_YEAR)
                    val anoTs = calTs.get(Calendar.YEAR)
                    if (semanaTs == semanaHoje && anoTs == anoHoje) {
                        calTs.get(Calendar.DAY_OF_WEEK) // 1=domingo,...7=sábado
                    } else null
                }.toSet()

                // 4) Pinta cada TextView: índice 0->domingo (Calendar.SUNDAY), ..., índice6->sábado
                diasSemana.forEachIndexed { idx, tv ->
                    val diaCal = Calendar.SUNDAY + idx // 1..7
                    val corRes = if (feitos.contains(diaCal))
                        R.color.green else R.color.red
                    tv.setTextColor(resources.getColor(corRes, null))
                }
            }
            .addOnFailureListener {
                Log.e("TelaPrincipalAluno", "erro ao carregar presenca", it)
            }
    }
}
