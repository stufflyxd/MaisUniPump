package com.example.unipump.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.unipump.R
import com.example.unipump.models.exercicioFun
import com.example.unipump.models.serieFun

class exercicioFunAdapter(
    private val exercicios: MutableList<exercicioFun>,
    private val onDeleteExercicio: (Int) -> Unit,
    private val onAdicionarSerie: ((Int) -> Unit)? = null,
    private val onExercicioAlterado: ((exercicioFun, Int) -> Unit)? = null
) : RecyclerView.Adapter<exercicioFunAdapter.ExercicioViewHolder>() {

    class ExercicioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNomeExercicio: TextView = itemView.findViewById(R.id.tvNomeExercicio)
        val btnExcluirExercicio: ImageButton = itemView.findViewById(R.id.btnExcluirExercicio)
        val btnAddSerie: ImageButton = itemView.findViewById(R.id.btnAddSerie)
        val recyclerViewSeries: RecyclerView = itemView.findViewById(R.id.recyclerViewSeries)

        // NOVO: ImageView para a imagem do exercício
        val imgExercicio: ImageView = itemView.findViewById(R.id.imgExercicio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExercicioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercicio_funcionario, parent, false)
        return ExercicioViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExercicioViewHolder, position: Int) {
        val exercicio = exercicios[position]

        // Configurar nome do exercício
        holder.tvNomeExercicio.text = exercicio.nome

        // NOVO: Carregar imagem do exercício com Glide
        carregarImagemExercicio(holder, exercicio)

        // Configurar RecyclerView das séries
        val seriesAdapter = serieFunAdapter(
            series = exercicio.series,
            onDeleteSerie = { seriePosition: Int ->
                removerSerie(position, seriePosition)
            },
            onSerieAlterada = { serieAlterada: serieFun, seriePosition: Int ->
                onSerieAlterada(position, serieAlterada, seriePosition)
            }
        )

        holder.recyclerViewSeries.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = seriesAdapter
            isNestedScrollingEnabled = false
        }

        // Configurar botão adicionar série
        holder.btnAddSerie.setOnClickListener {
            Log.d("ADD_SERIE", "Clicou em adicionar série no exercício: ${exercicio.nome}")
            adicionarSerie(position)
        }

        // Configurar botão excluir exercício
        holder.btnExcluirExercicio.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                onDeleteExercicio(currentPosition)
            }
        }
    }

    // NOVO: Método para carregar imagem do exercício
    private fun carregarImagemExercicio(holder: ExercicioViewHolder, exercicio: exercicioFun) {
        val context = holder.itemView.context

        Log.d("GLIDE_EXERCICIO", "=== CARREGANDO IMAGEM ===")
        Log.d("GLIDE_EXERCICIO", "Exercício: ${exercicio.nome}")
        Log.d("GLIDE_EXERCICIO", "Frame URL: '${exercicio.frame}'")

        if (!exercicio.frame.isNullOrEmpty()) {
            // Configurar opções do Glide
            val requestOptions = RequestOptions()
                .placeholder(R.drawable.icon_rectangle) // Imagem enquanto carrega
                .error(R.drawable.icon_rectangle) // Imagem em caso de erro
                .fallback(R.drawable.icon_rectangle) // Imagem se URL for null
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache para melhor performance
                .centerCrop() // Ajustar imagem mantendo proporção

            try {
                Glide.with(context)
                    .load(exercicio.frame)
                    .apply(requestOptions)
                    .into(holder.imgExercicio)

                Log.d("GLIDE_EXERCICIO", "✅ Glide configurado para carregar: ${exercicio.frame}")

            } catch (e: Exception) {
                Log.e("GLIDE_EXERCICIO", "❌ Erro ao configurar Glide para ${exercicio.nome}", e)
                // Em caso de erro, usar imagem padrão
                holder.imgExercicio.setImageResource(R.drawable.icon_rectangle)
            }
        } else {
            Log.d("GLIDE_EXERCICIO", "⚠️ Frame URL vazio para ${exercicio.nome}, usando imagem padrão")
            // Se não há URL, usar imagem padrão
            holder.imgExercicio.setImageResource(R.drawable.icon_rectangle)
        }
    }

    override fun getItemCount(): Int = exercicios.size

    // Método para adicionar série a um exercício específico
    private fun adicionarSerie(exercicioPosition: Int) {
        if (exercicioPosition >= 0 && exercicioPosition < exercicios.size) {
            val exercicio = exercicios[exercicioPosition]

            // Determinar o número da próxima série
            val proximoNumero = if (exercicio.series.isEmpty()) {
                1
            } else {
                exercicio.series.maxOf { it.numero } + 1
            }

            // Criar nova série
            val novaSerie = serieFun(
                id = "${exercicio.id}_serie_${System.currentTimeMillis()}",
                numero = proximoNumero,
                repeticoes = 12, // Valor padrão
                peso = "", // Vazio para o usuário preencher
                tempo = "60" // Tempo padrão de descanso
            )

            // Adicionar à lista de séries do exercício
            exercicio.series.add(novaSerie)

            Log.d("ADD_SERIE", "Série adicionada ao exercício ${exercicio.nome}. Total de séries: ${exercicio.series.size}")

            // Notificar que o exercício foi alterado
            onExercicioAlterado?.invoke(exercicio, exercicioPosition)

            // Atualizar o RecyclerView
            notifyItemChanged(exercicioPosition)

            // Callback para a activity principal (se fornecido)
            onAdicionarSerie?.invoke(exercicioPosition)
        }
    }

    // Método chamado quando uma série é alterada
    private fun onSerieAlterada(exercicioPosition: Int, serieAlterada: serieFun, seriePosition: Int) {
        if (exercicioPosition >= 0 && exercicioPosition < exercicios.size) {
            val exercicio = exercicios[exercicioPosition]

            // Atualizar a série na lista
            if (seriePosition >= 0 && seriePosition < exercicio.series.size) {
                exercicio.series[seriePosition] = serieAlterada

                Log.d("SERIE_ALTERADA", "Série alterada no exercício ${exercicio.nome}")

                // Notificar que o exercício foi alterado
                onExercicioAlterado?.invoke(exercicio, exercicioPosition)
            }
        }
    }

    // Remover série
    private fun removerSerie(exercicioPosition: Int, seriePosition: Int) {
        if (exercicioPosition >= 0 && exercicioPosition < exercicios.size) {
            val exercicio = exercicios[exercicioPosition]

            if (seriePosition >= 0 && seriePosition < exercicio.series.size) {
                exercicio.series.removeAt(seriePosition)

                // Reordenar números das séries restantes
                exercicio.series.forEachIndexed { index, serie ->
                    exercicio.series[index] = serie.copy(numero = index + 1)
                }

                Log.d("REMOVE_SERIE", "Série removida do exercício ${exercicio.nome}. Total de séries: ${exercicio.series.size}")

                // Notificar que o exercício foi alterado
                onExercicioAlterado?.invoke(exercicio, exercicioPosition)

                // Atualizar o RecyclerView
                notifyItemChanged(exercicioPosition)
            }
        }
    }

    // Métodos existentes mantidos
    fun addExercicio(nomeExercicio: String, frameUrl: String = "") {
        val novoExercicio = exercicioFun(
            id = "exercicio_${System.currentTimeMillis()}",
            nome = nomeExercicio,
            frame = frameUrl, // NOVO: Incluir URL da imagem
            series = mutableListOf(
                serieFun(
                    id = "serie_${System.currentTimeMillis()}",
                    numero = 1,
                    repeticoes = 10,
                    peso = "",
                    tempo = "60"
                )
            )
        )
        exercicios.add(novoExercicio)
        notifyItemInserted(exercicios.size - 1)

        // Notificar que um novo exercício foi adicionado
        onExercicioAlterado?.invoke(novoExercicio, exercicios.size - 1)
    }

    fun removeExercicio(position: Int) {
        if (position >= 0 && position < exercicios.size) {
            exercicios.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, exercicios.size)
        }
    }

    // Método para salvar alterações pendentes
    fun salvarAlteracoesPendentes() {
        Log.d("EXERCICIO_ADAPTER", "Salvando alterações pendentes de exercícios")

        exercicios.forEachIndexed { index, exercicio ->
            onExercicioAlterado?.invoke(exercicio, index)
        }
    }
}




