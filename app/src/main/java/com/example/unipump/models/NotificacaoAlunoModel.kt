package com.example.unipump.models

import com.google.firebase.Timestamp

data class NotificacaoAlunoModel(
    val id: String = "",
    val tipo: String = "",
    val funcionarioId: String = "",
    val nomeFuncionario: String = "",
    val mensagem: String = "",
    val mensagemOriginal: String = "",
    val timestamp: Timestamp? = null,
    val lida: Boolean = false
)