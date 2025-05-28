package com.example.unipump

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.adapters.SeriesAdapterFinalizadoAluno
import com.example.unipump.models.ExercicioFinalizadoAluno

class TelaDetalheExercicio : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_detalhe_exercicio)

        // 1) Botões de voltar
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnVoltarBase).setOnClickListener { finish() }

        // 2) Recupera o Exercício
        val exercicio = intent.getParcelableExtra<ExercicioFinalizadoAluno>("EXERCICIO_DETALHE")
            ?: return finish()

        // 3) Header dentro do CardView
        findViewById<TextView>(R.id.tvTituloExercicio).text           = exercicio.nome
        findViewById<TextView>(R.id.tvLabelTempoExecucao).text       = "Execução normal"
        findViewById<TextView>(R.id.tvTempoExercicio).text           = exercicio.execucao

        // 4) RecyclerView de séries (dentro do card)
        val rv = findViewById<RecyclerView>(R.id.rvSeriesDetalhe)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter       = SeriesAdapterFinalizadoAluno(exercicio.series)
    }

}
