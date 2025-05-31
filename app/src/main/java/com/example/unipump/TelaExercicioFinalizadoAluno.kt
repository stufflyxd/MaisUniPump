// app/src/main/java/com/example/unipump/TelaExercicioFinalizadoAluno.kt

package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.adapters.ExerciciosAdapterFinalizadoAluno
import com.example.unipump.models.ExercicioFinalizadoAluno
import com.example.unipump.models.SerieFinalizadoAluno
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class TelaExercicioFinalizadoAluno : AppCompatActivity() {

    // 1) Registrar o launcher para receber resultado da TelaDetalheExercicio
    private val detalheLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val data = result.data!!
            val posicaoRetorno = data.getIntExtra("EXTRA_POSICAO", -1)
            val exercRetornado = data.getParcelableExtra<ExercicioFinalizadoAluno>("EXTRA_EXERCICIO_ATUALIZADO")
            if (posicaoRetorno >= 0 && exercRetornado != null) {
                // Substitui apenas o exercício na posição retornada
                listaDeExerciciosCarregados[posicaoRetorno] = exercRetornado
                rvExercicios.adapter?.notifyItemChanged(posicaoRetorno)
            }
        }
    }

    private lateinit var btnVoltar: ImageView
    private lateinit var btnFinalizar: Button
    private lateinit var rvExercicios: RecyclerView
    private lateinit var treinoDocId: String
    private val db = FirebaseFirestore.getInstance()

    // 2) A lista agora é um MutableList para permitir alterações pontuais
    private var listaDeExerciciosCarregados: MutableList<ExercicioFinalizadoAluno> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_exercicio_finalizado_aluno)

        // 3) Recupera o ID do treino passado via Intent
        treinoDocId = intent.getStringExtra("TREINO_ID")
            ?: throw IllegalArgumentException("TREINO_ID não informado")

        btnVoltar = findViewById(R.id.btnVoltar)
        btnFinalizar = findViewById(R.id.btnFinalizar)
        rvExercicios = findViewById(R.id.rvExercicios)

        btnVoltar.setOnClickListener { finish() }
        btnFinalizar.setOnClickListener { finalizarTreino() }

        rvExercicios.layoutManager = LinearLayoutManager(this)
        carregarExerciciosDoBanco()
    }

    /**
     * Carrega a lista de exercícios do documento de treino e popula o RecyclerView.
     * Também armazena a lista em variável mutável para permitir atualizações pontuais.
     */
    private fun carregarExerciciosDoBanco() {
        val uid = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            .getString("alunoDocId", null) ?: return

        db.collection("alunos")
            .document(uid)
            .collection("treino")
            .document(treinoDocId)
            .get()
            .addOnSuccessListener { snap ->
                // Extrai o campo "exercicios" que é List<Map<String,Any>>
                val raw = snap.get("exercicios") as? List<Map<String, Any>> ?: emptyList()
                val listaTemp = raw.map { m ->
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

                // Preenche o MutableList com os exercícios carregados
                listaDeExerciciosCarregados.clear()
                listaDeExerciciosCarregados.addAll(listaTemp)

                // 4) Configura o adapter passando lambdas de clique que incluem posição + objeto
                rvExercicios.adapter = ExerciciosAdapterFinalizadoAluno(
                    listaDeExerciciosCarregados
                ) { posicao, exercicio ->
                    // Monta o Intent para TelaDetalheExercicio, enviando posição e objeto
                    val intent = Intent(this, TelaDetalheExercicio::class.java).apply {
                        putExtra("EXTRA_POSICAO", posicao)
                        putExtra("EXTRA_EXERCICIO", exercicio)
                    }
                    // Lança a TelaDetalheExercicio e espera resultado
                    detalheLauncher.launch(intent)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Erro ao carregar exercícios: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /**
     * Finaliza o treino:
     * 1) Verifica se já foi finalizado hoje (campo datasSemana).
     * 2) Se não, conta quantos exercícios existem na lista carregada.
     * 3) Lê kcal e minutos do documento de treino.
     * 4) Executa transação para incrementar total_exercicios, total_kcal, total_tempo e outros campos.
     * 5) Grava timestamp de hoje em datasSemana (com fuso UTC−3).
     * 6) Avança suggestionIndex se este treino for a ficha sugerida.
     * 7) Retorna sempre para TelaPrincipalAluno.
     */
    private fun finalizarTreino() {
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val uid = prefs.getString("alunoDocId", null) ?: return
        val alunoRef = db.collection("alunos").document(uid)
        val treinoRef = alunoRef.collection("treino").document(treinoDocId)

        // Use o fuso “America/Fortaleza” para comparações de data
        val tzBR = TimeZone.getTimeZone("America/Fortaleza")
        val hojeCal = Calendar.getInstance(tzBR).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Passo A: se hoje for domingo no fuso Brasil, zera datasSemana
        if (hojeCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            alunoRef.update("datasSemana", emptyList<Timestamp>())
                .addOnFailureListener { e ->
                    Log.e("TelaExFin", "Falha ao zerar datasSemana no domingo", e)
                }
        }

        // Passo B: verifica se já finalizou hoje
        alunoRef.get().addOnSuccessListener { alunoSnap ->
            val rawList = alunoSnap.get("datasSemana") as? List<*> ?: emptyList<Any>()
            val timestamps = rawList.filterIsInstance<Timestamp>()
            val jaHoje = timestamps.any { ts ->
                val c = Calendar.getInstance(tzBR).apply {
                    time = ts.toDate()
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                c.get(Calendar.YEAR) == hojeCal.get(Calendar.YEAR) &&
                        c.get(Calendar.DAY_OF_YEAR) == hojeCal.get(Calendar.DAY_OF_YEAR)
            }
            if (jaHoje) {
                Toast.makeText(this, "Você já finalizou treino hoje.", Toast.LENGTH_SHORT).show()
                retornarTelaPrincipal()
                return@addOnSuccessListener
            }

            // Passo C: conta quantos exercícios existem na lista carregada
            val qtdExs = listaDeExerciciosCarregados.size.toLong()
            if (qtdExs <= 0L) {
                Toast.makeText(this, "Nenhum exercício para contabilizar.", Toast.LENGTH_SHORT).show()
                retornarTelaPrincipal()
                return@addOnSuccessListener
            }

            // Passo D: lê kcal e minutos do documento de treino
            treinoRef.get().addOnSuccessListener { tSnap ->
                val kcal = tSnap.getLong("kcal") ?: 0L
                val minutos = tSnap.getLong("minutos") ?: 0L

                Log.d(
                    "TelaExercicioFinalizado",
                    "Valores do treino: qtdExs=$qtdExs, kcal=$kcal, minutos=$minutos"
                )

                // Passo E: transação para incrementar totais, sequência e recorde
                db.runTransaction { tx ->
                    val aluno = tx.get(alunoRef)
                    val seqAtual = aluno.getLong("sequenciaDias") ?: 0L
                    val recAtual = aluno.getLong("recordeDias") ?: 0L

                    val novaSeq = seqAtual + 1
                    val novoRec = maxOf(recAtual, novaSeq)

                    tx.update(
                        alunoRef, mapOf(
                            "total_exercicios" to FieldValue.increment(qtdExs),
                            "total_kcal" to FieldValue.increment(kcal),
                            "total_tempo" to FieldValue.increment(minutos),
                            "sequenciaDias" to novaSeq,
                            "recordeDias" to novoRec,
                            "lastTreino" to FieldValue.serverTimestamp()
                        )
                    )
                }
                    .addOnSuccessListener {
                        Log.d(
                            "TelaExercicioFinalizado",
                            "Transação BEM-SUCEDIDA! total_exercicios +$qtdExs, total_kcal +$kcal, total_tempo +$minutos"
                        )

                        // Passo F: grava hoje em datasSemana (meia-noite local UTC−3)
                        val dataHojeZero = Calendar.getInstance(tzBR).apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val tsHoje = Timestamp(dataHojeZero.time)

                        alunoRef.update("datasSemana", FieldValue.arrayUnion(tsHoje))
                            .addOnSuccessListener {
                                // Passo G: avança suggestionIndex, se necessário
                                avancarSuggestionIndex(uid) {
                                    retornarTelaPrincipal()
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("TelaExFin", "Falha ao gravar datasSemana", e)
                                Toast.makeText(
                                    this,
                                    "Erro ao registrar dia da semana.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                retornarTelaPrincipal()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("TelaExercicioFinalizado", "Transação FALHOU: ${e.message}", e)
                        Toast.makeText(
                            this,
                            "Erro na transação: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        retornarTelaPrincipal()
                    }
            }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Erro ao ler dados do treino: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    retornarTelaPrincipal()
                }
        }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Erro ao verificar datasSemana: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                retornarTelaPrincipal()
            }
    }

    /**
     * Avança o campo suggestionIndex no documento do aluno caso este treino seja a ficha sugerida no momento.
     * Depois executa o callback, independente de atualizar ou não.
     */
    private fun avancarSuggestionIndex(uid: String, callback: () -> Unit) {
        val alunoRef = db.collection("alunos").document(uid)

        // 1) Lê o suggestionIndex atual
        alunoRef.get().addOnSuccessListener { alunoSnap ->
            val idx = (alunoSnap.getLong("suggestionIndex") ?: 0L).toInt()

            // 2) Busca todas as fichas na subcoleção "treino", ordena por letra
            db.collection("alunos").document(uid)
                .collection("treino")
                .get()
                .addOnSuccessListener { snaps ->
                    val docsOrdenados = snaps.documents
                        .sortedBy { it.getString("letra")?.uppercase(Locale.getDefault()) }

                    // 3) Se o treino finalizado é o sugerido (idx), incrementa index
                    if (idx in docsOrdenados.indices &&
                        docsOrdenados[idx].id == treinoDocId
                    ) {
                        // só incrementa se não for o último índice
                        if (idx < docsOrdenados.lastIndex) {
                            alunoRef.update("suggestionIndex", FieldValue.increment(1))
                                .addOnCompleteListener { callback() }
                                .addOnFailureListener {
                                    Log.e("TelaExFin", "Falha ao incrementar suggestionIndex", it)
                                    callback()
                                }
                        } else {
                            // já era a última ficha, não incrementa, mas executa callback
                            callback()
                        }
                    } else {
                        // não era a sugerida, apenas executa callback
                        callback()
                    }
                }
                .addOnFailureListener {
                    Log.e("TelaExFin", "Erro ao buscar fichas para suggestionIndex", it)
                    callback()
                }
        }.addOnFailureListener {
            Log.e("TelaExFin", "Erro ao ler suggestionIndex", it)
            callback()
        }
    }

    /** Redireciona o usuário para TelaPrincipalAluno e finaliza esta Activity. */
    private fun retornarTelaPrincipal() {
        startActivity(
            Intent(this, TelaPrincipalAluno::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
        finish()
    }
}
