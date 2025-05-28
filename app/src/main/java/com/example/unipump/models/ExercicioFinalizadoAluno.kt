
package com.example.unipump.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import com.example.unipump.objeto.SerieParcelerAluno

@Parcelize
@TypeParceler<SerieFinalizadoAluno, SerieParcelerAluno>()
data class ExercicioFinalizadoAluno(
    val frame: String = "",
    var execucao: String = "",
    var nome: String = "",
    var series: List<SerieFinalizadoAluno> = emptyList()
): Parcelable