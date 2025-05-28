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
import com.example.unipump.models.Serie

class SeriesAdapter(
    private val listaSeries: List<Serie>
) : RecyclerView.Adapter<SeriesAdapter.SerieVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SerieVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_serie, parent, false)
        return SerieVH(view)
    }

    override fun onBindViewHolder(holder: SerieVH, position: Int) {
        holder.bind(listaSeries[position])
    }

    override fun getItemCount(): Int = listaSeries.size

    inner class SerieVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrdem = itemView.findViewById<TextView>(R.id.tvOrdem)
        private val etReps = itemView.findViewById<EditText>(R.id.tvReps)
        private val etPeso = itemView.findViewById<EditText>(R.id.tvPeso)
        private val etDescanso = itemView.findViewById<EditText>(R.id.tvDescanso)

        private var repsWatcher: TextWatcher? = null
        private var pesoWatcher: TextWatcher? = null
        private var descansoWatcher: TextWatcher? = null

        fun bind(serie: Serie) {
            tvOrdem.text = serie.ordem

            // Remove watchers antigos
            repsWatcher?.let { etReps.removeTextChangedListener(it) }
            pesoWatcher?.let { etPeso.removeTextChangedListener(it) }
            descansoWatcher?.let { etDescanso.removeTextChangedListener(it) }

            // Popula campos
            etReps.setText(serie.reps)
            etPeso.setText(serie.peso)
            etDescanso.setText(serie.descanso)

            // Cria e atribui watcher de reps
            repsWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    serie.reps = s.toString()
                }
            }
            etReps.addTextChangedListener(repsWatcher)

            // Cria e atribui watcher de peso
            pesoWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    serie.peso = s.toString()
                }
            }
            etPeso.addTextChangedListener(pesoWatcher)

            // Cria e atribui watcher de descanso
            descansoWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    serie.descanso = s.toString()
                }
            }
            etDescanso.addTextChangedListener(descansoWatcher)
        }
    }
}