package com.example.unipump.models

data class fichaTreinoFun(
    val id: String = "",
    var letra: String = "",
    var nome: String = "",
    val exercicios: MutableList<exercicioFun> = mutableListOf()
)