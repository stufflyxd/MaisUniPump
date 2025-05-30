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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class TelaPrincipalAluno : AppCompatActivity() {

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
        btnVisualizar.setOnClickListener { abrirFicha() }
        linkRelatorio.setOnClickListener {
            startActivity(Intent(this, TelaRelatorioSemanal::class.java))
        }
        btnNotificacao.setOnClickListener {
            startActivity(Intent(this, TelaNotificacao_funcionario::class.java))
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
        loadPrimeiraFicha()
        carregarPresencaSemanal()
    }

    override fun onResume() {
        super.onResume()
        carregarPerfil()
        carregarSequencia()
        loadPrimeiraFicha()
        carregarPresencaSemanal()
    }

    /** Carrega nome e avatar via Glide, com circleCrop */
    private fun carregarPerfil() {
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val uid = prefs.getString("alunoDocId", null) ?: return
        val nome = prefs.getString("nome", "Usuário") ?: "Usuário"
        nomeUser.text = "Olá, $nome!"

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

    private fun loadPrimeiraFicha() {
        val user = auth.currentUser ?: run {
            tvFichaCard.text = "Faça login"
            return
        }
        val uid = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            .getString("alunoDocId", null) ?: run {
            tvFichaCard.text = "Aluno não encontrado"
            return
        }

        db.collection("alunos").document(uid)
            .collection("treino")
            .get()
            .addOnSuccessListener { snaps ->
                val doc = snaps.documents.firstOrNull()
                tvFichaCard.text = if (doc == null) {
                    "Nenhuma ficha"
                } else {
                    val letra  = doc.getString("letra").orEmpty()
                    val titulo = doc.getString("nome").orEmpty()
                    "Ficha $letra\n$titulo"
                }
            }
            .addOnFailureListener {
                Log.e("TelaPrincipalAluno", "erro ao carregar ficha", it)
                tvFichaCard.text = "Erro ao carregar"
            }
    }

    private fun abrirFicha() {
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val uid = prefs.getString("alunoDocId", null)
        if (uid.isNullOrBlank()) {
            Toast.makeText(this, "Usuário não encontrado.", Toast.LENGTH_LONG).show()
            return
        }

        db.collection("alunos").document(uid)
            .collection("treino")
            .get()
            .addOnSuccessListener { snaps ->
                val doc = snaps.documents.firstOrNull()
                if (doc == null) {
                    Toast.makeText(this, "Nenhuma ficha encontrada", Toast.LENGTH_LONG).show()
                } else {
                    Intent(this, TelaFichaTreinoAluno::class.java).apply {
                        putExtra("docIdTreino", doc.id)
                        putExtra("letra", doc.getString("letra").orEmpty())
                        putExtra("titulo", doc.getString("nome").orEmpty())
                        startActivity(this)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Falha ao carregar ficha", Toast.LENGTH_LONG).show()
            }
    }

    private fun carregarSequencia() {
        val uid = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            .getString("alunoDocId", null) ?: return

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

    private fun carregarPresencaSemanal() {
        val uid = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            .getString("alunoDocId", null) ?: return

        val inicioCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fimCal = Calendar.getInstance().apply {
            time = inicioCal.time
            add(Calendar.DAY_OF_YEAR, 7)
        }

        db.collection("alunos").document(uid)
            .collection("presenca")
            .whereGreaterThanOrEqualTo("data", Timestamp(inicioCal.time))
            .whereLessThan("data", Timestamp(fimCal.time))
            .get()
            .addOnSuccessListener { snaps ->
                val feitos = snaps.documents.mapNotNull { d ->
                    d.getTimestamp("data")?.toDate()?.let { dt ->
                        Calendar.getInstance().apply { time = dt }
                            .get(Calendar.DAY_OF_WEEK)
                    }
                }.toSet()

                diasSemana.forEachIndexed { idx, tv ->
                    val diaCal = Calendar.SUNDAY + idx
                    val corRes = if (feitos.contains(diaCal)) R.color.green else R.color.red
                    tv.setTextColor(resources.getColor(corRes, null))
                }
            }
            .addOnFailureListener {
                Log.e("TelaPrincipalAluno", "erro ao carregar presenca", it)
            }
    }
}
