package com.example.unipump

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.models.NotificacaoModel
import java.text.SimpleDateFormat
import java.util.*

class NotificacoesAdapter(
    private val notificacoes: List<NotificacaoModel>,
    private val onItemClick: (NotificacaoModel) -> Unit,
    private val onResponderClick: (NotificacaoModel) -> Unit
) : RecyclerView.Adapter<NotificacoesAdapter.NotificacaoViewHolder>() {

    class NotificacaoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textNomeAluno: TextView = itemView.findViewById(R.id.textNomeAluno)
        val textMensagem: TextView = itemView.findViewById(R.id.textMensagem)
        val textDataHora: TextView = itemView.findViewById(R.id.textDataHora)
        val indicadorLida: View = itemView.findViewById(R.id.indicadorLida)
        val btnResponder: Button = itemView.findViewById(R.id.btnResponder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacaoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacao, parent, false)
        return NotificacaoViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificacaoViewHolder, position: Int) {
        val notificacao = notificacoes[position]

        holder.textNomeAluno.text = notificacao.nomeAluno
        holder.textMensagem.text = notificacao.mensagem

        // Formatar data e hora
        notificacao.timestamp?.let { timestamp ->
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            holder.textDataHora.text = sdf.format(timestamp.toDate())
        }

        // Indicador de lida/não lida
        holder.indicadorLida.visibility = if (notificacao.lida) View.GONE else View.VISIBLE

        // Mostrar botão "Responder" apenas para solicitações de ficha
        if (notificacao.tipo == "solicitacao_ficha") {
            holder.btnResponder.visibility = View.VISIBLE
            holder.btnResponder.setOnClickListener {
                onResponderClick(notificacao)
            }
        } else {
            holder.btnResponder.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(notificacao)
        }
    }

    override fun getItemCount(): Int = notificacoes.size
}