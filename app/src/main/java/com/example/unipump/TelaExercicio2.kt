package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class TelaExercicio2 : AppCompatActivity() {

    private lateinit var voltar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_exercicio2)

        voltar = findViewById(R.id.btnVoltar)
        onClickFinalizar()
    }


    private fun onClickFinalizar(){
        voltar.setOnClickListener {
            val intent = Intent(this, TelaExercicioFinalizadoAluno:: class.java)
            startActivity(intent)
        }
    }
}