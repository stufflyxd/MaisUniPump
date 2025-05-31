// app/src/main/java/com/example/unipump/adapters/ExerciciosAdapterFinalizadoAluno.kt

package com.example.unipump.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.unipump.R
import com.example.unipump.TelaDetalheExercicio
import com.example.unipump.models.ExercicioFinalizadoAluno

/**
 * Adapter de exercícios finalizados.
 *
 * Agora o onExercicioClick recebe:
 *  - posicao: Int (para sabermos qual item da lista foi clicado)
 *  - exercicio: ExercicioFinalizadoAluno (o próprio objeto, que é Parcelable)
 */
class ExerciciosAdapterFinalizadoAluno(
    private val listaExercicios: List<ExercicioFinalizadoAluno>,
    private val onExercicioClick: (posicao: Int, exercicio: ExercicioFinalizadoAluno) -> Unit
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
        // IDs que existem em item_exercicio_finalizado_aluno.xml
        private val imgExercicio       = itemView.findViewById<ImageView>(R.id.imgExercicio)
        private val tvExecucao         = itemView.findViewById<TextView>(R.id.tvExecucao)
        private val tvNome             = itemView.findViewById<TextView>(R.id.tvNome)
        private val ivClock            = itemView.findViewById<ImageView>(R.id.ivClock)
        private val rvSeries           = itemView.findViewById<RecyclerView>(R.id.rvSeries)

        fun bind(exercicio: ExercicioFinalizadoAluno) {
            // 1) Carregar frame (imagem), se existir
            if (exercicio.frame.isNotEmpty()) {
                val requestOptions = RequestOptions()
                    .placeholder(R.drawable.icon_rectangle)
                    .error(R.drawable.icon_rectangle)
                    .circleCrop()

                Glide.with(itemView.context)
                    .load(exercicio.frame)
                    .apply(requestOptions)
                    .into(imgExercicio)
            } else {
                imgExercicio.setImageResource(R.drawable.icon_rectangle)
            }

            // 2) Preencher os TextViews com execução e nome
            tvExecucao.text = exercicio.execucao
            tvNome.text     = exercicio.nome

            // 3) ivClock permanece apenas como ícone. Não precisa mudar seu conteúdo aqui.

            // 4) Configurar RecyclerView interno (lista de séries)
            val seriesAdapter = SeriesAdapterFinalizadoAluno(exercicio.series)
            rvSeries.apply {
                layoutManager = LinearLayoutManager(
                    itemView.context,
                    LinearLayoutManager.VERTICAL,
                    false
                )
                adapter = seriesAdapter
                isNestedScrollingEnabled = false
            }
            seriesAdapter.notifyDataSetChanged()

            // 5) Clique no item passa a posição e o objeto para a Activity pai
            itemView.setOnClickListener {
                val posicao = adapterPosition
                if (posicao != RecyclerView.NO_POSITION) {
                    onExercicioClick(posicao, exercicio)
                }
            }
        }
    }
}
