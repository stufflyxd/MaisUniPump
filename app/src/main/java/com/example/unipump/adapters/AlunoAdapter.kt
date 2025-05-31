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
import com.bumptech.glide.Glide
import com.example.unipump.models.Aluno
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class AlunoAdapter(
    private val listaAlunos: MutableList<Aluno>,
    private val onAlunoClick: (Aluno) -> Unit
) : RecyclerView.Adapter<AlunoAdapter.AlunoViewHolder>() {

    // Firestore para verificar fichas e carregar fotos
    private val db = FirebaseFirestore.getInstance()

    // Cache para evitar consultas repetidas
    private val cacheTemFichas = mutableMapOf<String, Boolean>()
    private val cacheFotos = mutableMapOf<String, String?>() // Cache das URLs/caminhos das fotos

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

        // NOVO: Carregar foto do aluno
        carregarFotoAluno(aluno.documentId, holder.imgAluno)

        // Verificar se aluno tem fichas e aplicar cor de fundo
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

    // NOVA FUNÇÃO: Carregar foto do aluno
    private fun carregarFotoAluno(alunoDocId: String, imageView: ImageView) {
        // Verificar cache primeiro
        if (cacheFotos.containsKey(alunoDocId)) {
            val fotoPath = cacheFotos[alunoDocId]
            aplicarFoto(fotoPath, imageView, alunoDocId)
            return
        }

        // Consultar Firestore para obter caminho da foto
        db.collection("alunos")
            .document(alunoDocId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val fotoPath = document.getString("uri_foto") // Para fotos locais
                    val fotoUrl = document.getString("foto_url")   // Para Firebase Storage (se implementado)

                    // Priorizar Firebase Storage se existir, senão usar local
                    val caminhoFoto = fotoUrl ?: fotoPath

                    // Salvar no cache
                    cacheFotos[alunoDocId] = caminhoFoto

                    Log.d("FOTO_ALUNO", "Foto do aluno $alunoDocId: $caminhoFoto")
                    aplicarFoto(caminhoFoto, imageView, alunoDocId)
                } else {
                    Log.w("FOTO_ALUNO", "Documento do aluno $alunoDocId não encontrado")
                    cacheFotos[alunoDocId] = null
                    aplicarFoto(null, imageView, alunoDocId)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FOTO_ALUNO", "Erro ao carregar foto do aluno $alunoDocId", e)
                cacheFotos[alunoDocId] = null
                aplicarFoto(null, imageView, alunoDocId)
            }
    }

    // NOVA FUNÇÃO: Aplicar foto no ImageView
    private fun aplicarFoto(caminhoFoto: String?, imageView: ImageView, alunoDocId: String) {
        val context = imageView.context

        if (caminhoFoto.isNullOrBlank()) {
            // Sem foto - usar imagem padrão
            imageView.setImageResource(R.drawable.ic_person)
            Log.d("FOTO_ALUNO", "Aluno $alunoDocId sem foto - usando ic_person")
            return
        }

        try {
            if (caminhoFoto.startsWith("http")) {
                // É uma URL do Firebase Storage ou web
                Glide.with(context)
                    .load(caminhoFoto)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person) // Placeholder enquanto carrega
                    .error(R.drawable.ic_person) // Imagem padrão se der erro
                    .into(imageView)
                Log.d("FOTO_ALUNO", "Foto carregada via URL para aluno $alunoDocId")

            } else {
                // É um caminho local
                val file = File(caminhoFoto)
                if (file.exists()) {
                    Glide.with(context)
                        .load(file)
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(imageView)
                    Log.d("FOTO_ALUNO", "Foto local carregada para aluno $alunoDocId")
                } else {
                    // Arquivo não existe (dispositivo diferente)
                    imageView.setImageResource(R.drawable.ic_person)
                    Log.w("FOTO_ALUNO", "Arquivo local não encontrado para aluno $alunoDocId: $caminhoFoto")
                }
            }

        } catch (e: Exception) {
            Log.e("FOTO_ALUNO", "Erro ao carregar foto do aluno $alunoDocId", e)
            imageView.setImageResource(R.drawable.ic_person)
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
        cacheFotos.clear() // Limpar cache de fotos também
        Log.d("CACHE_FICHAS", "Cache de fichas e fotos limpo")
    }

    // Método para forçar atualização de um aluno específico
    fun atualizarAluno(position: Int) {
        if (position >= 0 && position < listaAlunos.size) {
            val aluno = listaAlunos[position]
            // Remover do cache para forçar nova consulta
            cacheTemFichas.remove(aluno.documentId)
            cacheFotos.remove(aluno.documentId) // Remover foto do cache também
            notifyItemChanged(position)
        }
    }

    // NOVO: Método para atualizar foto de um aluno específico
    fun atualizarFotoAluno(alunoDocId: String) {
        // Remover foto do cache
        cacheFotos.remove(alunoDocId)

        // Encontrar posição do aluno na lista
        val position = listaAlunos.indexOfFirst { it.documentId == alunoDocId }
        if (position != -1) {
            notifyItemChanged(position)
            Log.d("FOTO_ALUNO", "Foto do aluno $alunoDocId atualizada")
        }
    }
}