package com.example.unipump.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.unipump.R

data class ExercicioGrupo(
    val id: String,
    val nome: String,
    val frame: String,
    val grupoMuscular: String
)

class ExerciciosGrupoAdapter(
    private val exercicios: MutableList<ExercicioGrupo>,
    private val onExercicioClick: (ExercicioGrupo) -> Unit
) : RecyclerView.Adapter<ExerciciosGrupoAdapter.ExercicioViewHolder>() {

    class ExercicioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNomeExercicio: TextView = itemView.findViewById(R.id.tvNomeExercicio)
        val btnAddExercicio: ImageButton = itemView.findViewById(R.id.btnAddExercicio)

        // NOVO: ImageView para a imagem do exercício
        val imgExercicio: ImageView = itemView.findViewById(R.id.imgExercicio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExercicioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercicio_grupo, parent, false)
        return ExercicioViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExercicioViewHolder, position: Int) {
        val exercicio = exercicios[position]

        // Configurar nome do exercício
        holder.tvNomeExercicio.text = exercicio.nome

        // NOVO: Carregar imagem do exercício com Glide
        carregarImagemExercicio(holder, exercicio)

        // Configurar click do botão
        holder.btnAddExercicio.setOnClickListener {
            Log.d("EXERCICIO_GRUPO_CLICK", "Exercício selecionado: ${exercicio.nome}")
            onExercicioClick(exercicio)
        }

        // OPCIONAL: Click no item inteiro também
        holder.itemView.setOnClickListener {
            Log.d("EXERCICIO_GRUPO_CLICK", "Item exercício clicado: ${exercicio.nome}")
            onExercicioClick(exercicio)
        }
    }

    // NOVO: Método para carregar imagem do exercício
    private fun carregarImagemExercicio(holder: ExercicioViewHolder, exercicio: ExercicioGrupo) {
        val context = holder.itemView.context

        Log.d("GLIDE_GRUPO", "=== CARREGANDO IMAGEM DO GRUPO ===")
        Log.d("GLIDE_GRUPO", "Exercício: ${exercicio.nome}")
        Log.d("GLIDE_GRUPO", "Frame URL: '${exercicio.frame}'")
        Log.d("GLIDE_GRUPO", "Grupo: ${exercicio.grupoMuscular}")

        if (exercicio.frame.isNotEmpty()) {
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

                Log.d("GLIDE_GRUPO", "✅ Glide configurado para carregar: ${exercicio.frame}")

            } catch (e: Exception) {
                Log.e("GLIDE_GRUPO", "❌ Erro ao configurar Glide para ${exercicio.nome}", e)
                // Em caso de erro, usar imagem padrão
                holder.imgExercicio.setImageResource(R.drawable.icon_rectangle)
            }
        } else {
            Log.d("GLIDE_GRUPO", "⚠️ Frame URL vazio para ${exercicio.nome}, usando imagem padrão")
            // Se não há URL, usar imagem padrão
            holder.imgExercicio.setImageResource(R.drawable.icon_rectangle)
        }
    }

    override fun getItemCount(): Int = exercicios.size

    // Método para atualizar lista de exercícios
    fun updateExercicios(novosExercicios: List<ExercicioGrupo>) {
        Log.d("EXERCICIOS_GRUPO_ADAPTER", "Atualizando lista com ${novosExercicios.size} exercícios")

        exercicios.clear()
        exercicios.addAll(novosExercicios)
        notifyDataSetChanged()

        // Log dos exercícios carregados
        novosExercicios.forEachIndexed { index, exercicio ->
            Log.d("EXERCICIOS_GRUPO_ADAPTER", "Exercício $index: ${exercicio.nome} (Frame: ${exercicio.frame})")
        }
    }

    // Método para limpar exercícios
    fun clearExercicios() {
        Log.d("EXERCICIOS_GRUPO_ADAPTER", "Limpando lista de exercícios")
        exercicios.clear()
        notifyDataSetChanged()
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
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.R

// Classe simples para representar exercícios dos grupos musculares
data class ExercicioGrupo(
    val id: String = "",
    val nome: String = "",
    val frame: String = "",
    val grupoMuscular: String = ""
)

class ExerciciosGrupoAdapter(
    private val exercicios: MutableList<ExercicioGrupo>,
    private val onExercicioClick: (ExercicioGrupo) -> Unit
) : RecyclerView.Adapter<ExerciciosGrupoAdapter.ExercicioViewHolder>() {

    inner class ExercicioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNomeExercicio: TextView = itemView.findViewById(R.id.tvNomeExercicio)
        val btnAddExercicio: ImageButton = itemView.findViewById(R.id.btnAddExercicio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExercicioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercicio, parent, false)
        return ExercicioViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExercicioViewHolder, position: Int) {
        val exercicio = exercicios[position]

        holder.tvNomeExercicio.text = exercicio.nome

        // MELHORIA: Adicionar logs para debug
        holder.btnAddExercicio.setOnClickListener {
            Log.d("EXERCICIO_ADAPTER", "Botão adicionar clicado para: ${exercicio.nome}")
            Log.d("EXERCICIO_ADAPTER", "Grupo muscular: ${exercicio.grupoMuscular}")

            // Chamar callback
            onExercicioClick(exercicio)
        }

        // MELHORIA: Remover click no item inteiro para evitar duplos clicks
        // Manter apenas o botão como clicável para UX mais clara
        holder.itemView.setOnClickListener(null)

        // OPCIONAL: Adicionar efeito visual no botão
        holder.btnAddExercicio.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.7f
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1.0f
                }
            }
            false
        }
    }

    override fun getItemCount(): Int = exercicios.size

    // Método para atualizar a lista de exercícios
    fun updateExercicios(novosExercicios: List<ExercicioGrupo>) {
        Log.d("EXERCICIO_ADAPTER", "Atualizando lista com ${novosExercicios.size} exercícios")
        exercicios.clear()
        exercicios.addAll(novosExercicios)
        notifyDataSetChanged()
    }

    // Método para limpar a lista
    fun clearExercicios() {
        Log.d("EXERCICIO_ADAPTER", "Limpando lista de exercícios")
        exercicios.clear()
        notifyDataSetChanged()
    }

    // NOVO: Método para obter exercício por posição (útil para debug)
    fun getExercicioAt(position: Int): ExercicioGrupo? {
        return if (position in 0 until exercicios.size) {
            exercicios[position]
        } else {
            null
        }
    }
}*/
