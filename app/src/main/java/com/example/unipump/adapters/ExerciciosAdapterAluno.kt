package com.example.unipump.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.unipump.R
import com.example.unipump.models.ExercicioAluno
import com.example.unipump.models.SerieAluno

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
        private val tvNome = itemView.findViewById<TextView>(R.id.tvNome)
        private val tvExecucao = itemView.findViewById<TextView>(R.id.tvExecucao)
        private val rvSeries = itemView.findViewById<RecyclerView>(R.id.rvSeries)
        private val imgExercicio = itemView.findViewById<ImageView>(R.id.imgExercicio)

        fun bind(exercicioAluno: ExercicioAluno) {
            // Configurar nome e execução
            tvNome.text = exercicioAluno.nome
            tvExecucao.text = exercicioAluno.execucao

            // Carregar imagem do exercício
            carregarImagemExercicio(exercicioAluno)

            // Configurar RecyclerView de séries
            rvSeries.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = SeriesAdapterAluno(
                    exercicioAluno.series,
                    onSerieFieldChanged = { indice: Int, serieAtualizada: SerieAluno ->
                        // 1) Reconstruir lista de séries a partir da antiga
                        val listaMutavel = exercicioAluno.series.toMutableList()
                        listaMutavel[indice] = serieAtualizada
                        // 2) Atribuir de volta para o ExercicioAluno
                        exercicioAluno.series = listaMutavel.toList()
                    }
                ).also {
                    it.notifyDataSetChanged()
                }
            }
        }

        private fun carregarImagemExercicio(exercicioAluno: ExercicioAluno) {
            val context = itemView.context

            Log.d("GLIDE_EXERCICIO_ALUNO", "=== CARREGANDO IMAGEM DO EXERCÍCIO ALUNO ===")
            Log.d("GLIDE_EXERCICIO_ALUNO", "Exercício: ${exercicioAluno.nome}")
            Log.d("GLIDE_EXERCICIO_ALUNO", "Frame URL: '${exercicioAluno.frame}'")
            Log.d("GLIDE_EXERCICIO_ALUNO", "Execução: ${exercicioAluno.execucao}")

            if (exercicioAluno.frame.isNotEmpty()) {
                try {
                    // Configurar opções do Glide
                    val requestOptions = RequestOptions()
                        .placeholder(R.drawable.icon_rectangle)
                        .error(R.drawable.icon_rectangle)
                        .fallback(R.drawable.icon_rectangle)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()

                    Glide.with(context)
                        .load(exercicioAluno.frame)
                        .apply(requestOptions)
                        .into(imgExercicio)

                    Log.d("GLIDE_EXERCICIO_ALUNO", "✅ Glide configurado para carregar: ${exercicioAluno.frame}")
                } catch (e: Exception) {
                    Log.e("GLIDE_EXERCICIO_ALUNO", "❌ Erro ao configurar Glide para ${exercicioAluno.nome}", e)
                    imgExercicio.setImageResource(R.drawable.icon_rectangle)
                }
            } else {
                Log.d("GLIDE_EXERCICIO_ALUNO", "⚠️ Frame URL vazio para ${exercicioAluno.nome}, usando imagem padrão")
                imgExercicio.setImageResource(R.drawable.icon_rectangle)
            }
        }
    }
}
