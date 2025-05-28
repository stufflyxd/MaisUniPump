package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class TelaGerenciamentoAlunoNovo_Funcionario : AppCompatActivity() {

    private lateinit var btnSetaVoltar : ImageButton
    private lateinit var btnNavegacao : BottomNavigationView
    private lateinit var linkMaisDetalhes: TextView
    private lateinit var linkCriar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_gerenciamento_aluno_novo_funcionario)

        linkMaisDetalhes = findViewById(R.id.link_mais_detalhes)
        btnSetaVoltar = findViewById(R.id.SetaVoltarTelaGerenciamentoAluno)
        btnNavegacao = findViewById(R.id.bottom_navigation)
        linkCriar = findViewById(R.id.link_criar)

        configurarEventos()
    }

    fun configurarEventos() {

        linkMaisDetalhes.setOnClickListener{
            val intent = Intent(this, TelaDetalhesUsuario_Funcionario::class.java)
            startActivity(intent)

        }

        linkCriar.setOnClickListener{
            val intent = Intent(this, TelaEdicaoFichaTreino_funcionario::class.java)
            startActivity(intent)

        }

        btnSetaVoltar.setOnClickListener {
            val intent = Intent(this, TelaFuncionario::class.java)
            startActivity(intent)
        }


        btnNavegacao.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    // O que acontece quando o item "Início" é clicado
                    val intent = Intent(this, TelaFuncionario::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_chat -> {
                    // Abre a tela de chat
                    val intent = Intent(this, TelaChat::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_config -> {
                    // Abre a tela de configurações
                    val intent = Intent(this, TelaConfiguracao_Funcionario::class.java)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }

    }


}