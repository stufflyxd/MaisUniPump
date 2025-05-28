package com.example.unipump

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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class TelaExercicioFinalizadoAluno : AppCompatActivity() {

    private lateinit var btnVoltar: ImageView
    private lateinit var btnFinalizar: Button
    private lateinit var rvExercicios: RecyclerView

    private val db = FirebaseFirestore.getInstance()

    private val listaExercicios = listOf(
        ExercicioFinalizadoAluno(
            frame = "frame1",
            execucao = "12 minutos",
            nome = "Puxada alta aberta",
            series = listOf(
                SerieFinalizadoAluno("1", "10", "8 Kg", "30", true, "12"),
                SerieFinalizadoAluno("2", "12", "8 Kg", "30", false, "12")
            )
        ),
        ExercicioFinalizadoAluno(
            frame = "frame2",
            execucao = "9 minutos",
            nome = "Remada baixa",
            series = listOf(
                SerieFinalizadoAluno("1", "10", "8 Kg", "30", true, "9"),
                SerieFinalizadoAluno("2", "12", "8 Kg", "30", false, "9"),
                SerieFinalizadoAluno("3", "10", "10 Kg", "30", false, "9")
            )
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_exercicio_finalizado_aluno)

        btnVoltar    = findViewById(R.id.btnVoltar)
        btnFinalizar = findViewById(R.id.btnFinalizar)
        rvExercicios = findViewById(R.id.rvExercicios)

        // Voltar sem incrementar
        btnVoltar.setOnClickListener { finish() }

        // Finalizar: incrementa sequência e atualiza recorde, depois fecha
        btnFinalizar.setOnClickListener {
            incrementarSequencia()
        }

        rvExercicios.layoutManager = LinearLayoutManager(this)
        rvExercicios.adapter = ExerciciosAdapterFinalizadoAluno(listaExercicios) { exercicio ->
            // caso tenha detalhe
        }
    }

    private fun incrementarSequencia() {
        val uid = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            .getString("alunoDocId", null) ?: return

        val alunoRef = db.collection("alunos").document(uid)

        // 1) Incrementa em 1 o campo sequenciaDias e atualiza lastTreino
        alunoRef.update(
            mapOf(
                "sequenciaDias" to FieldValue.increment(1),
                "lastTreino"     to FieldValue.serverTimestamp()
            )
        ).addOnSuccessListener {
            // 2) Atualiza recorde, se precisar
            alunoRef.get().addOnSuccessListener { snap ->
                val seq = snap.getLong("sequenciaDias") ?: 0L
                val rec = snap.getLong("recordeDias")   ?: 0L
                if (seq > rec) {
                    alunoRef.update("recordeDias", seq)
                }
                finish()
                Toast.makeText(this, "Treino finalizado! 🔥", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this,
                "Falha ao atualizar sequência: ${e.message}",
                Toast.LENGTH_LONG).show()
        }
    }
}
