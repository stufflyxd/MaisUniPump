package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class TelaLogin : AppCompatActivity() {

    private lateinit var btnVoltar: Button
    private lateinit var textEsqueceuSenha: TextView
    private lateinit var edtEmail: EditText
    private lateinit var edtSenha: EditText
    private lateinit var btnEntrar: AppCompatButton
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var tipoUsuario: String = "aluno"

    private var senhaVisivel: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_login)

        auth = FirebaseAuth.getInstance()
        db = Firebase.firestore

        tipoUsuario = intent.getStringExtra("tipo") ?: "aluno"

        btnVoltar         = findViewById(R.id.btnVoltar)
        textEsqueceuSenha = findViewById(R.id.tvEsqueceuSenha)
        edtEmail          = findViewById(R.id.etEmail)
        edtSenha          = findViewById(R.id.etSenha)
        btnEntrar         = findViewById(R.id.btnEntrar)

        if (tipoUsuario == "aluno") {
            edtEmail.hint = "Email ou Telefone"
        } else {
            edtEmail.hint = "ID"
        }

        configurarToggleSenha()
        configurarEventos()
    }

    private fun configurarToggleSenha() {
        edtSenha.setOnTouchListener { _, event ->
            val DRAWABLE_RIGHT = 2
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableRight = edtSenha.compoundDrawables[DRAWABLE_RIGHT]
                if (drawableRight != null && event.rawX >= (edtSenha.right - drawableRight.bounds.width())) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun togglePasswordVisibility() {
        senhaVisivel = !senhaVisivel
        if (senhaVisivel) {
            edtSenha.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            edtSenha.setCompoundDrawablesWithIntrinsicBounds(null, null,
                ContextCompat.getDrawable(this, R.drawable.ic_eye_off), null)
        } else {
            edtSenha.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            edtSenha.setCompoundDrawablesWithIntrinsicBounds(null, null,
                ContextCompat.getDrawable(this, R.drawable.ic_eye), null)
        }
        edtSenha.setSelection(edtSenha.text.length)
    }

    private fun configurarEventos() {
        btnVoltar.setOnClickListener {
            startActivity(Intent(this, TelaInicial::class.java).apply {
                putExtra("tipo", tipoUsuario)
            })
            finish()
        }

        textEsqueceuSenha.setOnClickListener {
            startActivity(Intent(this, TelaEsqueceuSenha::class.java).apply {
                putExtra("tipo", tipoUsuario)
            })
        }

        btnEntrar.setOnClickListener { onEntrarClick() }
    }

    private fun onEntrarClick() {
        val usuario = edtEmail.text.toString().trim()
        val senha = edtSenha.text.toString().trim()

        if (usuario.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        when (tipoUsuario) {
            "funcionario" -> loginFuncionario(usuario, senha)
            "aluno"       -> loginAluno(usuario, senha)
            else           -> Toast.makeText(this, "Tipo de usuário inválido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginFuncionario(id: String, senha: String) {
        if (!id.matches(Regex("^[0-9]+$"))) {
            Toast.makeText(this, "ID inválido", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("funcionarios").document(id).get()
            .addOnSuccessListener { doc ->
                val email = doc.getString("email")
                if (email.isNullOrEmpty()) {
                    Toast.makeText(this, "Funcionário não encontrado", Toast.LENGTH_SHORT).show()
                } else {
                    signIn(email, senha)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao buscar funcionário", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loginAluno(login: String, senha: String) {
        val campo = if (login.matches(Regex("^\\+?[0-9]{10,13}$"))) "telefone" else "email"
        if (campo == "email" && !login.matches(Regex("[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}"))) {
            Toast.makeText(this, "Email inválido", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("alunos")
            .whereEqualTo(campo, login)
            .get()
            .addOnSuccessListener { snaps ->
                if (snaps.isEmpty) {
                    Toast.makeText(this, "Aluno não encontrado", Toast.LENGTH_SHORT).show()
                } else {
                    val email = snaps.documents[0].getString("email") ?: ""
                    signIn(email, senha)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao buscar aluno", Toast.LENGTH_SHORT).show()
            }
    }

    private fun signIn(email: String, senha: String) {
        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener { authRes ->
                if (authRes.isSuccessful) onSignInSuccess(email) else
                    Toast.makeText(this, "Usuário ou senha inválidos", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { ex ->
                val msg = when (ex) {
                    is FirebaseAuthWeakPasswordException -> "Senha fraca"
                    is FirebaseAuthInvalidUserException -> "Usuário desativado"
                    is FirebaseAuthInvalidCredentialsException -> "Credenciais inválidas"
                    else -> "Erro: ${ex.localizedMessage}"
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
    }

    private fun onSignInSuccess(email: String) {
        val tipo = tipoUsuario
        val colecao = if (tipo == "aluno") "alunos" else "funcionarios"
        val prefsNome = if (tipo == "aluno") "alunoPrefs" else "funcionarioPrefs"
        val docKey = if (tipo == "aluno") "alunoDocId" else "funcionarioDocId"

        db.collection(colecao)
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { snaps ->
                val doc = snaps.documents[0]
                val docId = doc.id

                val prefs = getSharedPreferences(prefsNome, MODE_PRIVATE).edit()
                prefs.putString(docKey, docId)
                prefs.apply()

                val destino = if (tipo == "aluno") TelaPrincipalAluno::class.java else TelaFuncionario::class.java
                startActivity(Intent(this, destino).apply {
                    putExtra("tipo", tipo)
                })
                finish()
            }
    }
}
