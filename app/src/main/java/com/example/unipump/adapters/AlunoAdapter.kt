package com.example.unipump

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.models.Aluno

class AlunoAdapter(
    private val alunos: List<Aluno>,
    private val onAlunoClick: (Aluno) -> Unit
) : RecyclerView.Adapter<AlunoAdapter.AlunoViewHolder>() {

    class AlunoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val containerAluno: LinearLayout = itemView.findViewById(R.id.containerAluno)
        val txtNomeAluno: TextView = itemView.findViewById(R.id.txtNomeAluno)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlunoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_aluno, parent, false)
        return AlunoViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlunoViewHolder, position: Int) {
        val aluno = alunos[position]

        holder.txtNomeAluno.text = aluno.nome

        holder.containerAluno.setOnClickListener {
            onAlunoClick(aluno)
        }
    }

    override fun getItemCount(): Int = alunos.size
}