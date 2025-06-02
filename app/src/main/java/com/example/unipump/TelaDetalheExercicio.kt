// app/src/main/java/com/example/unipump/TelaDetalheExercicio.kt

package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.unipump.adapters.SeriesAdapterFinalizadoAluno
import com.example.unipump.models.ExercicioFinalizadoAluno

class TelaDetalheExercicio : BaseActivity() {

    // Guarda a posição que veio da tela anterior
    private var posicaoRecebida: Int = -1

    // O exercício que veio da intent. Será um Parcelable.
    private lateinit var exercicioAtual: ExercicioFinalizadoAluno

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_detalhe_exercicio)

        // ===== 1) Botões de “voltar” na tela de detalhe =====
        // Quando clicado, chama retornarComResultado()
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            retornarComResultado()
        }
        findViewById<Button>(R.id.btnVoltarBase).setOnClickListener {
            retornarComResultado()
        }

        // ===== 2) Recupera os extras: posição e exercício =====
        posicaoRecebida = intent.getIntExtra("EXTRA_POSICAO", -1)
        val extraExercicio = intent.getParcelableExtra<ExercicioFinalizadoAluno>("EXTRA_EXERCICIO")
        if (posicaoRecebida < 0 || extraExercicio == null) {
            // Se faltar dados, fecha a Activity
            finish()
            return
        }
        exercicioAtual = extraExercicio

        // ===== 3) Carregar imagem do exercício no container (se houver) =====
        carregarImagemExercicio(exercicioAtual)

        // ===== 4) Preencher o cabeçalho dentro do CardView =====
        findViewById<TextView>(R.id.tvTituloExercicio).text = exercicioAtual.nome
        // Pode-se exibir a execução textual ou fixa. Aqui deixamos do jeito que você tinha:
        findViewById<TextView>(R.id.tvLabelTempoExecucao).text = "Execução normal"
        findViewById<TextView>(R.id.tvTempoExercicio).text = exercicioAtual.execucao

        // ===== 5) Configurar RecyclerView de séries (dentro do card) =====
        // O Adapter de séries já atualizará o próprio objeto exercicioAtual.series[].feito
        val rvSeriesDetalhe = findViewById<RecyclerView>(R.id.rvSeriesDetalhe)
        rvSeriesDetalhe.layoutManager = LinearLayoutManager(this)
        rvSeriesDetalhe.adapter = SeriesAdapterFinalizadoAluno(exercicioAtual.series)
    }

    /**
     * Método para carregar a imagem (frame) do exercício no ImageView (fullscreen ou card).
     */
    private fun carregarImagemExercicio(exercicio: ExercicioFinalizadoAluno) {
        val ivMediaPlaceholder = findViewById<ImageView>(R.id.ivMediaPlaceholder)

        Log.d("GLIDE_DETALHE_EXERCICIO", "=== CARREGANDO IMAGEM DO EXERCÍCIO ===")
        Log.d("GLIDE_DETALHE_EXERCICIO", "Exercício: ${exercicio.nome}")
        Log.d("GLIDE_DETALHE_EXERCICIO", "Frame URL: '${exercicio.frame}'")
        Log.d("GLIDE_DETALHE_EXERCICIO", "Execução: ${exercicio.execucao}")

        if (exercicio.frame.isNotEmpty()) {
            try {
                val requestOptions = RequestOptions()
                    .placeholder(R.drawable.icon_rectangle)
                    .error(R.drawable.icon_rectangle)
                    .fallback(R.drawable.icon_rectangle)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()

                Glide.with(this)
                    .load(exercicio.frame)
                    .apply(requestOptions)
                    .into(ivMediaPlaceholder)

                Log.d("GLIDE_DETALHE_EXERCICIO", "✅ Glide carregou: ${exercicio.frame}")

            } catch (e: Exception) {
                Log.e("GLIDE_DETALHE_EXERCICIO", "❌ Erro ao carregar: ${exercicio.nome}", e)
                ivMediaPlaceholder.setImageResource(R.drawable.icon_rectangle)
            }
        } else {
            Log.d("GLIDE_DETALHE_EXERCICIO", "⚠️ Frame vazio para ${exercicio.nome}, usando placeholder")
            ivMediaPlaceholder.setImageResource(R.drawable.icon_rectangle)
        }
    }

    /**
     * Chama-se este método sempre que se deseja retornar para a tela anterior
     * levando de volta a posição e o objeto ExercicioFinalizadoAluno atualizado.
     */
    private fun retornarComResultado() {
        val data = Intent().apply {
            putExtra("EXTRA_POSICAO", posicaoRecebida)
            putExtra("EXTRA_EXERCICIO_ATUALIZADO", exercicioAtual)
        }
        setResult(RESULT_OK, data)
        finish()
    }

    /**
     * Garante que pressionar o botão “back” (físico ou de sistema) também retorne o objeto modificado.
     */
    override fun onBackPressed() {
        super.onBackPressed()
        retornarComResultado()
    }
}
