package com.example.unipump.models

import com.google.firebase.Timestamp

data class NotificacaoModel(
    val id: String = "",
    val tipo: String = "",
    val alunoId: String = "",
    val nomeAluno: String = "",
    val mensagem: String = "",
    val timestamp: Timestamp? = null,
    val lida: Boolean = false
)