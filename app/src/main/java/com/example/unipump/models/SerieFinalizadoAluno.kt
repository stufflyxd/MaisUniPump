package com.example.unipump.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SerieFinalizadoAluno(
    val ordem: String = "",
    var reps: String = "",
    var peso: String = "",
    var descanso: String = "",
    var feito: Boolean,
    val duracao: String = ""
) : Parcelable