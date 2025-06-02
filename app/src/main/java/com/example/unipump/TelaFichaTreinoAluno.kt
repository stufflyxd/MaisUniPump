package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

class TelaFichaTreinoAluno : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var alunoDocId: String
    private val nomeSubColecao = "treino"
    private var docIdTreino: String? = null

    private val listaExercicioAlunos = mutableListOf<ExercicioAluno>()
    private lateinit var exercAdapter: ExerciciosAdapterAluno
    private lateinit var rvExercicios: RecyclerView

    companion object {
        private const val TAG = "TREINO_LOG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_ficha_treino)

        // 1) Título
        val tvTitulo = findViewById<TextView>(R.id.Titulo_exercicio)
        val titulo   = intent.getStringExtra("titulo") ?: ""
        tvTitulo.text = titulo

        // 2) Lê alunoDocId do SharedPreferences
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        alunoDocId = prefs.getString("alunoDocId", null)
            ?: run {
                Toast.makeText(this, "Faça login novamente.", Toast.LENGTH_LONG).show()
                finish()
                return
            }
        Log.d(TAG, "alunoDocId obtido: $alunoDocId")

        // 3) Configura RecyclerView e Adapter
        rvExercicios = findViewById<RecyclerView>(R.id.rvExercicios).apply {
            layoutManager = LinearLayoutManager(this@TelaFichaTreinoAluno)
        }
        exercAdapter = ExerciciosAdapterAluno(listaExercicioAlunos)
        rvExercicios.adapter = exercAdapter

        // 4) Botão Voltar (salva e fecha) — limpamos foco antes de salvar
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            rvExercicios.clearFocus()
            salvarTreino { sucesso ->
                if (sucesso) {
                    Log.d(TAG, "Salvar treino (Voltar) → sucesso. Chamando finish()")
                    finish()
                } else {
                    Log.e(TAG, "Salvar treino (Voltar) → falhou.")
                    Toast.makeText(this, "Falha ao salvar treino", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 5) Botão Iniciar (salva e abre Feedback) — limpamos foco antes de salvar
        findViewById<Button>(R.id.buttonStart).setOnClickListener {
            rvExercicios.clearFocus()
            salvarTreino { sucesso ->
                if (sucesso && !docIdTreino.isNullOrEmpty()) {
                    Log.d(TAG, "Salvar treino (Iniciar) → sucesso. docIdTreino = $docIdTreino")
                    val intent = Intent(this, TelaExercicioFinalizadoAluno::class.java).apply {
                        putExtra("TREINO_ID", docIdTreino)
                        putExtra("titulo", titulo)
                    }
                    startActivity(intent)
                } else {
                    Log.e(TAG, "Salvar treino (Iniciar) → falhou ou docIdTreino está vazio.")
                    Toast.makeText(this, "Falha ao salvar treino", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 6) Navegação inferior — limpamos foco antes de salvar
        findViewById<BottomNavigationView>(R.id.bottom_navigation)
            .setOnItemSelectedListener { item ->
                rvExercicios.clearFocus()
                salvarTreino { sucesso ->
                    if (sucesso) {
                        Log.d(TAG, "Salvar treino (NavInferior) → sucesso. Navegando para item ${item.itemId}")
                        when (item.itemId) {
                            R.id.nav_inicio  -> startActivity(Intent(this, TelaPrincipalAluno::class.java))
                            R.id.nav_treinos -> startActivity(Intent(this, TelaTreinoAluno::class.java))
                            R.id.nav_chat    -> startActivity(Intent(this, TelaChat::class.java))
                            R.id.nav_config  -> startActivity(Intent(this, TelaConfig::class.java))
                        }
                    } else {
                        Log.e(TAG, "Salvar treino (NavInferior) → falhou.")
                        Toast.makeText(this, "Falha ao salvar treino", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }

        // 7) Lê letra do treino (vinda via Intent)
        val letraTreino = intent.getStringExtra("letra") ?: ""
        if (letraTreino.isBlank()) {
            Toast.makeText(this, "Letra do treino não informada", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        Log.d(TAG, "Letra do treino recebida: '$letraTreino'")

        // 8) Busca o documento de treino filtrando pela letra
        db.collection("alunos")
            .document(alunoDocId)
            .collection(nomeSubColecao)
            .whereEqualTo("letra", letraTreino)
            .get()
            .addOnSuccessListener { snaps ->
                val doc = snaps.documents.firstOrNull()
                if (doc == null) {
                    Log.w(TAG, "Nenhum documento de treino encontrado para a letra '$letraTreino'")
                    Toast.makeText(this, "Treino “$letraTreino” não encontrado", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Aqui guardamos o docId e exibimos em log/toast para você confirmar
                docIdTreino = doc.id
                Log.d(TAG, "docIdTreino encontrado: $docIdTreino")
//                Toast.makeText(this, "docIdTreino = $docIdTreino", Toast.LENGTH_SHORT).show()

                // Carrega a lista de exercícios a partir do campo “exercicios”
                val rawList = doc.get("exercicios") as? List<Map<String, Any>> ?: emptyList()
                listaExercicioAlunos.clear()
                listaExercicioAlunos.addAll(rawList.map { m ->
                    val seriesRaw = m["series"] as? List<Map<String, Any>> ?: emptyList()
                    val series = seriesRaw.map { s ->
                        val feitoBool = when (val feitoRaw = s["feito"]) {
                            is Boolean -> feitoRaw
                            is String  -> feitoRaw.toBoolean()
                            is Number  -> feitoRaw.toInt() != 0
                            else -> false
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
                Log.e(TAG, "Erro ao carregar treino: ${e.message}", e)
                Toast.makeText(this, "Erro ao carregar treino: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun salvarTreino(onResult: (Boolean) -> Unit) {
        val docId = docIdTreino
        if (docId.isNullOrEmpty()) {
            Log.e(TAG, "salvarTreino: docIdTreino está nulo ou vazio → não posso salvar")
            onResult(false)
            return
        }

        // Monta o payload que será enviado ao Firestore
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

        // Log para você ver exatamente o caminho e payload que vai para o Firestore
        Log.d(TAG, "salvarTreino() → escrevendo em: alunos/$alunoDocId/treino/$docId/exercicios")
        Log.d(TAG, "Payload raw = $payload")

        db.collection("alunos")
            .document(alunoDocId)
            .collection(nomeSubColecao)
            .document(docId)
            .update("exercicios", payload)
            .addOnSuccessListener {
                Log.d(TAG, "Firestore: campo 'exercicios' atualizado COM SUCESSO.")
                onResult(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore: falha ao atualizar campo 'exercicios'.", e)
                onResult(false)
            }
    }
}
