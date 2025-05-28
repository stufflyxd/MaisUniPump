package com.example.unipump

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.adapters.ChatAdapter
import com.example.unipump.models.ChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

class TelaChat : AppCompatActivity() {

    private lateinit var setaVoltar: ImageButton
    private lateinit var edtPergunta: EditText
    private lateinit var btnEnviarMsg: Button
    private lateinit var recyclerViewChat: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val mensagens = mutableListOf<ChatMessage>()

    companion object {
        private val PRE_PROMPT = """
            Você é um assistente virtual especializado em academia e saúde chamado FitBot, 
            criado para o aplicativo UniPump. Você deve seguir estas diretrizes rigorosamente:

            ESCOPO PERMITIDO:
            ✅ Exercícios físicos e musculação
            ✅ Nutrição e alimentação saudável
            ✅ Suplementação esportiva
            ✅ Técnicas de treino e periodização
            ✅ Recuperação muscular e descanso
            ✅ Prevenção de lesões no treino
            ✅ Motivação e disciplina para exercícios
            ✅ Equipamentos de academia
            ✅ Programas de treino e rotinas
            ✅ Hidratação e sono
            ✅ Alongamento e flexibilidade

            ESCOPO RESTRITO:
            ❌ Diagnósticos médicos específicos
            ❌ Prescrição de medicamentos
            ❌ Tratamento de doenças
            ❌ Tópicos não relacionados à fitness/saúde
            ❌ Conteúdo político, religioso ou controverso
            ❌ Informações pessoais ou privadas

            INSTRUÇÕES DE RESPOSTA:
            - Seja sempre positivo e motivador
            - Forneça respostas práticas e aplicáveis
            - Quando não souber algo específico, admita e sugira consultar um profissional
            - Para questões médicas sérias, sempre recomende procurar um médico
            - Mantenha respostas concisas mas informativas
            - Use emojis moderadamente para tornar a conversa mais amigável
            - Sempre lembre sobre a importância do acompanhamento profissional

            Se perguntarem sobre algo fora do escopo, responda educadamente que você é especializado apenas em fitness e saúde, e redirecione para tópicos relevantes.

            Agora responda à seguinte pergunta:
        """.trimIndent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_chat)

        // Inicializar views
        setaVoltar = findViewById(R.id.SetaVoltar)
        edtPergunta = findViewById(R.id.editTextMessage)
        btnEnviarMsg = findViewById(R.id.btnEnviarMsg)
        recyclerViewChat = findViewById(R.id.recyclerViewChat)

        // Configurar RecyclerView
        setupRecyclerView()

        // Adicionar mensagem inicial da IA
        adicionarMensagemInicial()

        // Configurar eventos
        configurarEventos()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(mensagens)
        recyclerViewChat.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@TelaChat)

            // Scroll automático para baixo quando nova mensagem for adicionada
            chatAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    scrollToPosition(chatAdapter.itemCount - 1)
                }
            })
        }
    }

    private fun adicionarMensagemInicial() {
        val mensagemInicial = ChatMessage(
            mensagem = "💪 Olá! Sou seu assistente virtual! Estou aqui para te ajudar com treinos, nutrição, suplementação e tudo sobre vida saudável. Como posso te ajudar hoje?",
            isUsuario = false
        )
        chatAdapter.adicionarMensagem(mensagemInicial)
    }

    private fun configurarEventos() {
        // Definindo o clique do botão de voltar
        setaVoltar.setOnClickListener {
            finish()
        }

        btnEnviarMsg.setOnClickListener {
            val pergunta = edtPergunta.text.toString().trim()
            if (pergunta.isNotEmpty()) {
                enviarMensagem(pergunta)
                edtPergunta.setText("") // Limpar campo
            } else {
                Toast.makeText(this, "Digite uma mensagem antes de enviar.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enviarMensagem(mensagem: String) {
        // Adicionar mensagem do usuário
        val mensagemUsuario = ChatMessage(
            mensagem = mensagem,
            isUsuario = true
        )
        chatAdapter.adicionarMensagem(mensagemUsuario)

        // Mostrar indicador de "digitando"
        mostrarIndicadorDigitando()

        // Enviar para IA e aguardar resposta
        sendPrompt(mensagem)
    }

    private fun mostrarIndicadorDigitando() {
        val indicador = ChatMessage(
            mensagem = "Digitando...",
            isUsuario = false
        )
        chatAdapter.adicionarMensagem(indicador)
    }

    private fun sendPrompt(prompt: String) {
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = "AIzaSyAYnoacKnW6Dt2Q6Vz9h0Z4hAFmXoV0Aw0"
        )

        lifecycleScope.launch {
            try {
                // APLICAR O PRÉ-PROMPT
                val promptCompleto = "$PRE_PROMPT\n\nPergunta do usuário: $prompt"

                val response = generativeModel.generateContent(promptCompleto)

                response.text?.let { outputContent ->
                    Log.d("resposta", outputContent)

                    // Remover indicador de "digitando"
                    if (mensagens.isNotEmpty() && mensagens.last().mensagem == "Digitando...") {
                        mensagens.removeAt(mensagens.size - 1)
                        runOnUiThread {
                            chatAdapter.notifyItemRemoved(mensagens.size)
                        }
                    }

                    // Adicionar resposta da IA
                    val respostaIA = ChatMessage(
                        mensagem = outputContent,
                        isUsuario = false
                    )

                    runOnUiThread {
                        chatAdapter.adicionarMensagem(respostaIA)
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatError", "Erro ao obter resposta da IA", e)

                // Remover indicador de "digitando"
                if (mensagens.isNotEmpty() && mensagens.last().mensagem == "Digitando...") {
                    mensagens.removeAt(mensagens.size - 1)
                    runOnUiThread {
                        chatAdapter.notifyItemRemoved(mensagens.size)
                    }
                }

                // CORREÇÃO: Criar mensagem de erro corretamente
                val mensagemErro = ChatMessage(
                    mensagem = "Desculpe, ocorreu um erro. Tente novamente.",
                    isUsuario = false
                )

                runOnUiThread {
                    chatAdapter.adicionarMensagem(mensagemErro) // CORRIGIDO: era "respostaIA"
                }
            }
        }
    }
}