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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.util.*

class TelaTreinoAluno : BaseActivity() {

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

        // 1) Header: avatar, nome e notificação
        imgAvatar      = findViewById(R.id.imgAvatar)
        nomeUser       = findViewById(R.id.nomeUser)
        btnNotificacao = findViewById(R.id.btn_notificacao)
        btnNotificacao.setOnClickListener {
            startActivity(Intent(this, TelaNotificacaoAluno::class.java))
        }

        // 2) RecyclerView + Adapter
        rvFichas = findViewById(R.id.rvFichas)
        rvFichas.layoutManager = LinearLayoutManager(this)
        adapter = FichaAdapterAluno(fichas) { ficha ->
            // Ao clicar em uma ficha, abre TelaFichaTreinoAluno passando dados
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

        // 4) Carrega dados iniciais
        carregarPerfil()
        carregarFichas()
    }

    override fun onResume() {
        super.onResume()
        carregarPerfil()
        carregarFichas()
    }

    /** Carrega nome e avatar via Glide, com circleCrop */
    private fun carregarPerfil() {
        val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        val uid = prefs.getString("alunoDocId", null) ?: return

        val nome = prefs.getString("nome", "Usuario") ?: "Usuario"
        val nomeUsuario = prefs.getString("nome_usuario", "") ?: ""

        // Prioridade: nome_usuario se não estiver vazio, senão nome
        nomeUser.text = if (nomeUsuario.isNotBlank()) {
            "Olá, $nomeUsuario!"
        } else {
            "Olá, $nome!"
        }

        db.collection("alunos").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val path = doc.getString("uri_foto")
                Log.d("TelaPrincipalAluno", "Caminho da foto no banco: $path")

                if (!path.isNullOrBlank()) {
                    // 🎯 MUDANÇA PRINCIPAL: Usar File diretamente como na tela funcionando
                    val file = File(path)
                    Log.d("TelaPrincipalAluno", "Arquivo existe? ${file.exists()}")
                    Log.d("TelaPrincipalAluno", "Caminho completo: ${file.absolutePath}")

                    if (file.exists()) {
                        Log.d("TelaPrincipalAluno", "Carregando foto com Glide...")

                        // 🎯 NOVO: Limpar qualquer imagem anterior (igual na tela funcionando)
                        imgAvatar.setImageDrawable(null)

                        Glide.with(this)
                            .load(file) // 🎯 Usar File diretamente, não Uri.parse()
                            .placeholder(R.drawable.ic_person) // Placeholder enquanto carrega
                            .error(R.drawable.ic_person) // Imagem de erro
                            .circleCrop()
                            .skipMemoryCache(true) // Manter igual à tela funcionando
                            .into(imgAvatar)

                        Log.d("TelaPrincipalAluno", "Comando Glide executado!")
                    } else {
                        Log.w("TelaPrincipalAluno", "Arquivo da foto não encontrado! Usando placeholder")
                        imgAvatar.setImageResource(R.drawable.ic_person)
                    }
                } else {
                    Log.d("TelaPrincipalAluno", "Nenhuma foto salva no banco. Usando placeholder")
                    imgAvatar.setImageResource(R.drawable.ic_person)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("TelaPrincipalAluno", "Erro ao carregar perfil", exception)
                imgAvatar.setImageResource(R.drawable.ic_person)
            }
    }

    /** Busca todas as fichas na subcoleção "treino" e ordena alfabeticamente por letra */
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
                // Limpa lista atual
                fichas.clear()

                // Converte cada documento em FichaTreinoAluno
                snapshot.documents.forEach { doc ->
                    val letra  = doc.getString("letra") ?: return@forEach
                    val titulo = doc.getString("nome") ?: ""
                    val qtd    = doc.getLong("quantidadeExercicios")?.toInt()
                        ?: doc.getString("quantidadeExercicios")?.toIntOrNull()
                        ?: 0
                    fichas.add(FichaTreinoAluno(letra, titulo, qtd))
                }

                // Ordena alfabeticamente pelo campo 'letra' (ignorando maiúsculas/minúsculas)
                fichas.sortBy { it.letra.uppercase(Locale.getDefault()) }

                // Notifica o adapter para atualizar a RecyclerView
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("TelaTreinoAluno", "Erro ao buscar fichas", e)
                Toast.makeText(this, "Erro ao carregar treinos", Toast.LENGTH_SHORT).show()
            }
    }
}
