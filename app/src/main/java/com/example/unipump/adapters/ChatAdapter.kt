package com.example.unipump.adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.R
import com.example.unipump.models.ChatMessage

class ChatAdapter(
    private val mensagens: MutableList<ChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_USER = 1
        const val VIEW_TYPE_BOT = 2
    }

    // ViewHolder para mensagens do usuário
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMensagem: TextView = itemView.findViewById(R.id.tvMensagemUser)
    }

    // ViewHolder para mensagens da IA
    class BotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMensagem: TextView = itemView.findViewById(R.id.tvMensagemBot)
    }

    override fun getItemViewType(position: Int): Int {
        return if (mensagens[position].isUsuario) VIEW_TYPE_USER else VIEW_TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_mensagem_user, parent, false)
                UserViewHolder(view)
            }
            VIEW_TYPE_BOT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_mensagem_bot, parent, false)
                BotViewHolder(view)
            }
            else -> throw IllegalArgumentException("Tipo de view inválido")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mensagem = mensagens[position]

        when (holder) {
            is UserViewHolder -> {
                holder.tvMensagem.text = mensagem.mensagem
            }
            is BotViewHolder -> {
                holder.tvMensagem.text = mensagem.mensagem
            }
        }
    }

    override fun getItemCount(): Int = mensagens.size

    fun adicionarMensagem(mensagem: ChatMessage) {
        mensagens.add(mensagem)
        notifyItemInserted(mensagens.size - 1)
    }
}