package com.example.unipump.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.R
import com.example.unipump.models.NotificacaoAlunoModel
import java.text.SimpleDateFormat
import java.util.*

class NotificacoesAlunoAdapter(
    private val notificacoes: List<NotificacaoAlunoModel>,
    private val onItemClick: (NotificacaoAlunoModel) -> Unit
) : RecyclerView.Adapter<NotificacoesAlunoAdapter.NotificacaoAlunoViewHolder>() {

    class NotificacaoAlunoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textNomeFuncionario: TextView = itemView.findViewById(R.id.textNomeFuncionario)
        val textMensagem: TextView = itemView.findViewById(R.id.textMensagem)
        val textMensagemOriginal: TextView = itemView.findViewById(R.id.textMensagemOriginal)
        val textDataHora: TextView = itemView.findViewById(R.id.textDataHora)
        val indicadorLida: View = itemView.findViewById(R.id.indicadorLida)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacaoAlunoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacao_aluno, parent, false)
        return NotificacaoAlunoViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificacaoAlunoViewHolder, position: Int) {
        val notificacao = notificacoes[position]

        holder.textNomeFuncionario.text = notificacao.nomeFuncionario
        holder.textMensagem.text = notificacao.mensagem

        // Mostrar contexto da mensagem original
        if (notificacao.mensagemOriginal.isNotEmpty()) {
            holder.textMensagemOriginal.visibility = View.VISIBLE
            holder.textMensagemOriginal.text = "Ref: ${notificacao.mensagemOriginal}"
        } else {
            holder.textMensagemOriginal.visibility = View.GONE
        }

        // Formatar data e hora
        notificacao.timestamp?.let { timestamp ->
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            holder.textDataHora.text = sdf.format(timestamp.toDate())
        }

        // Indicador de lida/não lida
        holder.indicadorLida.visibility = if (notificacao.lida) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener {
            onItemClick(notificacao)
        }
    }

    override fun getItemCount(): Int = notificacoes.size
}