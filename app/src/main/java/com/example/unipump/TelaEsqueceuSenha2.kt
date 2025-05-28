package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class TelaEsqueceuSenha2 : AppCompatActivity() {

    private lateinit var btnEditar : Button
    private lateinit var btnConfirmar : Button
    private lateinit var textInstruction : TextView

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_esqueceu_senha2)
        Log.d("CicloDeVida", "onStart chamado")

        btnEditar = findViewById(R.id.btnEditar)
        btnConfirmar = findViewById(R.id.btnConfirmar)
        textInstruction = findViewById(R.id.textViewInstruction)

        auth = FirebaseAuth.getInstance()


        configurarEventos()

        val tipo = intent.getStringExtra("tipo") ?: ""
        textInstruction.text = when {
            isValidEmail(tipo) -> "Antes de enviarmos o código, $tipo é o E-mail correto?"
            isValidPhone(tipo) -> "Antes de enviarmos o código, $tipo é o seu número correto?"
            else -> "tipo invalido"
        }
    }



    private fun configurarEventos() {

        val tipoUsuario = intent.getStringExtra("tipoUsuario")
        val emailUsuario = intent.getStringExtra("tipo") ?: ""

        btnEditar.setOnClickListener {
            val intent = Intent(this, TelaEsqueceuSenha::class.java)
            startActivity(intent)
        }

        btnConfirmar.setOnClickListener {

            if (isValidEmail(emailUsuario)) {
                auth.sendPasswordResetEmail(emailUsuario)
                    .addOnSuccessListener {
                        // Enviado com sucesso!
                        Log.d("Email", "E-mail de redefinição enviado para $emailUsuario")
                        Toast.makeText(this, "Email enviado para o seu email para a redefinição da sua senha ", Toast.LENGTH_SHORT).show()

                        // Redireciona para próxima tela
                        /*val intent = Intent(this, TelaEsqueceuSenha3::class.java)
                        intent.putExtra("tipo", tipo)
                        intent.putExtra("codigoEnviado", "12345")
                        intent.putExtra("tipoUsuario", intent.getStringExtra("tipoUsuario"))
                        startActivity(intent)*/

                        val intent = Intent(this, TelaLogin::class.java)
                        intent.putExtra("tipo", tipoUsuario)
                        startActivity(intent)

                    }
                    .addOnFailureListener { e ->
                        Log.e("Email", "Erro ao enviar e-mail: ${e.message}")
                        Toast.makeText(this, "Email NAO FOI ENVIADO ", Toast.LENGTH_SHORT).show()

                        // Mostre uma mensagem para o usuário, se quiser
                    }
            } else {
                Log.e("Email", "Tipo inválido para envio de email de redefinição.")
                // Pode exibir um Toast avisando que o e-mail é inválido
            }
        }

    }



    override fun onStart() {
        super.onStart()
        Log.d("CicloDeVida", "onStart chamado")
    }

    override fun onResume() {
        super.onResume()
        Log.d("CicloDeVida", "onResume chamado")
    }

    override fun onPause() {
        super.onPause()
        Log.d("CicloDeVida", "onPause chamado")
    }

    override fun onStop() {
        super.onStop()
        Log.d("CicloDeVida", "onStop chamado")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CicloDeVida", "onDestroy chamado")
    }




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
private fun configurarEventos() {

    val tipoUsuario = intent.getStringExtra("tipoUsuario")
    val tipo = intent.getStringExtra("tipo") ?: ""

    btnEditar.setOnClickListener {
        val intent = Intent(this, TelaEsqueceuSenha::class.java)
        startActivity(intent)
    }

    btnConfirmar.setOnClickListener {
        val intent = Intent(this, TelaEsqueceuSenha3::class.java)
        intent.putExtra("tipo", tipo)
        intent.putExtra("codigoEnviado", "12345")
        if (tipoUsuario == "aluno"){
            intent.putExtra("tipoUsuario", "aluno")
        } else if (tipoUsuario == "funcionario"){
            intent.putExtra("tipoUsuario", "funcionario")
        }
        startActivity(intent)
    }
}
*/
