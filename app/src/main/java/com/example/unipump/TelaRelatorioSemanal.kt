package com.example.unipump

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import java.util.*

class TelaRelatorioSemanal : BaseActivity() {

    private lateinit var btnVoltar: ImageButton
    private lateinit var tvTreinos: TextView
    private lateinit var tvKcal: TextView
    private lateinit var tvMinutos: TextView
    private lateinit var calendar: MaterialCalendarView
    /*private lateinit var todosReg: TextView*/

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_relatorio_semanal)

        btnVoltar = findViewById(R.id.btnVoltar)
        tvTreinos = findViewById(R.id.tvTreinosCount)
        tvKcal    = findViewById(R.id.tvKcalCount)
        tvMinutos = findViewById(R.id.tvMinutosCount)
        calendar  = findViewById(R.id.calendar_view)
        /*todosReg  = findViewById(R.id.todosRegistros)
*/
        btnVoltar.setOnClickListener { finish() }
        /*todosReg.setOnClickListener {
            startActivity(Intent(this, TelaDadosDeTreino::class.java))
        }*/

        setupBottomNav()
        carregarResumoMes()
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

    private fun carregarResumoMes() {
        val uid = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            .getString("alunoDocId", null) ?: return

        val alunoRef = db.collection("alunos").document(uid)

        alunoRef.get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                // 1) pega os totais que você já acumulou no campo do aluno
                val totalExs  = doc.getLong("total_exercicios") ?: 0L
                val totalKcal = doc.getLong("total_kcal")       ?: 0L
                val totalMin  = doc.getLong("total_tempo")      ?: 0L

                tvTreinos.text  = totalExs.toString()
                tvKcal.text     = totalKcal.toString()
                tvMinutos.text  = totalMin.toString()

                // 2) marca no calendário apenas o último treino, forçando fuso de Fortaleza
                val lastTs = doc.getTimestamp("lastTreino")
                val treinoDaySet: Set<CalendarDay> = lastTs
                    ?.toDate()
                    // converte para Calendar no fuso “America/Fortaleza”
                    ?.let { date ->
                        val tzBR = TimeZone.getTimeZone("America/Fortaleza")
                        val cal = Calendar.getInstance(tzBR).apply {
                            time = date
                        }
                        // extrai ano, mês e dia corretamente no fuso local BR
                        val ano   = cal.get(Calendar.YEAR)
                        val mes   = cal.get(Calendar.MONTH)  // 0 = janeiro, 1 = fevereiro, etc.
                        val dia   = cal.get(Calendar.DAY_OF_MONTH)
                        setOf(CalendarDay.from(ano, mes, dia))
                    }
                    ?: emptySet()

                // 3) Para pintar o restante do mês de vermelho:
                //    a) pego “hoje” no fuso “America/Fortaleza”
                val tzBR = TimeZone.getTimeZone("America/Fortaleza")
                val hoje = Calendar.getInstance(tzBR)
                //    b) defino início do mês (sempre dia 1) e fim do mês (getActualMaximum)
                val inicioMes = hoje.clone() as Calendar
                inicioMes.set(Calendar.DAY_OF_MONTH, 1)
                inicioMes.set(Calendar.HOUR_OF_DAY, 0)
                inicioMes.set(Calendar.MINUTE, 0)
                inicioMes.set(Calendar.SECOND, 0)
                inicioMes.set(Calendar.MILLISECOND, 0)

                val fimMes = hoje.clone() as Calendar
                fimMes.set(Calendar.DAY_OF_MONTH, fimMes.getActualMaximum(Calendar.DAY_OF_MONTH))
                fimMes.set(Calendar.HOUR_OF_DAY, 0)
                fimMes.set(Calendar.MINUTE, 0)
                fimMes.set(Calendar.SECOND, 0)
                fimMes.set(Calendar.MILLISECOND, 0)

                //    c) itero cada dia do mês, convertendo para CalendarDay via o mesmo fuso
                val todosDiasMes = mutableSetOf<CalendarDay>()
                var cal = inicioMes.clone() as Calendar
                while (!cal.after(fimMes)) {
                    val anoI  = cal.get(Calendar.YEAR)
                    val mesI  = cal.get(Calendar.MONTH)
                    val diaI  = cal.get(Calendar.DAY_OF_MONTH)
                    todosDiasMes.add(CalendarDay.from(anoI, mesI, diaI))
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                }

                // 4) finalmente: limpo qualquer decorator anterior e tiro do “todosDiasMes”
                //    aqueles que são “treinoDaySet”, pintando-os de verde, e o resto de vermelho
                calendar.removeDecorators()
                calendar.addDecorator(EventDecorator(Color.RED,   todosDiasMes - treinoDaySet))
                calendar.addDecorator(EventDecorator(Color.GREEN, treinoDaySet))

                // 5) defino limite mínimo e máximo do calendário
                calendar.state().edit()
                    .setMinimumDate(CalendarDay.from(
                        inicioMes.get(Calendar.YEAR),
                        inicioMes.get(Calendar.MONTH),
                        inicioMes.get(Calendar.DAY_OF_MONTH)
                    ))
                    .setMaximumDate(CalendarDay.from(
                        fimMes.get(Calendar.YEAR),
                        fimMes.get(Calendar.MONTH),
                        fimMes.get(Calendar.DAY_OF_MONTH)
                    ))
                    .commit()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar resumo: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Decora o fundo dos dias especificados com a cor indicada.
     */
    class EventDecorator(
        private val color: Int,
        private val dates: Collection<CalendarDay>
    ) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean =
            dates.contains(day)

        override fun decorate(view: DayViewFacade) {
            view.setBackgroundDrawable(ColorDrawable(color))
        }
    }
}
