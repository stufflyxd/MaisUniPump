package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class TelaLogin : BaseActivity() {

    private lateinit var btnVoltar: Button
    private lateinit var textEsqueceuSenha: TextView
    private lateinit var edtEmail: EditText
    private lateinit var edtSenha: EditText
    private lateinit var btnEntrar: AppCompatButton
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // NOVA VARIÁVEL para controlar o estado da visibilidade da senha
    private var senhaVisivel: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_login)

        // Inicializa os componentes da UI
        btnVoltar = findViewById(R.id.btnVoltar)
        textEsqueceuSenha = findViewById(R.id.tvEsqueceuSenha)
        edtEmail = findViewById(R.id.etEmail)
        edtSenha = findViewById(R.id.etSenha)
        btnEntrar = findViewById(R.id.btnEntrar)

        // Inicializa Firebase
        auth = FirebaseAuth.getInstance()
        db = Firebase.firestore

        // Configura hint baseado no tipo de usuário
        val tipo = intent.getStringExtra("tipo")
        if (tipo == "aluno") {
            edtEmail.hint = "Email ou Telefone"
        } else if (tipo == "funcionario") {
            edtEmail.hint = "Id"
        }

        // NOVA FUNÇÃO: Configurar toggle de visibilidade da senha
        configurarToggleSenha()

        // Configura os eventos
        configurarEventos()
    }

    // NOVA FUNÇÃO: Configurar o toggle de visibilidade da senha
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

    // NOVA FUNÇÃO: Alternar visibilidade da senha
    private fun togglePasswordVisibility() {
        senhaVisivel = !senhaVisivel

        if (senhaVisivel) {
            // Mostrar senha
            edtSenha.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

            // Trocar para ícone "olho fechado" - indica que a senha está visível
            val eyeOffDrawable = ContextCompat.getDrawable(this, R.drawable.ic_eye_off)
            edtSenha.setCompoundDrawablesWithIntrinsicBounds(null, null, eyeOffDrawable, null)

        } else {
            // Ocultar senha
            edtSenha.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            // Trocar para ícone "olho aberto" - indica que a senha está oculta
            val eyeOnDrawable = ContextCompat.getDrawable(this, R.drawable.ic_eye)
            edtSenha.setCompoundDrawablesWithIntrinsicBounds(null, null, eyeOnDrawable, null)
        }

        // Manter o cursor no final do texto
        edtSenha.setSelection(edtSenha.text.length)
    }

    private fun configurarEventos() {
        val tipo = intent.getStringExtra("tipo")

        btnVoltar.setOnClickListener {
            try {
                val intent = Intent(this, TelaInicial::class.java)
                // Limpar toda a pilha de Activities
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                // NÃO chamar finish() aqui - deixar o sistema gerenciar
            } catch (e: Exception) {
                Log.e("TelaLogin", "Erro ao voltar para TelaInicial", e)
                super.onBackPressed()
            }
        }

        textEsqueceuSenha.setOnClickListener {
            try {
                val intent = Intent(this, TelaEsqueceuSenha::class.java)
                // CORREÇÃO: Pegar o tipo do intent ATUAL, não do novo intent
                val tipoAtual = this.intent.getStringExtra("tipo")
                intent.putExtra("tipo", tipoAtual)

                // IMPORTANTE: Não finalizar TelaLogin ainda
                startActivity(intent)
                // Não chamar finish() aqui
            } catch (e: Exception) {
                Log.e("TelaLogin", "Erro ao abrir TelaEsqueceuSenha", e)
                Toast.makeText(this, "Erro ao abrir tela de recuperação", Toast.LENGTH_SHORT).show()
            }
        }

        // Evento do botão de login
        btnEntrar.setOnClickListener {
            onEntrarClick()
        }
    }

    // Função de login
    private fun onEntrarClick() {
        val usuario = edtEmail.text.toString().trim()
        val senha = edtSenha.text.toString()
        val tipo = intent.getStringExtra("tipo")

        if (usuario.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (tipo == "funcionario" && isValidId(usuario)) {
            // Buscar e-mail associado ao ID no Firestore
            db.collection("funcionarios").document(usuario).get()
                .addOnSuccessListener { documento ->
                    if (documento.exists()) {
                        val email = documento.getString("email")
                        if (!email.isNullOrEmpty()) {
                            loginComEmail(email, senha, tipo)
                        } else {
                            Toast.makeText(this, "ID inválido: e-mail não encontrado", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Funcionário não encontrado", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao buscar funcionário: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }

        } else if (tipo == "aluno" && (isValidEmail(usuario) || isValidPhone(usuario))) {
            val campoBusca = if (isValidPhone(usuario)) "telefone" else "email"

            db.collection("alunos")
                .whereEqualTo(campoBusca, usuario)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val documento = documents.documents[0]
                        val email = documento.getString("email")
                        if (!email.isNullOrEmpty()) {
                            loginComEmail(email, senha, tipo)
                        } else {
                            Toast.makeText(this, "Email do aluno não encontrado", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Aluno não encontrado", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao buscar aluno: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Formato de login inválido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginComEmail(email: String, senha: String, tipo: String?) {
        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener { autenticacao ->

                if (autenticacao.isSuccessful) {

                    val uid = auth.currentUser?.uid

                    if (uid != null && tipo != null) {
                        val colecao = if (tipo == "aluno") "alunos" else "funcionarios"
                        db.collection(colecao).whereEqualTo("email", email).get()
                            .addOnSuccessListener { documents ->
                                if (!documents.isEmpty) {
                                    val dadosUsuario = documents.documents[0]
                                    val docId = dadosUsuario.id

                                    // CORREÇÃO: Salvar sempre na SharedPreference correta baseada no tipo
                                    val prefsName = if (tipo == "aluno") "alunoPrefs" else "funcionarioPrefs"
                                    val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
                                    val editor = prefs.edit()

                                    // Dados comuns (sempre salvos)
                                    editor.putString("uid", uid)
                                    editor.putString("tipo", tipo)
                                    editor.putString("email", email)

                                    if (tipo == "aluno") {
                                        // Dados específicos do aluno
                                        editor.putString("alunoDocId", docId)
                                        editor.putString("nome_usuario", dadosUsuario.getString("nome_usuario"))
                                        editor.putString("nome", dadosUsuario.getString("nome"))
                                        editor.putString("sobrenome", dadosUsuario.getString("sobrenome"))
                                        editor.putString("idade", dadosUsuario.getString("idade"))
                                        editor.putString("genero", dadosUsuario.getString("genero"))
                                        editor.putString("endereco", dadosUsuario.getString("endereco"))
                                        editor.putString("telefone", dadosUsuario.getString("telefone"))

                                        Log.d("TelaLogin", "Dados do aluno salvos - DocId: $docId, Nome: ${dadosUsuario.getString("nome")}")
                                    } else {
                                        // Dados específicos do funcionário
                                        editor.putString("funcionarioDocId", docId)
                                        editor.putString("nome", dadosUsuario.getString("nome"))
                                        editor.putString("sobrenome", dadosUsuario.getString("sobrenome"))
                                        editor.putString("nome_usuario", dadosUsuario.getString("nome_usuario"))
                                        editor.putString("idade", dadosUsuario.getString("idade"))
                                        editor.putString("genero", dadosUsuario.getString("genero"))
                                        editor.putString("endereco", dadosUsuario.getString("endereco"))
                                        editor.putString("telefone", dadosUsuario.getString("telefone"))

                                        Log.d("TelaLogin", "Dados do funcionário salvos - DocId: $docId, Nome: ${dadosUsuario.getString("nome")}")
                                    }

                                    // IMPORTANTE: Aplicar as mudanças
                                    editor.apply()

                                    // Log para confirmar que foi salvo
                                    Log.d("TelaLogin", "=== DADOS SALVOS NO SHAREDPREFERENCES ===")
                                    Log.d("TelaLogin", "Tipo: ${prefs.getString("tipo", "N/A")}")
                                    Log.d("TelaLogin", "UID: ${prefs.getString("uid", "N/A")}")
                                    if (tipo == "aluno") {
                                        Log.d("TelaLogin", "AlunoDocId: ${prefs.getString("alunoDocId", "N/A")}")
                                    } else {
                                        Log.d("TelaLogin", "FuncionarioDocId: ${prefs.getString("funcionarioDocId", "N/A")}")
                                    }

                                    // Vai para a tela principal
                                    try {
                                        val intent = if (tipo == "aluno") {
                                            Intent(this, TelaPrincipalAluno::class.java)
                                        } else {
                                            Intent(this, TelaFuncionario::class.java)
                                        }
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                    } catch (e: Exception) {
                                        Log.e("TelaLogin", "Erro ao navegar para tela principal", e)
                                        Toast.makeText(this, "Erro ao abrir tela principal", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this,
                                    "Erro ao buscar dados do usuário",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Usuário ou senha inválidos.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { excecao ->
                val msgErro = when (excecao) {
                    is FirebaseAuthWeakPasswordException ->
                        "Digite uma senha com no mínimo 6 caracteres"
                    is FirebaseAuthInvalidUserException ->
                        "Usuário não encontrado ou desativado"
                    is FirebaseAuthInvalidCredentialsException ->
                        "Credenciais inválidas"
                    else ->
                        "Erro ao autenticar: ${excecao.localizedMessage}"
                }
                Toast.makeText(this, msgErro, Toast.LENGTH_LONG).show()
            }
    }

    override fun onBackPressed() {
        try {
            if (!isFinishing && !isDestroyed) {
                val intent = Intent(this, TelaInicial::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("TelaLogin", "Erro no onBackPressed", e)
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
        } catch (e: Exception) {
            Log.e("TelaLogin", "Erro no onDestroy", e)
        }
    }

    override fun finish() {
        try {
            if (!isFinishing) {
                super.finish()
            }
        } catch (e: Exception) {
            Log.e("TelaLogin", "Erro no finish", e)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        return email.matches(emailPattern.toRegex())
    }

    private fun isValidId(id: String): Boolean {
        val idPattern = "^[0-9]+$"
        return id.matches(idPattern.toRegex())
    }

    private fun isValidPhone(phone: String): Boolean {
        val phonePattern = "^\\+?[0-9]{10,13}$"
        return phone.matches(phonePattern.toRegex())
    }
}