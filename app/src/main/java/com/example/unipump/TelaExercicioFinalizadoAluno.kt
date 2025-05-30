package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.adapters.ExerciciosAdapterFinalizadoAluno
import com.example.unipump.models.ExercicioFinalizadoAluno
import com.example.unipump.models.SerieFinalizadoAluno
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class TelaExercicioFinalizadoAluno : AppCompatActivity() {

    private lateinit var btnVoltar: ImageView
    private lateinit var btnFinalizar: Button
    private lateinit var rvExercicios: RecyclerView
    private lateinit var treinoDocId: String
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_exercicio_finalizado_aluno)

        treinoDocId  = intent.getStringExtra("TREINO_ID")
            ?: throw IllegalArgumentException("TREINO_ID não informado")

        btnVoltar    = findViewById(R.id.btnVoltar)
        btnFinalizar = findViewById(R.id.btnFinalizar)
        rvExercicios = findViewById(R.id.rvExercicios)

        btnVoltar.setOnClickListener { finish() }
        btnFinalizar.setOnClickListener { finalizarTreino() }

        rvExercicios.layoutManager = LinearLayoutManager(this)
        carregarExerciciosDoBanco()
    }

    private fun carregarExerciciosDoBanco() {
        val uid = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            .getString("alunoDocId", null) ?: return

        db.collection("alunos")
            .document(uid)
            .collection("treino")
            .document(treinoDocId)
            .get()
            .addOnSuccessListener { snap ->
                val raw = snap.get("exercicios") as? List<Map<String, Any>> ?: emptyList()
                val lista = raw.map { m ->
                    val seriesRaw = m["series"] as? List<Map<String, Any>> ?: emptyList()
                    val series = seriesRaw.map { s ->
                        SerieFinalizadoAluno(
                            ordem    = s["ordem"]?.toString().orEmpty(),
                            reps     = s["reps"]?.toString().orEmpty(),
                            peso     = s["peso"]?.toString().orEmpty(),
                            descanso = s["descanso"]?.toString().orEmpty(),
                            feito    = s["feito"] as? Boolean ?: false,
                            duracao  = s["duracao"]?.toString().orEmpty()
                        )
                    }
                    ExercicioFinalizadoAluno(
                        frame    = m["frame"]?.toString().orEmpty(),
                        execucao = m["execucao"]?.toString().orEmpty(),
                        nome     = m["nome"]?.toString().orEmpty(),
                        series   = series
                    )
                }
                rvExercicios.adapter =
                    ExerciciosAdapterFinalizadoAluno(lista) { /* clique nas séries */ }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar exercícios: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun finalizarTreino() {
        val prefs     = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val uid       = prefs.getString("alunoDocId", null) ?: return
        val alunoRef  = db.collection("alunos").document(uid)
        val treinoRef = alunoRef.collection("treino").document(treinoDocId)

        // 1) Lê os dados do treino para extrair qtdExs, kcal e minutos
        treinoRef.get().addOnSuccessListener { tSnap ->
            val rawExs  = tSnap.get("exercicios") as? List<*> ?: emptyList<Any>()
            val qtdExs  = rawExs.size.toLong()
            val kcal    = tSnap.getLong("kcal")    ?: 0L
            val minutos = tSnap.getLong("minutos") ?: 0L

            // 2) Executa uma TRANSACTION para atualizar totais, sequência e recorde de uma vez
            db.runTransaction { transaction ->
                // 2.1) Lê o documento do aluno dentro da transação
                val alunoSnap = transaction.get(alunoRef)
                val seqAtual  = alunoSnap.getLong("sequenciaDias") ?: 0L
                val recAtual  = alunoSnap.getLong("recordeDias")   ?: 0L

                val novaSeq   = seqAtual + 1
                val novoRec   = maxOf(recAtual, novaSeq)

                // 2.2) Prepara o mapa de updates
                val updates = mapOf(
                    "total_exercicios" to FieldValue.increment(qtdExs),
                    "total_kcal"       to FieldValue.increment(kcal),
                    "total_tempo"      to FieldValue.increment(minutos),
                    "sequenciaDias"    to novaSeq,      // já setamos o novo valor
                    "recordeDias"      to novoRec,      // forçamos o maior entre old e new
                    "lastTreino"       to FieldValue.serverTimestamp()
                )

                // 2.3) Aplica tudo em alunoRef
                transaction.update(alunoRef, updates)
            }.addOnSuccessListener {
                // 3) (Opcional) registra em 'presenca' fora da transação
                alunoRef.collection("presenca")
                    .add(
                        mapOf(
                            "data"          to FieldValue.serverTimestamp(),
                            "qtdExercicios" to qtdExs,
                            "kcal"          to kcal,
                            "minutos"       to minutos
                        )
                    )

                Toast.makeText(this, "Treino finalizado! 🎉", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, TelaPrincipalAluno::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                })
                finish()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Erro na transação: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Erro ao ler treino: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