/*
package com.example.unipump.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.R
import com.example.unipump.models.exercicioFun
import com.example.unipump.models.serieFun

class exercicioFunAdapter(
    private val exercicios: MutableList<exercicioFun>,
    private val onDeleteExercicio: (Int) -> Unit,
    private val onAdicionarSerie: ((Int) -> Unit)? = null,
    private val onExercicioAlterado: ((exercicioFun, Int) -> Unit)? = null
) : RecyclerView.Adapter<exercicioFunAdapter.ExercicioViewHolder>() {

    class ExercicioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNomeExercicio: TextView = itemView.findViewById(R.id.tvNomeExercicio)
        val btnExcluirExercicio: ImageButton = itemView.findViewById(R.id.btnExcluirExercicio)
        val btnAddSerie: ImageButton = itemView.findViewById(R.id.btnAddSerie)
        val recyclerViewSeries: RecyclerView = itemView.findViewById(R.id.recyclerViewSeries)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExercicioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercicio_funcionario, parent, false)
        return ExercicioViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExercicioViewHolder, position: Int) {
        val exercicio = exercicios[position]

        holder.tvNomeExercicio.text = exercicio.nome

        // Configurar RecyclerView das séries
        val seriesAdapter = serieFunAdapter(
            series = exercicio.series,
            onDeleteSerie = { seriePosition: Int ->
                // Callback para deletar série
                removerSerie(position, seriePosition)
            },
            onSerieAlterada = { serieAlterada: serieFun, seriePosition: Int ->
                // Callback para quando série for alterada
                onSerieAlterada(position, serieAlterada, seriePosition)
            }
        )

        holder.recyclerViewSeries.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = seriesAdapter
            isNestedScrollingEnabled = false
        }

        // Configurar botão adicionar série
        holder.btnAddSerie.setOnClickListener {
            Log.d("ADD_SERIE", "Clicou em adicionar série no exercício: ${exercicio.nome}")
            adicionarSerie(position)
        }

        // Configurar botão excluir exercício
        holder.btnExcluirExercicio.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                onDeleteExercicio(currentPosition)
            }
        }
    }

    override fun getItemCount(): Int = exercicios.size

    // Método para adicionar série a um exercício específico
    private fun adicionarSerie(exercicioPosition: Int) {
        if (exercicioPosition >= 0 && exercicioPosition < exercicios.size) {
            val exercicio = exercicios[exercicioPosition]

            // Determinar o número da próxima série
            val proximoNumero = if (exercicio.series.isEmpty()) {
                1
            } else {
                exercicio.series.maxOf { it.numero } + 1
            }

            // Criar nova série
            val novaSerie = serieFun(
                id = "${exercicio.id}_serie_${System.currentTimeMillis()}",
                numero = proximoNumero,
                repeticoes = 12, // Valor padrão
                peso = "", // Vazio para o usuário preencher
                tempo = "60" // Tempo padrão de descanso
            )

            // Adicionar à lista de séries do exercício
            exercicio.series.add(novaSerie)

            Log.d("ADD_SERIE", "Série adicionada ao exercício ${exercicio.nome}. Total de séries: ${exercicio.series.size}")

            // Notificar que o exercício foi alterado
            onExercicioAlterado?.invoke(exercicio, exercicioPosition)

            // Atualizar o RecyclerView
            notifyItemChanged(exercicioPosition)

            // Callback para a activity principal (se fornecido)
            onAdicionarSerie?.invoke(exercicioPosition)
        }
    }

    // Método chamado quando uma série é alterada
    private fun onSerieAlterada(exercicioPosition: Int, serieAlterada: serieFun, seriePosition: Int) {
        if (exercicioPosition >= 0 && exercicioPosition < exercicios.size) {
            val exercicio = exercicios[exercicioPosition]

            // Atualizar a série na lista
            if (seriePosition >= 0 && seriePosition < exercicio.series.size) {
                exercicio.series[seriePosition] = serieAlterada

                Log.d("SERIE_ALTERADA", "Série alterada no exercício ${exercicio.nome}")

                // Notificar que o exercício foi alterado
                onExercicioAlterado?.invoke(exercicio, exercicioPosition)
            }
        }
    }

    // Remover série
    private fun removerSerie(exercicioPosition: Int, seriePosition: Int) {
        if (exercicioPosition >= 0 && exercicioPosition < exercicios.size) {
            val exercicio = exercicios[exercicioPosition]

            if (seriePosition >= 0 && seriePosition < exercicio.series.size) {
                exercicio.series.removeAt(seriePosition)

                // Reordenar números das séries restantes
                exercicio.series.forEachIndexed { index, serie ->
                    exercicio.series[index] = serie.copy(numero = index + 1)
                }

                Log.d("REMOVE_SERIE", "Série removida do exercício ${exercicio.nome}. Total de séries: ${exercicio.series.size}")

                // Notificar que o exercício foi alterado
                onExercicioAlterado?.invoke(exercicio, exercicioPosition)

                // Atualizar o RecyclerView
                notifyItemChanged(exercicioPosition)
            }
        }
    }

    // Métodos existentes mantidos
    fun addExercicio(nomeExercicio: String) {
        val novoExercicio = exercicioFun(
            id = "exercicio_${System.currentTimeMillis()}",
            nome = nomeExercicio,
            series = mutableListOf(
                serieFun(
                    id = "serie_${System.currentTimeMillis()}",
                    numero = 1,
                    repeticoes = 10,
                    peso = "",
                    tempo = "60"
                )
            )
        )
        exercicios.add(novoExercicio)
        notifyItemInserted(exercicios.size - 1)

        // Notificar que um novo exercício foi adicionado
        onExercicioAlterado?.invoke(novoExercicio, exercicios.size - 1)
    }

    fun removeExercicio(position: Int) {
        if (position >= 0 && position < exercicios.size) {
            exercicios.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, exercicios.size)
        }
    }

    // Método para salvar alterações pendentes
    fun salvarAlteracoesPendentes() {
        Log.d("EXERCICIO_ADAPTER", "Salvando alterações pendentes de exercícios")

        exercicios.forEachIndexed { index, exercicio ->
            onExercicioAlterado?.invoke(exercicio, index)
        }
    }
}*/
