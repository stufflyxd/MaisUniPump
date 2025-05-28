package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class TelaEsqueceuSenha : AppCompatActivity() {

    private lateinit var btnEnviar: Button
    private lateinit var edtEmailOuTelefone: EditText

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_esqueceu_senha)

        // Inicializa as variáveis
        btnEnviar = findViewById(R.id.btnEnviar)
        edtEmailOuTelefone = findViewById(R.id.edtEmailOuTelefone_recuperar_senha_1)

        /*auth = FirebaseAuth.getInstance()*/


        // Configura os eventos
        configurarEventos()
    }

    private fun configurarEventos() {
        btnEnviar.setOnClickListener {
            onEnviarClick()
        }
    }

    private fun onEnviarClick() {
        val emailOuTelefone = edtEmailOuTelefone.text.toString()

        val tipoUsuario = intent.getStringExtra("tipo")


        if (emailOuTelefone.isNotEmpty()) {

            if (isValidEmail(emailOuTelefone)) {
                // Se for um e-mail válido, redireciona para a próxima tela
                val intent = Intent(this, TelaEsqueceuSenha2::class.java)
                intent.putExtra("tipo",emailOuTelefone)
                if (tipoUsuario == "aluno"){
                    intent.putExtra("tipoUsuario", "aluno")
                } else if (tipoUsuario == "funcionario"){
                    intent.putExtra("tipoUsuario", "funcionario")
                }
                startActivity(intent)
            }

            else if (isValidPhone(emailOuTelefone)) {
                // Se for um número de telefone válido, redireciona para a próxima tela
                val intent = Intent(this, TelaEsqueceuSenha2::class.java)
                intent.putExtra("tipo", emailOuTelefone)
                if (tipoUsuario == "aluno"){
                    intent.putExtra("tipoUsuario", "aluno")
                } else if (tipoUsuario == "funcionario"){
                    intent.putExtra("tipoUsuario", "funcionario")
                }
                startActivity(intent)
            }
            else {
                // Se a entrada não for válida, mostra um erro
                Toast.makeText(this, "Por favor, insira um e-mail ou número de telefone válido.", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Mostra uma mensagem caso o campo esteja vazio
            Toast.makeText(this, "Por favor, insira um e-mail ou número de telefone.", Toast.LENGTH_SHORT).show()
        }
    }

    /*private fun onEnviarClick() {
        val emailOuTelefone = edtEmailOuTelefone.text.toString()

        val tipoUsuario = intent.getStringExtra("tipo")


        if (emailOuTelefone.isNotEmpty()) {

            if (isValidEmail(emailOuTelefone)) {
                // Verifica se o e-mail existe no Firebase
                auth.fetchSignInMethodsForEmail(emailOuTelefone)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val signInMethods = task.result?.signInMethods
                            if (!signInMethods.isNullOrEmpty()) {
                                // E-mail encontrado no Firebase, continuar
                                val intent = Intent(this, TelaEsqueceuSenha2::class.java)
                                intent.putExtra("tipo", emailOuTelefone)
                                intent.putExtra("tipoUsuario", tipoUsuario)
                                startActivity(intent)
                            } else {
                                Toast.makeText(this, "E-mail não cadastrado.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Erro ao verificar e-mail: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }

            else if (isValidPhone(emailOuTelefone)) {
                // Se for um número de telefone válido, redireciona para a próxima tela
                val intent = Intent(this, TelaEsqueceuSenha2::class.java)
                intent.putExtra("tipo", emailOuTelefone)
                if (tipoUsuario == "aluno"){
                    intent.putExtra("tipoUsuario", "aluno")
                } else if (tipoUsuario == "funcionario"){
                    intent.putExtra("tipoUsuario", "funcionario")
                }
                startActivity(intent)
            }
            else {
                // Se a entrada não for válida, mostra um erro
                Toast.makeText(this, "Por favor, insira um e-mail ou número de telefone válido.", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Mostra uma mensagem caso o campo esteja vazio
            Toast.makeText(this, "Por favor, insira um e-mail ou número de telefone.", Toast.LENGTH_SHORT).show()
        }
    }*/

    // Função para validar o e-mail
    private fun isValidEmail(email: String): Boolean {
        // Regex para validar e-mails simples
        val emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        return email.matches(emailPattern.toRegex())
    }

    // Função para validar o número de telefone
    private fun isValidPhone(phone: String): Boolean {
        // Regex para validar números de telefone (aqui estou considerando números com 10 ou 11 dígitos)
        val phonePattern = "^\\+?[0-9]{10,13}$"
        return phone.matches(phonePattern.toRegex())
    }
}





/*
private fun onEnviarClick() {
    val emailOuTelefone = edtEmailOuTelefone.text.toString()

    val tipoUsuario = intent.getStringExtra("tipo")


    if (emailOuTelefone.isNotEmpty()) {

        if (isValidEmail(emailOuTelefone)) {
            // Se for um e-mail válido, redireciona para a próxima tela
            val intent = Intent(this, TelaEsqueceuSenha2::class.java)
            intent.putExtra("tipo",emailOuTelefone)
            if (tipoUsuario == "aluno"){
                intent.putExtra("tipoUsuario", "aluno")
            } else if (tipoUsuario == "funcionario"){
                intent.putExtra("tipoUsuario", "funcionario")
            }
            startActivity(intent)
        }

        else if (isValidPhone(emailOuTelefone)) {
            // Se for um número de telefone válido, redireciona para a próxima tela
            val intent = Intent(this, TelaEsqueceuSenha2::class.java)
            intent.putExtra("tipo", emailOuTelefone)
            if (tipoUsuario == "aluno"){
                intent.putExtra("tipoUsuario", "aluno")
            } else if (tipoUsuario == "funcionario"){
                intent.putExtra("tipoUsuario", "funcionario")
            }
            startActivity(intent)
        }
        else {
            // Se a entrada não for válida, mostra um erro
            Toast.makeText(this, "Por favor, insira um e-mail ou número de telefone válido.", Toast.LENGTH_SHORT).show()
        }
    } else {
        // Mostra uma mensagem caso o campo esteja vazio
        Toast.makeText(this, "Por favor, insira um e-mail ou número de telefone.", Toast.LENGTH_SHORT).show()
    }
}*/
