package com.example.unipump.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.R
import com.example.unipump.models.SerieFinalizadoAluno

class SeriesAdapterFinalizadoAluno(
    private val listaSeries: List<SerieFinalizadoAluno>
) : RecyclerView.Adapter<SeriesAdapterFinalizadoAluno.SerieVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SerieVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_serie_exercicio_finalizado_aluno, parent, false)
        return SerieVH(view)
    }

    override fun onBindViewHolder(holder: SerieVH, position: Int) {
        holder.bind(listaSeries[position])
    }

    override fun getItemCount(): Int = listaSeries.size

    inner class SerieVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrdem     = itemView.findViewById<TextView>(R.id.tvNumSerie)
        private val tvPeso      = itemView.findViewById<TextView>(R.id.tvPeso)
        private val tvReps      = itemView.findViewById<TextView>(R.id.tvReps)
        private val tvDuracao   = itemView.findViewById<TextView>(R.id.tvDuracao)
        private val cbFeito     = itemView.findViewById<CheckBox>(R.id.cbFeito)

        fun bind(serie: SerieFinalizadoAluno) {
            // Preenche valores
            tvOrdem.text   = serie.ordem
            tvPeso.text    = serie.peso
            tvReps.text    = serie.reps
            tvDuracao.text = serie.duracao

            // Marca/desmarca o checkbox
            cbFeito.isChecked = serie.feito
            cbFeito.setOnCheckedChangeListener { _, checked ->
                serie.feito = checked
            }
        }
    }
}
