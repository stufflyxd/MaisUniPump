
package com.example.unipump.models

data class ExercicioAluno(
    val frame: String = "",
    var execucao: String = "",
    var nome: String = "",
    var series: List<SerieAluno> = emptyList()
)