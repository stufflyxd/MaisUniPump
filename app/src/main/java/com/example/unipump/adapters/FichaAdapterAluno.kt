package com.example.unipump.adapters

import com.example.unipump.R
import com.example.unipump.models.FichaTreinoAluno
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView

class FichaAdapterAluno(
    private val items: List<FichaTreinoAluno>,
    private val onClick: (FichaTreinoAluno) -> Unit
) : RecyclerView.Adapter<FichaAdapterAluno.FichaViewHolder>() {

    inner class FichaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLetra: TextView = itemView.findViewById(R.id.tvLetra)
        private val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        private val tvTotal: TextView  = itemView.findViewById(R.id.tvTotal)
        private val tvLabel: TextView  = itemView.findViewById(R.id.tvLabel)

        fun bind(ficha: FichaTreinoAluno) {
            tvLetra.text = ficha.letra
            tvTitulo.text = ficha.titulo
            tvTotal.text = ficha.totalExercicios.toString()
            tvLabel.text = if (ficha.totalExercicios == 1) "Exercício" else "Exercícios"

            itemView.setOnClickListener { onClick(ficha) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FichaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ficha_aluno, parent, false)
        return FichaViewHolder(view)
    }

    override fun onBindViewHolder(holder: FichaViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}