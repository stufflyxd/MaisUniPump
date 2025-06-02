package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.models.NotificacaoModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TelaNotificacao_funcionario : BaseActivity() {

    private lateinit var btnSetaVoltar: ImageButton
    private lateinit var btnNavegacao: BottomNavigationView
    private lateinit var recyclerNotificacoes: RecyclerView
    private lateinit var textSemNotificacoes: TextView

    private val db = FirebaseFirestore.getInstance()
    private lateinit var notificacoesAdapter: NotificacoesAdapter
    private val notificacoesList = mutableListOf<NotificacaoModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_notificacao_funcionario)

        btnSetaVoltar = findViewById(R.id.SetaVoltar)
        recyclerNotificacoes = findViewById(R.id.recyclerNotificacoes)
        textSemNotificacoes = findViewById(R.id.textSemNotificacoes)
        btnNavegacao = findViewById(R.id.bottom_navigation)

        setupRecyclerView()
        carregarNotificacoes()


        btnNavegacao.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio ->
                    startActivity(Intent(this, TelaFuncionario::class.java)).run { true }
                R.id.nav_chat ->
                    startActivity(Intent(this, TelaChat::class.java)).run { true }
                R.id.nav_config ->
                    startActivity(Intent(this, TelaConfiguracao_Funcionario::class.java)).run { true }
                else -> false
            }
        }

        // Definindo o clique do botão de voltar
        btnSetaVoltar.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        notificacoesAdapter = NotificacoesAdapter(
            notificacoesList,
            onItemClick = { notificacao ->
                marcarComoLida(notificacao)
            },
            onResponderClick = { notificacao ->
                mostrarDialogResposta(notificacao)
            }
        )
        recyclerNotificacoes.layoutManager = LinearLayoutManager(this)
        recyclerNotificacoes.adapter = notificacoesAdapter
    }

    private fun carregarNotificacoes() {
        db.collection("notificacoes_funcionario")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                notificacoesList.clear()
                snapshots?.documents?.forEach { doc ->
                    val notificacao = NotificacaoModel(
                        id = doc.id,
                        tipo = doc.getString("tipo") ?: "",
                        alunoId = doc.getString("alunoId") ?: "",
                        nomeAluno = doc.getString("nomeAluno") ?: "",
                        mensagem = doc.getString("mensagem") ?: "",
                        timestamp = doc.getTimestamp("timestamp"),
                        lida = doc.getBoolean("lida") ?: false
                    )
                    notificacoesList.add(notificacao)
                }

                notificacoesAdapter.notifyDataSetChanged()

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

    private fun marcarComoLida(notificacao: NotificacaoModel) {
        if (!notificacao.lida) {
            db.collection("notificacoes_funcionario")
                .document(notificacao.id)
                .update("lida", true)
        }
    }

    private fun mostrarDialogResposta(notificacao: NotificacaoModel) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_resposta_funcionario, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        val editMensagem = dialogView.findViewById<EditText>(R.id.editMensagem)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelar)
        val btnEnviar = dialogView.findViewById<Button>(R.id.btnEnviar)
        val textTitulo = dialogView.findViewById<TextView>(R.id.textTitulo)

        textTitulo.text = "Responder para ${notificacao.nomeAluno}"

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnEnviar.setOnClickListener {
            val mensagem = editMensagem.text.toString().trim()
            if (mensagem.isNotEmpty()) {
                enviarRespostaParaAluno(notificacao, mensagem)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Digite uma mensagem", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enviarRespostaParaAluno(notificacaoOriginal: NotificacaoModel, mensagem: String) {
        // Buscar dados do funcionário logado
        val funcionarioPrefs = getSharedPreferences("funcionarioPrefs", MODE_PRIVATE)
        val funcionarioDocId = funcionarioPrefs.getString("funcionarioDocId", null)

        if (funcionarioDocId.isNullOrBlank()) {
            Toast.makeText(this, "Erro: Funcionário não identificado", Toast.LENGTH_SHORT).show()
            return
        }

        // Buscar nome do funcionário no Firestore
        db.collection("funcionarios").document(funcionarioDocId)
            .get()
            .addOnSuccessListener { funcionarioDoc ->
                val nomeFuncionario = funcionarioDoc.getString("nome") ?: "Personal Trainer"

                // Criar notificação para o aluno
                val notificacaoAluno = hashMapOf(
                    "tipo" to "resposta_funcionario",
                    "funcionarioId" to funcionarioDocId,
                    "nomeFuncionario" to nomeFuncionario,
                    "mensagem" to mensagem,
                    "mensagemOriginal" to "Solicitação de nova ficha de treino",
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "lida" to false
                )

                // Salvar na coleção específica do aluno
                db.collection("alunos")
                    .document(notificacaoOriginal.alunoId)
                    .collection("notificacoes")
                    .add(notificacaoAluno)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Resposta enviada para ${notificacaoOriginal.nomeAluno}",
                            Toast.LENGTH_LONG
                        ).show()

                        // Marcar a notificação original como lida
                        marcarComoLida(notificacaoOriginal)
                    }
                    .addOnFailureListener { ex ->
                        Toast.makeText(
                            this,
                            "Erro ao enviar resposta: ${ex.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { ex ->
                Toast.makeText(
                    this,
                    "Erro ao buscar dados do funcionário: ${ex.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}