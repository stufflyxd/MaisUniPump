package com.example.unipump.models

data class Exercicio(
    val frame: String = "",
    var execucao: String = "",
    var nome: String = "",
    val series: List<Serie> // Correto
)
