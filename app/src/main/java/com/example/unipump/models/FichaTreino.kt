package com.example.unipump.models

data class FichaTreino(
    var documentId: String = "", // NOVO: ID do documento no Firestore
    var letra: String,
    var nome: String,
    var descricao: String,
    var quantidadeExercicios: Int,
    /*var tempoEstimado: Int // em minutos*/
) {
    // Método para formatação da descrição
    fun getDescricaoFormatada(): String {
        return "$quantidadeExercicios exercícios"
    }
}