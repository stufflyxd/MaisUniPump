package com.example.unipump

import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.models.Aluno
import com.google.firebase.firestore.FirebaseFirestore

class AlunoAdapter(
    private val listaAlunos: MutableList<Aluno>,
    private val onAlunoClick: (Aluno) -> Unit
) : RecyclerView.Adapter<AlunoAdapter.AlunoViewHolder>() {

    // Firestore para verificar fichas
    private val db = FirebaseFirestore.getInstance()

    // Cache para evitar consultas repetidas
    private val cacheTemFichas = mutableMapOf<String, Boolean>()

    class AlunoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val containerAluno: LinearLayout = itemView.findViewById(R.id.containerAluno)
        val imgAluno: ImageView = itemView.findViewById(R.id.imgAluno)
        val txtNomeAluno: TextView = itemView.findViewById(R.id.txtNomeAluno)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlunoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_aluno, parent, false)
        return AlunoViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlunoViewHolder, position: Int) {
        val aluno = listaAlunos[position]

        // Configurar nome
        holder.txtNomeAluno.text = aluno.nome

        // Verificar se aluno tem fichas e aplicar cor
        verificarSeTemFichas(aluno.documentId) { temFichas ->
            // Executar na thread principal
            Handler(Looper.getMainLooper()).post {
                val context = holder.itemView.context
                if (temFichas) {
                    // 🔵 AZUL = Aluno com fichas
                    holder.imgAluno.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.blue)
                    )
                    Log.d("VISUAL_ALUNO", "✅ ${aluno.nome} - TEM fichas (azul)")
                } else {
                    // 🟢 VERDE = Aluno sem fichas
                    holder.imgAluno.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.green)
                    )
                    Log.d("VISUAL_ALUNO", "🆕 ${aluno.nome} - SEM fichas (verde)")
                }
            }
        }

        // Click do aluno
        holder.containerAluno.setOnClickListener {
            Log.d("ALUNO_CLICK", "Aluno clicado: ${aluno.nome}")
            onAlunoClick(aluno)
        }
    }

    private fun verificarSeTemFichas(alunoDocId: String, callback: (Boolean) -> Unit) {
        // Verificar cache primeiro
        if (cacheTemFichas.containsKey(alunoDocId)) {
            callback(cacheTemFichas[alunoDocId] == true)
            return
        }

        // Consultar Firestore
        db.collection("alunos")
            .document(alunoDocId)
            .collection("treino")
            .limit(1) // Só precisamos saber se existe pelo menos 1
            .get()
            .addOnSuccessListener { documents ->
                val temFichas = !documents.isEmpty

                // Salvar no cache
                cacheTemFichas[alunoDocId] = temFichas

                Log.d("VERIFICAR_FICHAS", "Aluno $alunoDocId: ${if (temFichas) "TEM" else "NÃO TEM"} fichas")
                callback(temFichas)
            }
            .addOnFailureListener { e ->
                Log.e("VERIFICAR_FICHAS", "Erro ao verificar fichas do aluno $alunoDocId", e)
                // Em caso de erro, assumir que não tem fichas
                cacheTemFichas[alunoDocId] = false
                callback(false)
            }
    }

    override fun getItemCount(): Int = listaAlunos.size

    // Método para limpar cache quando necessário
    fun limparCache() {
        cacheTemFichas.clear()
        Log.d("CACHE_FICHAS", "Cache de fichas limpo")
    }

    // Método para forçar atualização de um aluno específico
    fun atualizarAluno(position: Int) {
        if (position >= 0 && position < listaAlunos.size) {
            val aluno = listaAlunos[position]
            // Remover do cache para forçar nova consulta
            cacheTemFichas.remove(aluno.documentId)
            notifyItemChanged(position)
        }
    }
}