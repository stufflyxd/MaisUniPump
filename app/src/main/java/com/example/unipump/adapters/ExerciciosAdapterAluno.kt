package com.example.unipump.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.R
import com.example.unipump.models.ExercicioAluno


class ExerciciosAdapterAluno(
    private val listaExercicioAlunos: List<ExercicioAluno>
) : RecyclerView.Adapter<ExerciciosAdapterAluno.ExercicioVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExercicioVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_exercicio, parent, false)
        return ExercicioVH(view)
    }

    override fun onBindViewHolder(holder: ExercicioVH, position: Int) {
        holder.bind(listaExercicioAlunos[position])
    }

    override fun getItemCount(): Int = listaExercicioAlunos.size

    inner class ExercicioVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Ajuste: o TextView no card_exercicios tem id "tvNome"
        private val tvNome = itemView.findViewById<TextView>(R.id.tvNome)
        private val rvSeries = itemView.findViewById<RecyclerView>(R.id.rvSeries)

        fun bind(exercicioAluno: ExercicioAluno) {
            tvNome.text = exercicioAluno.nome
            rvSeries.apply {
                layoutManager = LinearLayoutManager(context)
                // Corrige instância de adapter sem chamada inválida de função
                adapter =
                    SeriesAdapterAluno(exercicioAluno.series).also { it.notifyDataSetChanged() }
            }
        }
    }
}