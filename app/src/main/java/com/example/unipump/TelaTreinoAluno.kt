package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.adapters.FichaAdapterAluno
import com.example.unipump.models.FichaTreinoAluno
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TelaTreinoAluno : AppCompatActivity() {

    private lateinit var rvFichas: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var nomeUser: TextView

    private val fichas = mutableListOf<FichaTreinoAluno>()
    private lateinit var adapter: FichaAdapterAluno

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_treino_aluno)

        // 1) Header: nome do usuário
        nomeUser = findViewById(R.id.nomeUser)
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val nome  = prefs.getString("nome", "Usuário") ?: "Usuário"
        nomeUser.text = "Olá, $nome!"

        // 2) RecyclerView + Adapter
        rvFichas = findViewById(R.id.rvFichas)
        rvFichas.layoutManager = LinearLayoutManager(this)
        adapter = FichaAdapterAluno(fichas) { ficha ->
            // Ao clicar em uma ficha, abre TelaFichaTreinoAluno
            val intent = Intent(this, TelaFichaTreinoAluno::class.java).apply {
                putExtra("letra", ficha.letra)
                putExtra("titulo", ficha.titulo)
                putExtra("totalExercicios", ficha.totalExercicios)
            }
            startActivity(intent)
        }
        rvFichas.adapter = adapter

        // 3) Bottom Navigation
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    startActivity(Intent(this, TelaPrincipalAluno::class.java))
                    true
                }
                R.id.nav_treinos -> true
                R.id.nav_chat -> {
                    startActivity(Intent(this, TelaChat::class.java))
                    true
                }
                R.id.nav_config -> {
                    startActivity(Intent(this, TelaConfig::class.java))
                    true
                }
                else -> false
            }
        }

        // 4) Carrega as fichas do Firestore
        carregarFichas()
    }

    private fun carregarFichas() {
        // Verifica usuário logado
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.w("TelaTreinoAluno", "Usuário NÃO está logado → não carrega fichas")
            Toast.makeText(this, "Faça login para ver seus treinos", Toast.LENGTH_SHORT).show()
            return
        }

        // Recupera o docId do aluno em prefs
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val uid   = prefs.getString("alunoDocId", null)
        if (uid.isNullOrBlank()) {
            Toast.makeText(this, "ID do aluno não encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("alunos")
            .document(uid)
            .collection("treino")
            .get()
            .addOnSuccessListener { snapshot ->
                fichas.clear()
                for (doc in snapshot.documents) {
                    val letra = doc.getString("letra") ?: continue
                    val titulo = doc.getString("nome") ?: ""
                    val quantidade = doc.getLong("quantidadeExercicios")?.toInt()
                        ?: doc.getString("quantidadeExercicios")?.toIntOrNull()
                        ?: 0

                    fichas.add(FichaTreinoAluno(letra, titulo, quantidade))
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("TelaTreinoAluno", "Erro ao buscar fichas", e)
                Toast.makeText(this, "Erro ao carregar treinos", Toast.LENGTH_SHORT).show()
            }
    }
}
