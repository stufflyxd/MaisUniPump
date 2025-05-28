package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
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
    private val fichas = mutableListOf<FichaTreinoAluno>()
    private lateinit var adapter: FichaAdapterAluno
    private lateinit var nomeUser: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_treino_aluno)

        // 1) Configura RecyclerView + Adapter VAZIO
        rvFichas = findViewById(R.id.rvFichas)
        rvFichas.layoutManager = LinearLayoutManager(this)
        adapter = FichaAdapterAluno(fichas) { ficha ->
            val intent = Intent(this, TelaFichaTreinoAluno::class.java).apply {
                putExtra("letra", ficha.letra)
                putExtra("titulo", ficha.titulo)
                putExtra("totalExercicios", ficha.totalExercicios)
            }
            startActivity(intent)
        }
        rvFichas.adapter = adapter

        // 2) Puxa o UID e verifica
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.w("TelaTreinoAluno", "usuário NÃO está logado → não carrega fichas")
        } else {
            // Recuperar os dados do usuário
            val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
            val uid = prefs.getString("alunoDocId", "Usuário").toString()

            Log.d("TelaTreinoAluno", "UID do usuário = $uid")

            // 3) Faz a query manual
            val db = FirebaseFirestore.getInstance()
            db.collection("alunos")
                .document(uid)
                .collection("treino")
                .get()
                .addOnSuccessListener { snapshot ->
                    Log.d("TelaTreinoAluno", "Docs recebidos: ${snapshot.size()}")
                    fichas.clear()
                    for (doc in snapshot.documents) {
                        // log de cada doc
                        Log.d("TelaTreinoAluno", "→ docId=${doc.id} data=${doc.data}")
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
                    Log.e("TelaTreinoAluno", "erro ao buscar fichas", e)
                }
        }

        nomeUser = findViewById(R.id.nomeUser)

        // Recuperar os dados do usuário
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val nome = prefs.getString("nome", "Usuário")

        nomeUser.text = "$nome"

        // 4) BottomNavigation (igual ao seu)
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
    }
}
