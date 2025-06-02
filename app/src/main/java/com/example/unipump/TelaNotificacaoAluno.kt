package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.adapters.NotificacoesAlunoAdapter
import com.example.unipump.models.NotificacaoAlunoModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TelaNotificacaoAluno : BaseActivity() {

    private lateinit var btnSetaVoltar: ImageButton
    private lateinit var recyclerNotificacoes: RecyclerView
    private lateinit var textSemNotificacoes: TextView
    private lateinit var bottomNav: BottomNavigationView

    private val db = FirebaseFirestore.getInstance()
    private lateinit var notificacoesAlunoAdapter: NotificacoesAlunoAdapter
    private val notificacoesList = mutableListOf<NotificacaoAlunoModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_notificacao_aluno)

        btnSetaVoltar = findViewById(R.id.SetaVoltar)
        recyclerNotificacoes = findViewById(R.id.recyclerNotificacoes)
        textSemNotificacoes = findViewById(R.id.textSemNotificacoes)

        setupRecyclerView()
        carregarNotificacoes()

        btnSetaVoltar.setOnClickListener {
            finish()
        }

        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio ->
                    startActivity(Intent(this, TelaPrincipalAluno::class.java)).run { true }
                R.id.nav_treinos ->
                    startActivity(Intent(this, TelaTreinoAluno::class.java)).run {true}
                R.id.nav_chat ->
                    startActivity(Intent(this, TelaChat::class.java)).run { true }
                R.id.nav_config ->
                    startActivity(Intent(this, TelaConfig::class.java)).run { true }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        notificacoesAlunoAdapter = NotificacoesAlunoAdapter(notificacoesList) { notificacao ->
            marcarComoLida(notificacao)
        }
        recyclerNotificacoes.layoutManager = LinearLayoutManager(this)
        recyclerNotificacoes.adapter = notificacoesAlunoAdapter
    }

    private fun carregarNotificacoes() {
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val alunoId = prefs.getString("alunoDocId", null) ?: return

        db.collection("alunos")
            .document(alunoId)
            .collection("notificacoes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                notificacoesList.clear()
                snapshots?.documents?.forEach { doc ->
                    val notificacao = NotificacaoAlunoModel(
                        id = doc.id,
                        tipo = doc.getString("tipo") ?: "",
                        funcionarioId = doc.getString("funcionarioId") ?: "",
                        nomeFuncionario = doc.getString("nomeFuncionario") ?: "",
                        mensagem = doc.getString("mensagem") ?: "",
                        mensagemOriginal = doc.getString("mensagemOriginal") ?: "",
                        timestamp = doc.getTimestamp("timestamp"),
                        lida = doc.getBoolean("lida") ?: false
                    )
                    notificacoesList.add(notificacao)
                }

                notificacoesAlunoAdapter.notifyDataSetChanged()

                // Mostrar/ocultar mensagem de "sem notificações"
                if (notificacoesList.isEmpty()) {
                    textSemNotificacoes.visibility = View.VISIBLE
                    recyclerNotificacoes.visibility = View.GONE
                } else {
                    textSemNotificacoes.visibility = View.GONE
                    recyclerNotificacoes.visibility = View.VISIBLE
                }
            }
    }

    private fun marcarComoLida(notificacao: NotificacaoAlunoModel) {
        if (!notificacao.lida) {
            val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            val alunoId = prefs.getString("alunoDocId", null) ?: return

            db.collection("alunos")
                .document(alunoId)
                .collection("notificacoes")
                .document(notificacao.id)
                .update("lida", true)
        }
    }
}