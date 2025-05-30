package com.example.unipump

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.unipump.adapters.FichaAdapterAluno
import com.example.unipump.models.FichaTreinoAluno
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class TelaTreinoAluno : AppCompatActivity() {

    private lateinit var imgAvatar: ImageView
    private lateinit var nomeUser: TextView
    private lateinit var btnNotificacao: ImageButton
    private lateinit var rvFichas: RecyclerView
    private lateinit var bottomNav: BottomNavigationView

    private val fichas = mutableListOf<FichaTreinoAluno>()
    private lateinit var adapter: FichaAdapterAluno

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_treino_aluno)

        // 1) Header: avatar, nome e notificações
        imgAvatar       = findViewById(R.id.imgAvatar)
        nomeUser        = findViewById(R.id.nomeUser)
        btnNotificacao  = findViewById(R.id.btn_notificacao)

        btnNotificacao.setOnClickListener {
            startActivity(Intent(this, TelaNotificacao_funcionario::class.java))
        }

        // 2) RecyclerView + Adapter
        rvFichas = findViewById(R.id.rvFichas)
        rvFichas.layoutManager = LinearLayoutManager(this)
        adapter = FichaAdapterAluno(fichas) { ficha ->
            Intent(this, TelaFichaTreinoAluno::class.java).apply {
                putExtra("letra", ficha.letra)
                putExtra("titulo", ficha.titulo)
                putExtra("totalExercicios", ficha.totalExercicios)
                startActivity(this)
            }
        }
        rvFichas.adapter = adapter

        // 3) Bottom Navigation
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio ->
                    startActivity(Intent(this, TelaPrincipalAluno::class.java)).run { true }
                R.id.nav_treinos -> true
                R.id.nav_chat ->
                    startActivity(Intent(this, TelaChat::class.java)).run { true }
                R.id.nav_config ->
                    startActivity(Intent(this, TelaConfig::class.java)).run { true }
                else -> false
            }
        }

        // 4) Carrega os dados
        carregarPerfil()
        carregarFichas()
    }

    override fun onResume() {
        super.onResume()
        carregarPerfil()
        carregarFichas()
    }

    /** Garante que nome e avatar sejam carregados igual ao TelaConfig */
    private fun carregarPerfil() {
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val uid   = prefs.getString("alunoDocId", null) ?: return
        val nome  = prefs.getString("nome", "Usuário") ?: "Usuário"
        nomeUser.text = "Olá, $nome!"

        db.collection("alunos").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val path = doc.getString("uri_foto")
                if (!path.isNullOrBlank()) {
                    Glide.with(this)
                        .load(Uri.parse(path))
                        .circleCrop()
                        .skipMemoryCache(true)
                        .into(imgAvatar)
                } else {
                    imgAvatar.setImageResource(R.drawable.ic_person)
                }
            }
            .addOnFailureListener {
                Log.e("TelaTreinoAluno", "erro ao carregar perfil", it)
            }
    }

    /** Busca todas as fichas na subcoleção treino */
    private fun carregarFichas() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Faça login para ver seus treinos", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val uid   = prefs.getString("alunoDocId", null)
        if (uid.isNullOrEmpty()) {
            Toast.makeText(this, "ID do aluno não encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("alunos").document(uid)
            .collection("treino")
            .get()
            .addOnSuccessListener { snapshot ->
                fichas.clear()
                snapshot.documents.forEach { doc ->
                    val letra = doc.getString("letra") ?: return@forEach
                    val titulo = doc.getString("nome") ?: ""
                    val qtd = doc.getLong("quantidadeExercicios")?.toInt()
                        ?: doc.getString("quantidadeExercicios")?.toIntOrNull()
                        ?: 0
                    fichas.add(FichaTreinoAluno(letra, titulo, qtd))
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("TelaTreinoAluno", "Erro ao buscar fichas", e)
                Toast.makeText(this, "Erro ao carregar treinos", Toast.LENGTH_SHORT).show()
            }
    }
}
