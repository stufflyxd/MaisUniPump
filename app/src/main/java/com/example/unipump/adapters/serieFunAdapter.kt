package com.example.unipump.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.R
import com.example.unipump.models.serieFun

class serieFunAdapter(
    private val series: MutableList<serieFun>,
    private val onDeleteSerie: (Int) -> Unit,
    private val onSerieAlterada: ((serieFun, Int) -> Unit)? = null // NOVO callback
) : RecyclerView.Adapter<serieFunAdapter.SerieViewHolder>() {

    class SerieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrdem: TextView = itemView.findViewById(R.id.tvOrdem)
        val etRepeticoes: EditText = itemView.findViewById(R.id.tvReps)
        val etPeso: EditText = itemView.findViewById(R.id.tvPeso)
        val etTempo: EditText = itemView.findViewById(R.id.tvDescanso)
        val btnExcluirSerie: ImageButton = itemView.findViewById(R.id.btnExcluirSerie)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SerieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_serie_funcionario, parent, false)
        return SerieViewHolder(view)
    }

    override fun onBindViewHolder(holder: SerieViewHolder, position: Int) {
        val serie = series[position]

        // Preencher campos
        holder.tvOrdem.text = serie.numero.toString()
        holder.etRepeticoes.setText(serie.repeticoes.toString())
        holder.etPeso.setText(serie.peso)
        holder.etTempo.setText(serie.tempo)

        // Remover listeners anteriores para evitar conflitos
        holder.etRepeticoes.removeTextChangedListener(holder.etRepeticoes.tag as? TextWatcher)
        holder.etPeso.removeTextChangedListener(holder.etPeso.tag as? TextWatcher)
        holder.etTempo.removeTextChangedListener(holder.etTempo.tag as? TextWatcher)

        // NOVO: Criar TextWatchers com callback de alteração
        val repeticoesWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION && currentPosition < series.size) {
                    val repeticoes = s.toString().toIntOrNull() ?: 0
                    val serieAtualizada = series[currentPosition].copy(repeticoes = repeticoes)
                    series[currentPosition] = serieAtualizada

                    // NOVO: Notificar alteração
                    onSerieAlterada?.invoke(serieAtualizada, currentPosition)
                }
            }
        }

        val pesoWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION && currentPosition < series.size) {
                    val serieAtualizada = series[currentPosition].copy(peso = s.toString())
                    series[currentPosition] = serieAtualizada

                    // NOVO: Notificar alteração
                    onSerieAlterada?.invoke(serieAtualizada, currentPosition)
                }
            }
        }

        val tempoWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION && currentPosition < series.size) {
                    val serieAtualizada = series[currentPosition].copy(tempo = s.toString())
                    series[currentPosition] = serieAtualizada

                    // NOVO: Notificar alteração
                    onSerieAlterada?.invoke(serieAtualizada, currentPosition)
                }
            }
        }

        // Adicionar TextWatchers
        holder.etRepeticoes.addTextChangedListener(repeticoesWatcher)
        holder.etPeso.addTextChangedListener(pesoWatcher)
        holder.etTempo.addTextChangedListener(tempoWatcher)

        // Armazenar watchers como tags para remover depois
        holder.etRepeticoes.tag = repeticoesWatcher
        holder.etPeso.tag = pesoWatcher
        holder.etTempo.tag = tempoWatcher

        // Botão excluir
        holder.btnExcluirSerie.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                onDeleteSerie(currentPosition)
            }
        }
    }

    override fun getItemCount(): Int = series.size

    fun addSerie() {
        val novoNumero = if (series.isEmpty()) 1 else series.maxOf { it.numero } + 1
        val novaSerie = serieFun(
            id = "serie_${System.currentTimeMillis()}",
            numero = novoNumero,
            repeticoes = 10,
            peso = "",
            tempo = "60"
        )
        series.add(novaSerie)
        notifyItemInserted(series.size - 1)

        // NOVO: Notificar que nova série foi adicionada
        onSerieAlterada?.invoke(novaSerie, series.size - 1)
    }

    fun removeSerie(position: Int) {
        if (position >= 0 && position < series.size) {
            series.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, series.size)

            // Reordenar números das séries
            series.forEachIndexed { index, serie ->
                val serieAtualizada = serie.copy(numero = index + 1)
                series[index] = serieAtualizada

                // NOVO: Notificar alteração da reordenação
                onSerieAlterada?.invoke(serieAtualizada, index)
            }
            notifyDataSetChanged()
        }
    }
}