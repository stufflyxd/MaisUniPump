package com.example.unipump.objeto

import android.os.Parcel
import com.example.unipump.models.SerieFinalizadoAluno
import kotlinx.parcelize.Parceler

object SerieParcelerAluno : Parceler<SerieFinalizadoAluno> {
    override fun create(parcel: Parcel): SerieFinalizadoAluno {
        return SerieFinalizadoAluno(
            ordem = parcel.readString() ?: "",
            reps = parcel.readString() ?: "",
            peso = parcel.readString() ?: "",
            descanso = parcel.readString() ?: "",
            feito = parcel.readByte() != 0.toByte(),
            duracao = parcel.readString() ?: ""
        )
    }

    override fun SerieFinalizadoAluno.write(parcel: Parcel, flags: Int) {
        parcel.writeString(ordem)
        parcel.writeString(reps)
        parcel.writeString(peso)
        parcel.writeString(descanso)
        parcel.writeByte(if (feito) 1 else 0)
        parcel.writeString(duracao)
    }
}
