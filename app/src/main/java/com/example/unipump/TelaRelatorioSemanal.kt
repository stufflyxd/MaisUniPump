package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class TelaRelatorioSemanal : AppCompatActivity() {

    private lateinit var btnVoltar: ImageButton
    private lateinit var tvTreinos: TextView
    private lateinit var tvKcal: TextView
    private lateinit var tvMinutos: TextView
    private lateinit var calendar: CalendarView
    private lateinit var todosReg: TextView

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_relatorio_semanal)

        btnVoltar = findViewById(R.id.btnVoltar)
        tvTreinos = findViewById(R.id.tvTreinosCount)
        tvKcal    = findViewById(R.id.tvKcalCount)
        tvMinutos = findViewById(R.id.tvMinutosCount)
        calendar  = findViewById(R.id.calendar_view)
        todosReg  = findViewById(R.id.todosRegistros)

        btnVoltar.setOnClickListener { finish() }
        todosReg.setOnClickListener {
            startActivity(Intent(this, TelaDadosDeTreino::class.java))
        }

        setupBottomNav()
        carregarTotaisDoAluno()
    }

    private fun setupBottomNav() {
        findViewById<BottomNavigationView>(R.id.bottom_navigation)
            .setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_inicio  -> { startActivity(Intent(this, TelaPrincipalAluno::class.java)); true }
                    R.id.nav_treinos -> { startActivity(Intent(this, TelaTreinoAluno::class.java));    true }
                    R.id.nav_chat    -> { startActivity(Intent(this, TelaChat::class.java));          true }
                    R.id.nav_config  -> { startActivity(Intent(this, TelaConfig::class.java));        true }
                    else             -> false
                }
            }
    }

    private fun carregarTotaisDoAluno() {
        val uid = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            .getString("alunoDocId", null) ?: return

        db.collection("alunos")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                // pega total_exercicios em vez de total_treinos
                val totalExs = doc.getLong("total_exercicios") ?: 0L
                val totalKcal  = doc.getLong("total_kcal")    ?: 0L
                val totalTempo = doc.getLong("total_tempo")   ?: 0L

                tvTreinos.text = totalExs.toString()
                tvKcal   .text = totalKcal.toString()
                tvMinutos.text = totalTempo.toString()

                // ajusta o calendário para a última semana
                val calHoje   = Calendar.getInstance()
                val calSemana = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
                calendar.minDate = calSemana.timeInMillis
                calendar.maxDate = calHoje.timeInMillis
            }
            .addOnFailureListener { e ->
                Toast.makeText(this,
                    "Erro ao carregar totais: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
