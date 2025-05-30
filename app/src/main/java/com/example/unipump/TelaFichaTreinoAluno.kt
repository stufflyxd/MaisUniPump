package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.adapters.ExerciciosAdapterAluno
import com.example.unipump.models.ExercicioAluno
import com.example.unipump.models.SerieAluno
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class TelaFichaTreinoAluno : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var alunoDocId: String
    private val nomeSubColecao = "treino"
    private var docIdTreino: String? = null

    private val listaExercicioAlunos = mutableListOf<ExercicioAluno>()
    private lateinit var exercAdapter: ExerciciosAdapterAluno

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_ficha_treino)

        // Título
        val tvTitulo = findViewById<TextView>(R.id.Titulo_exercicio)
        val titulo   = intent.getStringExtra("titulo") ?: ""
        tvTitulo.text = titulo

        // Lê alunoDocId
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        alunoDocId = prefs.getString("alunoDocId", null)
            ?: run {
                Toast.makeText(this, "Faça login novamente.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

        // Configura RecyclerView
        val rv = findViewById<RecyclerView>(R.id.rvExercicios).apply {
            layoutManager = LinearLayoutManager(this@TelaFichaTreinoAluno)
        }
        exercAdapter = ExerciciosAdapterAluno(listaExercicioAlunos)
        rv.adapter = exercAdapter

        // Botão Voltar (salva e fecha)
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            salvarTreino { sucesso ->
                if (sucesso) finish()
                else Toast.makeText(this, "Falha ao salvar treino", Toast.LENGTH_SHORT).show()
            }
        }

        // Botão Iniciar (salva e abre Feedback)
        findViewById<Button>(R.id.buttonStart).setOnClickListener {
            salvarTreino { sucesso ->
                if (sucesso && !docIdTreino.isNullOrEmpty()) {
                    val intent = Intent(this, TelaExercicioFinalizadoAluno::class.java).apply {
                        putExtra("TREINO_ID", docIdTreino)
                        putExtra("titulo", titulo)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Falha ao salvar treino", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Navegação inferior
        findViewById<BottomNavigationView>(R.id.bottom_navigation)
            .setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_inicio  -> { startActivity(Intent(this, TelaPrincipalAluno::class.java)); true }
                    R.id.nav_treinos -> { startActivity(Intent(this, TelaTreinoAluno::class.java)); true }
                    R.id.nav_chat    -> { startActivity(Intent(this, TelaChat::class.java));      true }
                    R.id.nav_config  -> { startActivity(Intent(this, TelaConfig::class.java));    true }
                    else             -> false
                }
            }

        // Lê letra do treino
        val letraTreino = intent.getStringExtra("letra") ?: ""
        if (letraTreino.isBlank()) {
            Toast.makeText(this, "Letra do treino não informada", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Busca o documento de treino filtrando pela letra
        db.collection("alunos")
            .document(alunoDocId)
            .collection(nomeSubColecao)
            .whereEqualTo("letra", letraTreino)
            .get()
            .addOnSuccessListener { snaps ->
                val doc = snaps.documents.firstOrNull()
                if (doc == null) {
                    Toast.makeText(this, "Treino “$letraTreino” não encontrado", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                docIdTreino = doc.id

                val rawList = doc.get("exercicios") as? List<Map<String,Any>> ?: emptyList()
                listaExercicioAlunos.clear()
                listaExercicioAlunos.addAll(rawList.map { m ->
                    val seriesRaw = m["series"] as? List<Map<String,Any>> ?: emptyList()
                    val series = seriesRaw.map { s ->
                        val feitoBool = when (val feitoRaw = s["feito"]) {
                            is Boolean -> feitoRaw
                            is String  -> feitoRaw.toBoolean()
                            is Number  -> feitoRaw.toInt() != 0
                            else       -> false
                        }
                        SerieAluno(
                            ordem    = s["ordem"]?.toString().orEmpty(),
                            reps     = s["reps"]?.toString().orEmpty(),
                            peso     = s["peso"]?.toString().orEmpty(),
                            descanso = s["descanso"]?.toString().orEmpty(),
                            feito    = feitoBool,
                            duracao  = s["duracao"]?.toString().orEmpty()
                        )
                    }
                    ExercicioAluno(
                        frame    = m["frame"]?.toString().orEmpty(),
                        execucao = m["execucao"]?.toString().orEmpty(),
                        nome     = m["nome"]?.toString().orEmpty(),
                        series   = series
                    )
                })
                exercAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar treino: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun salvarTreino(onResult: (Boolean) -> Unit) {
        val docId = docIdTreino
        if (docId.isNullOrEmpty()) {
            onResult(false); return
        }
        val payload = listaExercicioAlunos.map { ex ->
            mapOf(
                "frame"    to ex.frame,
                "execucao" to ex.execucao,
                "nome"     to ex.nome,
                "series"   to ex.series.map { s ->
                    mapOf(
                        "ordem"    to s.ordem,
                        "reps"     to s.reps,
                        "peso"     to s.peso,
                        "descanso" to s.descanso,
                        "feito"    to s.feito,
                        "duracao"  to s.duracao
                    )
                }
            )
        }
        db.collection("alunos")
            .document(alunoDocId)
            .collection(nomeSubColecao)
            .document(docId)
            .update("exercicios", payload)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}
