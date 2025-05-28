package com.example.unipump.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.R
import com.example.unipump.models.Exercicio

class ExerciciosAdapter(
    private val listaExercicios: List<Exercicio>) : RecyclerView.Adapter<ExerciciosAdapter.ExercicioVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExercicioVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_exercicio, parent, false)
        return ExercicioVH(view)
    }

    override fun onBindViewHolder(holder: ExercicioVH, position: Int) {
        holder.bind(listaExercicios[position])
    }

    override fun getItemCount(): Int = listaExercicios.size

    inner class ExercicioVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Ajuste: o TextView no card_exercicios tem id "tvNome"
        private val tvNome = itemView.findViewById<TextView>(R.id.tvNome)
        private val rvSeries = itemView.findViewById<RecyclerView>(R.id.rvSeries)

        fun bind(exercicio: Exercicio) {
            tvNome.text = exercicio.nome
            rvSeries.apply {
                layoutManager = LinearLayoutManager(context)
                // Corrige instância de adapter sem chamada inválida de função
                adapter = SeriesAdapter(exercicio.series).also { it.notifyDataSetChanged() }
            }
        }
    }
}