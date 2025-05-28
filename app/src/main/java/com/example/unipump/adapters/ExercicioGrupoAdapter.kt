package com.example.unipump.adapters

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

        holder.btnAddExercicio.setOnClickListener {
            onExercicioClick(exercicio)
        }

        // Opcional: fazer o item inteiro clicável
        holder.itemView.setOnClickListener {
            onExercicioClick(exercicio)
        }
    }

    override fun getItemCount(): Int = exercicios.size

    // Método para atualizar a lista de exercícios
    fun updateExercicios(novosExercicios: List<ExercicioGrupo>) {
        exercicios.clear()
        exercicios.addAll(novosExercicios)
        notifyDataSetChanged()
    }

    // Método para limpar a lista
    fun clearExercicios() {
        exercicios.clear()
        notifyDataSetChanged()
    }
}