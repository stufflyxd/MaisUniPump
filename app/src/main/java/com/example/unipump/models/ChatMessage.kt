package com.example.unipump.models

data class ChatMessage(
    val mensagem: String,
    val isUsuario: Boolean, // true = usuário, false = IA
    val timestamp: Long = System.currentTimeMillis()
)