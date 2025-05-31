// caminho: app/src/main/java/com/example/unipump/adapters/SeriesAdapter.kt
package com.example.unipump.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.R
import com.example.unipump.models.SerieAluno

/**
 * Adapter que exibe cada linha de 'item_serie.xml' (uma única Série).
 * A cada vez que o usuário edita e sai de foco de um EditText,
 * chamamos onSerieFieldChanged(indexDaSerie, novaSerie).
 *
 * Note que recebemos: `initialList: List<SerieAluno>` (imutável).
 * Internamente, guardamos uma cópia mutável para controlar as mudanças.
 */
class SeriesAdapterAluno(
    initialList: List<SerieAluno>,
    private val onSerieFieldChanged: (indiceSerie: Int, serieAtualizada: SerieAluno) -> Unit
) : RecyclerView.Adapter<SeriesAdapterAluno.SerieViewHolder>() {

    // Cópia mutável das séries: ao editar, atualizamos aqui.
    private val seriesList = initialList.toMutableList()

    inner class SerieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrdem: TextView       = view.findViewById(R.id.tvOrdem)
        val tvPeso: EditText        = view.findViewById(R.id.tvPeso)
        val tvReps: EditText        = view.findViewById(R.id.tvReps)
        val tvDescanso: EditText    = view.findViewById(R.id.tvDescanso)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SerieViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_serie, parent, false)
        return SerieViewHolder(itemView)
    }

    override fun getItemCount(): Int = seriesList.size

    override fun onBindViewHolder(holder: SerieViewHolder, position: Int) {
        val serie = seriesList[position]

        // Preenche com os valores atuais
        holder.tvOrdem.text = serie.ordem
        holder.tvPeso.setText(serie.peso)
        holder.tvReps.setText(serie.reps)
        holder.tvDescanso.setText(serie.descanso)

        // Definimos listeners de FocusChange em cada EditText. Quando o usuário sai do campo,
        // capturamos o novo texto e, se diferente, criamos uma cópia de SerieAluno com o campo alterado,
        // armazenamos em seriesList e disparamos onSerieFieldChanged.
        holder.tvPeso.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val novoPeso = holder.tvPeso.text.toString()
                if (serie.peso != novoPeso) {
                    val updated = serie.copy(peso = novoPeso)
                    seriesList[position] = updated
                    onSerieFieldChanged(position, updated)
                }
            }
        }

        holder.tvReps.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val novasReps = holder.tvReps.text.toString()
                if (serie.reps != novasReps) {
                    val updated = serie.copy(reps = novasReps)
                    seriesList[position] = updated
                    onSerieFieldChanged(position, updated)
                }
            }
        }

        holder.tvDescanso.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val novoDescanso = holder.tvDescanso.text.toString()
                if (serie.descanso != novoDescanso) {
                    val updated = serie.copy(descanso = novoDescanso)
                    seriesList[position] = updated
                    onSerieFieldChanged(position, updated)
                }
            }
        }
    }
}
