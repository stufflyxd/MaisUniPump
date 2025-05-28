package com.example.unipump.models

// Em models/Aluno.kt
data class Aluno(
    val id: String,
    val nome: String,
    val sobrenome: String,
    val temDados: Boolean = true,
    val documentId: String = "",
    val email: String = "",
    val telefone: String = "",
    val idade: String = "",
    val genero: String = "",
    val endereco: String = "",
    val altura: Double = 0.0,
    val peso: Double = 0.0,
    val contusao: String = ""
)