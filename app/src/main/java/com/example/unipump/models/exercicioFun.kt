package com.example.unipump.models

data class exercicioFun(
    val id: String = "",
    val nome: String = "",
    val series: MutableList<serieFun> = mutableListOf()
)