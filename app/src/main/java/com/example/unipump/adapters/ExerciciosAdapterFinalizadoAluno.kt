package com.example.unipump.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.R
import com.example.unipump.TelaDetalheExercicio
import com.example.unipump.models.ExercicioFinalizadoAluno

class ExerciciosAdapterFinalizadoAluno(
    private val listaExercicios: List<ExercicioFinalizadoAluno>,
    private val onExercicioClick: (ExercicioFinalizadoAluno) -> Unit
) : RecyclerView.Adapter<ExerciciosAdapterFinalizadoAluno.ExercicioVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExercicioVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercicio_finalizado_aluno, parent, false)
        return ExercicioVH(view)
    }

    override fun onBindViewHolder(holder: ExercicioVH, position: Int) {
        holder.bind(listaExercicios[position])
    }

    override fun getItemCount(): Int = listaExercicios.size

    inner class ExercicioVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTituloExercicio = itemView.findViewById<TextView>(R.id.tvTituloExercicio)
        private val tvTempoExercicio  = itemView.findViewById<TextView>(R.id.tvTempoExercicio)
        private val rvSeries          = itemView.findViewById<RecyclerView>(R.id.rvSeries)

        fun bind(exercicio: ExercicioFinalizadoAluno) {
            tvTituloExercicio.text = exercicio.nome
            tvTempoExercicio.text  = "Tempo de execução ${exercicio.execucao}"

            // 1) garante que o SeriesAdapterAluno seja recriado para cada bind
            val seriesAdapter = SeriesAdapterFinalizadoAluno(exercicio.series)

            // 2) configura o RecyclerView de séries
            // dentro de bind(exercicio: ExercicioAluno) { … }
            rvSeries.apply {
                layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.VERTICAL, false)
                adapter       = seriesAdapter
                isNestedScrollingEnabled = false
            }


            // 3) força atualização (às vezes necessária dentro de outro RecyclerView)
            seriesAdapter.notifyDataSetChanged()

            // click no card, se precisar
            itemView.setOnClickListener {
                val intent = Intent(itemView.context, TelaDetalheExercicio::class.java)
                    .putExtra("EXERCICIO_DETALHE", exercicio)     // <<< passa o ExercicioAluno todo
                itemView.context.startActivity(intent)
            }
        }
    }

}
