package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TelaPrincipalAluno : AppCompatActivity() {

    private lateinit var visualizar: Button
    private lateinit var linkRelatorio: TextView
    private lateinit var notificacao: ImageButton
    private lateinit var nomeUser: TextView

    private lateinit var tvSequencia: TextView
    private lateinit var tvRecorde: TextView

    // este TextView está dentro do card "Treino do dia"
    private lateinit var tvFichaCard: TextView

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_principal_aluno)

        // --- Bind de views ---
        nomeUser      = findViewById(R.id.nomeUser)
        visualizar    = findViewById(R.id.btn_visualizar)
        linkRelatorio = findViewById(R.id.link_relatorio)
        notificacao   = findViewById(R.id.btn_notificacao)

        tvSequencia   = findViewById(R.id.tvSequencia)
        tvRecorde     = findViewById(R.id.Recorde)
        tvFichaCard   = findViewById(R.id.text_ficha) // TextView do card de treino

        // --- Saudação ---
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val nome  = prefs.getString("nome", "Usuário") ?: "Usuário"
        nomeUser.text = "Olá, $nome!"

        // --- Ações ---
        onClickVisualizar()
        onClickRelatorio()
        onClickNotificao()

        // --- Bottom Navigation ---
        findViewById<BottomNavigationView>(R.id.bottom_navigation)
            .setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_inicio  -> true
                    R.id.nav_treinos -> {
                        startActivity(Intent(this, TelaTreinoAluno::class.java))
                        true
                    }
                    R.id.nav_config  -> {
                        startActivity(Intent(this, TelaConfig::class.java))
                        true
                    }
                    R.id.nav_chat    -> {
                        Toast.makeText(this, "Indo para o chat...", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, TelaChat::class.java))
                        true
                    }
                    else -> false
                }
            }

        // --- Inicializa dados ---
        carregarSequencia()
        loadPrimeiraFicha()  // Pega a primeira ficha e preenche o card
    }

    override fun onResume() {
        super.onResume()
        carregarSequencia()
        loadPrimeiraFicha()
    }

    /** Lê os campos sequenciaDias e recordeDias e, se passou 1 dia, zera sequenciaDias */
    private fun carregarSequencia() {
        val uid = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            .getString("alunoDocId", null) ?: return
        val alunoRef = db.collection("alunos").document(uid)

        alunoRef.get().addOnSuccessListener { doc ->
            val seq       = doc.getLong("sequenciaDias") ?: 0L
            val rec       = doc.getLong("recordeDias")   ?: 0L
            val lastStamp = doc.getTimestamp("lastTreino")

            // 1 dia atrás, sem hora
            val agora  = Timestamp.now().toDate()
            val limite = java.util.Calendar.getInstance().apply {
                time = agora
                add(java.util.Calendar.DAY_OF_YEAR, -1)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE,      0)
                set(java.util.Calendar.SECOND,      0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time

            if (lastStamp == null || lastStamp.toDate().before(limite)) {
                if (seq != 0L) alunoRef.update("sequenciaDias", 0)
                tvSequencia.text = "0 🔥"
                tvRecorde.text   = "$rec dias"
            } else {
                tvSequencia.text = "$seq 🔥"
                tvRecorde.text   = "$rec dias"
            }
        }.addOnFailureListener { e ->
            Log.e("TelaPrincipalAluno", "erro ao ler sequência", e)
        }
    }

    /**
     * Busca a **primeira** ficha da coleção "treino" do aluno,
     * exatamente na ordem do Firestore (sem orderBy),
     * e preenche o card com "Ficha <letra>" + "<título>".
     */
    private fun loadPrimeiraFicha() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            tvFichaCard.text = "Faça login"
            return
        }

        val uid = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            .getString("alunoDocId", null) ?: run {
            tvFichaCard.text = "Aluno não encontrado"
            return
        }

        db.collection("alunos")
            .document(uid)
            .collection("treino")
            .get()
            .addOnSuccessListener { snaps ->
                val doc = snaps.documents.firstOrNull()
                if (doc == null) {
                    tvFichaCard.text = "Nenhuma ficha"
                    return@addOnSuccessListener
                }
                val letra  = doc.getString("letra").orEmpty()
                val titulo = doc.getString("nome").orEmpty()
                // só mostramos a letra + título, não "Feito pelo..."
                tvFichaCard.text = "Ficha $letra\n$titulo"
            }
            .addOnFailureListener { e ->
                Log.e("TelaPrincipalAluno", "erro ao carregar ficha", e)
                tvFichaCard.text = "Erro ao carregar"
            }
    }

    private fun onClickVisualizar() {
        visualizar.setOnClickListener {
            val uid = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
                .getString("alunoDocId", null)
            if (uid.isNullOrBlank()) {
                Toast.makeText(this, "Usuário não encontrado.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            db.collection("alunos")
                .document(uid)
                .collection("treino")
                .get()
                .addOnSuccessListener { snaps ->
                    val doc = snaps.documents.firstOrNull()
                    if (doc == null) {
                        Toast.makeText(this, "Nenhuma ficha encontrada", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }
                    val docId  = doc.id
                    val letra  = doc.getString("letra").orEmpty()
                    val titulo = doc.getString("nome").orEmpty()

                    Intent(this, TelaFichaTreinoAluno::class.java).apply {
                        putExtra("docIdTreino", docId)
                        putExtra("letra", letra)
                        putExtra("titulo", titulo)
                        startActivity(this)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("TelaPrincipalAluno", "erro ao buscar ficha", e)
                    Toast.makeText(this, "Falha ao carregar ficha", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun onClickRelatorio() {
        linkRelatorio.setOnClickListener {
            startActivity(Intent(this, TelaRelatorioSemanal::class.java))
        }
    }

    private fun onClickNotificao() {
        notificacao.setOnClickListener {
            startActivity(Intent(this, TelaNotificacao_funcionario::class.java))
        }
    }
}
