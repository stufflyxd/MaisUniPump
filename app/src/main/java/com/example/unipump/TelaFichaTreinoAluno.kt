package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
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

    // Estes valores serão definidos em onCreate()
    private lateinit var alunoDocId: String
    private val nomeSubColecao = "treino"

    // Guarda o ID do documento de treino selecionado
    private var docIdTreino: String? = null

    // Lista mutável de exercícios (cada ExercicioAluno contém sua lista de Séries)
    private val listaExercicioAlunos = mutableListOf<ExercicioAluno>()
    private lateinit var exercAdapter: ExerciciosAdapterAluno

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_ficha_treino)

        // 1) Carrega o alunoDocId dos SharedPreferences
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val maybeId = prefs.getString("alunoDocId", null)
        if (maybeId.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Identificador do aluno não encontrado. Faça login novamente.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }
        alunoDocId = maybeId

        // 2) Configura RecyclerView + Adapter
        val rv = findViewById<RecyclerView>(R.id.rvExercicios).apply {
            layoutManager = LinearLayoutManager(this@TelaFichaTreinoAluno)
        }
        exercAdapter = ExerciciosAdapterAluno(listaExercicioAlunos)
        rv.adapter = exercAdapter

        // 3) Botão Voltar: salva e fecha
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            salvarTreino { sucesso ->
                if (sucesso) finish()
                else Toast.makeText(this, "Falha ao salvar treino", Toast.LENGTH_SHORT).show()
            }
        }

        // 4) Botão Iniciar: salva e abre feedback
        findViewById<Button>(R.id.buttonStart).setOnClickListener {
            salvarTreino { sucesso ->
                if (sucesso) {
                    startActivity(Intent(this, TelaExercicioFinalizadoAluno::class.java))
                } else {
                    Toast.makeText(this, "Falha ao salvar treino", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 5) Bottom Navigation
        findViewById<BottomNavigationView>(R.id.bottom_navigation)
            .setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_inicio  -> true
                    R.id.nav_treinos -> {
                        startActivity(Intent(this, TelaTreinoAluno::class.java))
                        true
                    }
                    R.id.nav_chat    -> {
                        startActivity(Intent(this, TelaChat::class.java))
                        true
                    }
                    R.id.nav_config  -> {
                        startActivity(Intent(this, TelaConfig::class.java))
                        true
                    }
                    else             -> false
                }
            }

        // 6) Recupera a 'letra' que veio na Intent
        val letraTreino = intent.getStringExtra("letra") ?: ""
        if (letraTreino.isBlank()) {
            Toast.makeText(this, "Letra do treino não informada", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 7) Query no Firestore filtrando pela letra
        db.collection("alunos")
            .document(alunoDocId)
            .collection(nomeSubColecao)
            .whereEqualTo("letra", letraTreino)
            .get()
            .addOnSuccessListener { snaps ->
                val doc = snaps.documents.firstOrNull()
                if (doc == null) {
                    Toast.makeText(
                        this,
                        "Nenhum treino “$letraTreino” encontrado",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                // Guarda para usar no update
                docIdTreino = doc.id

                // Extrai lista bruta de exercícios
                val rawList = doc.get("exercicios") as? List<Map<String,Any>> ?: emptyList()

                // Constrói lista de ExercicioAluno + Series
                val exercList = rawList.map { m ->
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
                }

                // Atualiza adapter
                listaExercicioAlunos.clear()
                listaExercicioAlunos.addAll(exercList)
                exercAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Erro ao carregar treino: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun salvarTreino(onResult: (Boolean) -> Unit) {
        val docId = docIdTreino
        if (docId.isNullOrEmpty()) {
            onResult(false)
            return
        }

        // Reconstrói o payload com os valores editados
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
                        "descanso" to s.descanso
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
