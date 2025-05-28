package com.example.unipump.adapters

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.R
import com.example.unipump.TelaCriarFichaTreino_Funcionario
import com.example.unipump.models.fichaTreinoFun

class FichaTreinoFunAdapter(
    private val fichas: MutableList<fichaTreinoFun>,
    private val onExcluirFicha: ((String, Int) -> Unit)? = null,
    private val onFichaAlterada: ((fichaTreinoFun, Int) -> Unit)? = null
) : RecyclerView.Adapter<FichaTreinoFunAdapter.FichaViewHolder>() {

    // Referência ao RecyclerView principal
    private var recyclerView: RecyclerView? = null

    class FichaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val etLetraFicha: EditText = itemView.findViewById(R.id.etLetraFicha)
        val etNomeFicha: EditText = itemView.findViewById(R.id.etNomeFicha)
        val tvQuantidadeExercicios: TextView = itemView.findViewById(R.id.tvQuantidadeExercicios)
        val btnExcluirFicha: ImageButton = itemView.findViewById(R.id.btnExcluirFicha)
        val btnAddExercicio: ImageButton = itemView.findViewById(R.id.btnAddExercicio)
        val recyclerViewExercicios: RecyclerView = itemView.findViewById(R.id.recyclerViewExercicios)

        // NOVO: Variáveis para controlar TextWatchers
        private var letraTextWatcher: TextWatcher? = null
        private var nomeTextWatcher: TextWatcher? = null

        fun clearTextWatchers() {
            letraTextWatcher?.let { etLetraFicha.removeTextChangedListener(it) }
            nomeTextWatcher?.let { etNomeFicha.removeTextChangedListener(it) }
            letraTextWatcher = null
            nomeTextWatcher = null
        }

        fun setTextWatchers(
            onLetraChanged: (String) -> Unit,
            onNomeChanged: (String) -> Unit
        ) {
            // Remover watchers antigos
            clearTextWatchers()

            // Criar novos TextWatchers
            letraTextWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val novaLetra = s.toString().trim().uppercase()
                    onLetraChanged(novaLetra)
                }
            }

            nomeTextWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val novoNome = s.toString().trim()
                    onNomeChanged(novoNome)
                }
            }

            // Adicionar os novos watchers
            etLetraFicha.addTextChangedListener(letraTextWatcher)
            etNomeFicha.addTextChangedListener(nomeTextWatcher)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FichaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ficha_treino_funcionario, parent, false)
        return FichaViewHolder(view)
    }

    override fun onBindViewHolder(holder: FichaViewHolder, position: Int) {
        val ficha = fichas[position]
        Log.d("ADAPTER_DEBUG", "onBindViewHolder: position $position de ${fichas.size}")
        Log.d("ADAPTER_DEBUG", "Ficha: ${ficha.letra} - ${ficha.nome} (${ficha.exercicios.size} exercícios)")

        // CRÍTICO: Limpar watchers antigos antes de preencher
        holder.clearTextWatchers()

        // Preencher dados da ficha
        holder.etLetraFicha.setText(ficha.letra)
        holder.etNomeFicha.setText(ficha.nome)

        // ATUALIZAR O CONTADOR DE EXERCÍCIOS
        atualizarContadorExercicios(holder, ficha)

        // NOVO: Configurar TextWatchers otimizados
        holder.setTextWatchers(
            onLetraChanged = { novaLetra ->
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION && currentPosition < fichas.size) {
                    val fichaAtual = fichas[currentPosition]
                    if (novaLetra != fichaAtual.letra && novaLetra.isNotEmpty()) {
                        // Atualizar diretamente o objeto (referência)
                        fichaAtual.letra = novaLetra

                        Log.d("LETRA_ALTERADA", "Letra alterada para: '$novaLetra' na posição $currentPosition")

                        // Notificar alteração com debounce
                        onFichaAlterada?.invoke(fichaAtual, currentPosition)
                    }
                }
            },
            onNomeChanged = { novoNome ->
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION && currentPosition < fichas.size) {
                    val fichaAtual = fichas[currentPosition]
                    if (novoNome != fichaAtual.nome && novoNome.isNotEmpty()) {
                        // Atualizar diretamente o objeto (referência)
                        fichaAtual.nome = novoNome

                        Log.d("NOME_ALTERADO", "Nome alterado para: '$novoNome' na posição $currentPosition")

                        // Notificar alteração com debounce
                        onFichaAlterada?.invoke(fichaAtual, currentPosition)
                    }
                }
            }
        )

        // CONFIGURAR RecyclerView dos exercícios
        setupExerciciosRecyclerView(holder, ficha, position)

        // MODIFICAÇÃO: Botão para adicionar exercício agora navega para TelaCriarFichaTreino_Funcionario
        holder.btnAddExercicio.setOnClickListener {
            navegarParaTelaCriarExercicio(holder.itemView.context, ficha, position)
        }

        // Botão para excluir ficha
        holder.btnExcluirFicha.setOnClickListener {
            showDeleteFichaDialog(holder.itemView.context) {
                onExcluirFicha?.invoke(ficha.id, position)
            }
        }
    }

    // NOVO MÉTODO: Navegar para TelaCriarFichaTreino_Funcionario
    private fun navegarParaTelaCriarExercicio(context: android.content.Context, ficha: fichaTreinoFun, position: Int) {
        Log.d("NAVEGAR_CRIAR_EXERCICIO", "Navegando para TelaCriarFichaTreino_Funcionario")
        Log.d("NAVEGAR_CRIAR_EXERCICIO", "Ficha: ${ficha.letra} - ${ficha.nome}")
        Log.d("NAVEGAR_CRIAR_EXERCICIO", "Document ID: ${ficha.id}")

        try {
            val intent = Intent(context, TelaCriarFichaTreino_Funcionario::class.java).apply {
                // Passar dados da ficha atual
                putExtra("documentId", ficha.id)
                putExtra("ficha_letra", ficha.letra)
                putExtra("ficha_nome", ficha.nome)
                putExtra("modo_edicao", true) // Indicar que é modo de edição/adição
                putExtra("action", "add_exercicio") // Ação específica

                // Adicionar outros dados necessários se disponíveis no contexto
                // Você pode precisar passar o alunoDocId também
                // putExtra("alunoDocId", alunoDocId) // Se disponível no contexto atual

                Log.d("NAVEGAR_CRIAR_EXERCICIO", "Intent criado com dados da ficha")
            }

            context.startActivity(intent)
            Log.d("NAVEGAR_CRIAR_EXERCICIO", "Navegação iniciada com sucesso")

        } catch (e: Exception) {
            Log.e("NAVEGAR_CRIAR_EXERCICIO", "Erro ao navegar para TelaCriarFichaTreino_Funcionario", e)
            android.widget.Toast.makeText(
                context,
                "Erro ao abrir tela de criação: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Método para atualizar contador de exercícios
    private fun atualizarContadorExercicios(holder: FichaViewHolder, ficha: fichaTreinoFun) {
        val quantidadeExercicios = ficha.exercicios.size
        val texto = when (quantidadeExercicios) {
            0 -> "0 exercícios"
            1 -> "1 exercício"
            else -> "$quantidadeExercicios exercícios"
        }

        holder.tvQuantidadeExercicios.text = texto
        Log.d("CONTADOR_EXERCICIOS", "Ficha ${ficha.letra}: $texto")
    }

    private fun setupExerciciosRecyclerView(holder: FichaViewHolder, ficha: fichaTreinoFun, fichaPosition: Int) {
        Log.d("SETUP_EXERCICIOS", "=== CONFIGURANDO EXERCÍCIOS ===")
        Log.d("SETUP_EXERCICIOS", "Ficha: ${ficha.letra}")
        Log.d("SETUP_EXERCICIOS", "Número de exercícios: ${ficha.exercicios.size}")

        // Listar todos os exercícios para debug
        ficha.exercicios.forEachIndexed { index, exercicio ->
            Log.d("SETUP_EXERCICIOS", "Exercício $index: ${exercicio.nome} (${exercicio.series.size} séries)")
        }

        val exerciciosAdapter = exercicioFunAdapter(
            exercicios = ficha.exercicios,
            onDeleteExercicio = { exercicioPosition: Int ->
                // Callback para deletar exercício
                removeExercicioFromFicha(fichaPosition, exercicioPosition)
                // ATUALIZAR CONTADOR APÓS REMOVER
                atualizarContadorExercicios(holder, ficha)
            },
            onAdicionarSerie = { exercicioPosition: Int ->
                // Callback para quando série for adicionada
                Log.d("FICHA_ADAPTER", "Série adicionada ao exercício $exercicioPosition da ficha ${ficha.letra}")

                // Notificar que a ficha foi alterada
                onFichaAlterada?.invoke(ficha, fichaPosition)
            },
            onExercicioAlterado = { exercicioAlterado: com.example.unipump.models.exercicioFun, exercicioPosition: Int ->
                // Callback para quando exercício for alterado
                Log.d("FICHA_ADAPTER", "Exercício alterado: ${exercicioAlterado.nome} na ficha ${ficha.letra}")

                // Atualizar o exercício na ficha
                if (exercicioPosition >= 0 && exercicioPosition < ficha.exercicios.size) {
                    ficha.exercicios[exercicioPosition] = exercicioAlterado
                }

                // Notificar que a ficha foi alterada
                onFichaAlterada?.invoke(ficha, fichaPosition)
            }
        )

        holder.recyclerViewExercicios.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = exerciciosAdapter

            // CONFIGURAÇÕES CRÍTICAS
            isNestedScrollingEnabled = false
            setHasFixedSize(false)

            // Permitir altura wrap_content com limitação máxima do layout
            layoutParams = layoutParams.apply {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }

            Log.d("SETUP_EXERCICIOS", "Adapter configurado com ${exerciciosAdapter.itemCount} itens")
        }

        // Verificar após configuração
        holder.recyclerViewExercicios.post {
            Log.d("SETUP_EXERCICIOS", "Após configuração:")
            Log.d("SETUP_EXERCICIOS", "Child count: ${holder.recyclerViewExercicios.childCount}")
            Log.d("SETUP_EXERCICIOS", "Altura: ${holder.recyclerViewExercicios.height}")
            Log.d("SETUP_EXERCICIOS", "Adapter count: ${exerciciosAdapter.itemCount}")
        }

        Log.d("SETUP_EXERCICIOS", "===========================")
    }

    // MÉTODO REMOVIDO: addExercicioToFicha (não será mais usado)
    // O exercício será adicionado através da TelaCriarFichaTreino_Funcionario

    private fun removeExercicioFromFicha(fichaPosition: Int, exercicioPosition: Int) {
        Log.d("REMOVE_EXERCICIO", "Removendo exercício $exercicioPosition da ficha $fichaPosition")

        if (fichaPosition >= 0 && fichaPosition < fichas.size) {
            val ficha = fichas[fichaPosition]
            if (exercicioPosition >= 0 && exercicioPosition < ficha.exercicios.size) {
                val exercicioRemovido = ficha.exercicios.removeAt(exercicioPosition)

                // Tentar atualizar o adapter específico
                try {
                    val viewHolder = recyclerView?.findViewHolderForAdapterPosition(fichaPosition) as? FichaViewHolder
                    val exerciciosAdapter = viewHolder?.recyclerViewExercicios?.adapter as? exercicioFunAdapter

                    if (exerciciosAdapter != null && viewHolder != null) {
                        exerciciosAdapter.notifyItemRemoved(exercicioPosition)
                        exerciciosAdapter.notifyItemRangeChanged(exercicioPosition, ficha.exercicios.size)

                        // ATUALIZAR CONTADOR NO VIEWHOLDER ESPECÍFICO
                        atualizarContadorExercicios(viewHolder, ficha)
                    } else {
                        // Fallback: atualizar item completo
                        notifyItemChanged(fichaPosition)
                    }
                } catch (e: Exception) {
                    Log.e("REMOVE_EXERCICIO", "Erro ao notificar adapter específico", e)
                    // Fallback: atualizar item completo
                    notifyItemChanged(fichaPosition)
                }

                // Notificar que a ficha foi alterada
                onFichaAlterada?.invoke(ficha, fichaPosition)

                Log.d("REMOVE_EXERCICIO", "Exercício '${exercicioRemovido.nome}' removido da ficha ${ficha.letra}")
            }
        }
    }

    override fun getItemCount(): Int {
        Log.d("ADAPTER_DEBUG", "getItemCount: ${fichas.size} itens")
        return fichas.size
    }

    // MÉTODO REMOVIDO: showAddExercicioDialog (não será mais usado)

    private fun showDeleteFichaDialog(context: android.content.Context, onDelete: () -> Unit) {
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Excluir Ficha")
            .setMessage("Tem certeza que deseja excluir esta ficha? Esta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                onDelete()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // MÉTODO CRÍTICO: Salvar alterações pendentes dos campos de texto
    fun salvarTodasAlteracoesPendentes() {
        Log.d("FICHA_ADAPTER", "=== SALVANDO ALTERAÇÕES PENDENTES ===")

        try {
            recyclerView?.let { recyclerView ->
                for (i in 0 until recyclerView.childCount) {
                    val child = recyclerView.getChildAt(i)
                    val viewHolder = recyclerView.getChildViewHolder(child) as? FichaViewHolder

                    viewHolder?.let { holder ->
                        val position = holder.adapterPosition
                        if (position >= 0 && position < fichas.size) {
                            val ficha = fichas[position]

                            // CRÍTICO: Capturar valores atuais dos EditTexts
                            val letraAtual = holder.etLetraFicha.text.toString().trim().uppercase()
                            val nomeAtual = holder.etNomeFicha.text.toString().trim()

                            var houveMudanca = false

                            // Atualizar se houve mudança na letra
                            if (letraAtual != ficha.letra && letraAtual.isNotEmpty()) {
                                ficha.letra = letraAtual
                                houveMudanca = true
                                Log.d("SAVE_PENDING", "Letra atualizada para: '$letraAtual' na ficha $position")
                            }

                            // Atualizar se houve mudança no nome
                            if (nomeAtual != ficha.nome && nomeAtual.isNotEmpty()) {
                                ficha.nome = nomeAtual
                                houveMudanca = true
                                Log.d("SAVE_PENDING", "Nome atualizado para: '$nomeAtual' na ficha $position")
                            }

                            // Notificar alteração se houve mudança
                            if (houveMudanca) {
                                onFichaAlterada?.invoke(ficha, position)
                            }

                            // Salvar alterações dos exercícios também
                            val exerciciosAdapter = holder.recyclerViewExercicios.adapter as? exercicioFunAdapter
                            exerciciosAdapter?.salvarAlteracoesPendentes()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FICHA_ADAPTER", "Erro ao salvar alterações pendentes", e)
        }

        Log.d("FICHA_ADAPTER", "=== SALVAMENTO CONCLUÍDO ===")
    }

    // Métodos adicionais
    fun addFicha() {
        val proximaLetra = when (fichas.size) {
            0 -> "A"
            1 -> "B"
            2 -> "C"
            3 -> "D"
            4 -> "E"
            5 -> "F"
            else -> "${('A'.code + fichas.size).toChar()}"
        }

        val novaFicha = fichaTreinoFun(
            id = "ficha_${System.currentTimeMillis()}",
            letra = proximaLetra,
            nome = "Nova Ficha",
            exercicios = mutableListOf()
        )

        fichas.add(novaFicha)
        notifyItemInserted(fichas.size - 1)

        Log.d("ADAPTER_DEBUG", "Nova ficha '$proximaLetra' adicionada")
    }

    fun removeFicha(position: Int) {
        if (position >= 0 && position < fichas.size) {
            val fichaRemovida = fichas.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, fichas.size)

            Log.d("ADAPTER_DEBUG", "Ficha '${fichaRemovida.letra}' removida")
        }
    }

    // Método para obter todas as fichas (útil para salvar no Firebase)
    fun getAllFichas(): List<fichaTreinoFun> = fichas.toList()

    // Método para atualizar uma ficha específica
    fun updateFicha(position: Int, updatedFicha: fichaTreinoFun) {
        if (position >= 0 && position < fichas.size) {
            fichas[position] = updatedFicha
            notifyItemChanged(position)
        }
    }

    // Método público para forçar atualização do contador
    fun atualizarContadores() {
        try {
            recyclerView?.let { recyclerView ->
                for (i in 0 until recyclerView.childCount) {
                    val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i)) as? FichaViewHolder
                    viewHolder?.let { holder ->
                        val position = holder.adapterPosition
                        if (position >= 0 && position < fichas.size) {
                            atualizarContadorExercicios(holder, fichas[position])
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FICHA_ADAPTER", "Erro ao atualizar contadores", e)
        }
    }
}





/*
package com.example.unipump.adapters

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.R
import com.example.unipump.models.fichaTreinoFun

class FichaTreinoFunAdapter(
    private val fichas: MutableList<fichaTreinoFun>,
    private val onExcluirFicha: ((String, Int) -> Unit)? = null,
    private val onFichaAlterada: ((fichaTreinoFun, Int) -> Unit)? = null
) : RecyclerView.Adapter<FichaTreinoFunAdapter.FichaViewHolder>() {

    // Referência ao RecyclerView principal
    private var recyclerView: RecyclerView? = null

    class FichaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val etLetraFicha: EditText = itemView.findViewById(R.id.etLetraFicha)
        val etNomeFicha: EditText = itemView.findViewById(R.id.etNomeFicha)
        val tvQuantidadeExercicios: TextView = itemView.findViewById(R.id.tvQuantidadeExercicios)
        val btnExcluirFicha: ImageButton = itemView.findViewById(R.id.btnExcluirFicha)
        val btnAddExercicio: ImageButton = itemView.findViewById(R.id.btnAddExercicio)
        val recyclerViewExercicios: RecyclerView = itemView.findViewById(R.id.recyclerViewExercicios)

        // NOVO: Variáveis para controlar TextWatchers
        private var letraTextWatcher: TextWatcher? = null
        private var nomeTextWatcher: TextWatcher? = null

        fun clearTextWatchers() {
            letraTextWatcher?.let { etLetraFicha.removeTextChangedListener(it) }
            nomeTextWatcher?.let { etNomeFicha.removeTextChangedListener(it) }
            letraTextWatcher = null
            nomeTextWatcher = null
        }

        fun setTextWatchers(
            onLetraChanged: (String) -> Unit,
            onNomeChanged: (String) -> Unit
        ) {
            // Remover watchers antigos
            clearTextWatchers()

            // Criar novos TextWatchers
            letraTextWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val novaLetra = s.toString().trim().uppercase()
                    onLetraChanged(novaLetra)
                }
            }

            nomeTextWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val novoNome = s.toString().trim()
                    onNomeChanged(novoNome)
                }
            }

            // Adicionar os novos watchers
            etLetraFicha.addTextChangedListener(letraTextWatcher)
            etNomeFicha.addTextChangedListener(nomeTextWatcher)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FichaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ficha_treino_funcionario, parent, false)
        return FichaViewHolder(view)
    }

    override fun onBindViewHolder(holder: FichaViewHolder, position: Int) {
        val ficha = fichas[position]
        Log.d("ADAPTER_DEBUG", "onBindViewHolder: position $position de ${fichas.size}")
        Log.d("ADAPTER_DEBUG", "Ficha: ${ficha.letra} - ${ficha.nome} (${ficha.exercicios.size} exercícios)")

        // CRÍTICO: Limpar watchers antigos antes de preencher
        holder.clearTextWatchers()

        // Preencher dados da ficha
        holder.etLetraFicha.setText(ficha.letra)
        holder.etNomeFicha.setText(ficha.nome)

        // ATUALIZAR O CONTADOR DE EXERCÍCIOS
        atualizarContadorExercicios(holder, ficha)

        // NOVO: Configurar TextWatchers otimizados
        holder.setTextWatchers(
            onLetraChanged = { novaLetra ->
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION && currentPosition < fichas.size) {
                    val fichaAtual = fichas[currentPosition]
                    if (novaLetra != fichaAtual.letra && novaLetra.isNotEmpty()) {
                        // Atualizar diretamente o objeto (referência)
                        fichaAtual.letra = novaLetra

                        Log.d("LETRA_ALTERADA", "Letra alterada para: '$novaLetra' na posição $currentPosition")

                        // Notificar alteração com debounce
                        onFichaAlterada?.invoke(fichaAtual, currentPosition)
                    }
                }
            },
            onNomeChanged = { novoNome ->
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION && currentPosition < fichas.size) {
                    val fichaAtual = fichas[currentPosition]
                    if (novoNome != fichaAtual.nome && novoNome.isNotEmpty()) {
                        // Atualizar diretamente o objeto (referência)
                        fichaAtual.nome = novoNome

                        Log.d("NOME_ALTERADO", "Nome alterado para: '$novoNome' na posição $currentPosition")

                        // Notificar alteração com debounce
                        onFichaAlterada?.invoke(fichaAtual, currentPosition)
                    }
                }
            }
        )

        // CONFIGURAR RecyclerView dos exercícios
        setupExerciciosRecyclerView(holder, ficha, position)

        // Botão para adicionar exercício
        holder.btnAddExercicio.setOnClickListener {
            showAddExercicioDialog(holder.itemView.context) { nomeExercicio ->
                addExercicioToFicha(position, nomeExercicio)
            }
        }

        // Botão para excluir ficha
        holder.btnExcluirFicha.setOnClickListener {
            showDeleteFichaDialog(holder.itemView.context) {
                onExcluirFicha?.invoke(ficha.id, position)
            }
        }
    }

    // Método para atualizar contador de exercícios
    private fun atualizarContadorExercicios(holder: FichaViewHolder, ficha: fichaTreinoFun) {
        val quantidadeExercicios = ficha.exercicios.size
        val texto = when (quantidadeExercicios) {
            0 -> "0 exercícios"
            1 -> "1 exercício"
            else -> "$quantidadeExercicios exercícios"
        }

        holder.tvQuantidadeExercicios.text = texto
        Log.d("CONTADOR_EXERCICIOS", "Ficha ${ficha.letra}: $texto")
    }

    private fun setupExerciciosRecyclerView(holder: FichaViewHolder, ficha: fichaTreinoFun, fichaPosition: Int) {
        Log.d("SETUP_EXERCICIOS", "=== CONFIGURANDO EXERCÍCIOS ===")
        Log.d("SETUP_EXERCICIOS", "Ficha: ${ficha.letra}")
        Log.d("SETUP_EXERCICIOS", "Número de exercícios: ${ficha.exercicios.size}")

        // Listar todos os exercícios para debug
        ficha.exercicios.forEachIndexed { index, exercicio ->
            Log.d("SETUP_EXERCICIOS", "Exercício $index: ${exercicio.nome} (${exercicio.series.size} séries)")
        }

        val exerciciosAdapter = exercicioFunAdapter(
            exercicios = ficha.exercicios,
            onDeleteExercicio = { exercicioPosition: Int ->
                // Callback para deletar exercício
                removeExercicioFromFicha(fichaPosition, exercicioPosition)
                // ATUALIZAR CONTADOR APÓS REMOVER
                atualizarContadorExercicios(holder, ficha)
            },
            onAdicionarSerie = { exercicioPosition: Int ->
                // Callback para quando série for adicionada
                Log.d("FICHA_ADAPTER", "Série adicionada ao exercício $exercicioPosition da ficha ${ficha.letra}")

                // Notificar que a ficha foi alterada
                onFichaAlterada?.invoke(ficha, fichaPosition)
            },
            onExercicioAlterado = { exercicioAlterado: com.example.unipump.models.exercicioFun, exercicioPosition: Int ->
                // Callback para quando exercício for alterado
                Log.d("FICHA_ADAPTER", "Exercício alterado: ${exercicioAlterado.nome} na ficha ${ficha.letra}")

                // Atualizar o exercício na ficha
                if (exercicioPosition >= 0 && exercicioPosition < ficha.exercicios.size) {
                    ficha.exercicios[exercicioPosition] = exercicioAlterado
                }

                // Notificar que a ficha foi alterada
                onFichaAlterada?.invoke(ficha, fichaPosition)
            }
        )

        holder.recyclerViewExercicios.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = exerciciosAdapter

            // CONFIGURAÇÕES CRÍTICAS
            isNestedScrollingEnabled = false
            setHasFixedSize(false)

            // Permitir altura wrap_content com limitação máxima do layout
            layoutParams = layoutParams.apply {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }

            Log.d("SETUP_EXERCICIOS", "Adapter configurado com ${exerciciosAdapter.itemCount} itens")
        }

        // Verificar após configuração
        holder.recyclerViewExercicios.post {
            Log.d("SETUP_EXERCICIOS", "Após configuração:")
            Log.d("SETUP_EXERCICIOS", "Child count: ${holder.recyclerViewExercicios.childCount}")
            Log.d("SETUP_EXERCICIOS", "Altura: ${holder.recyclerViewExercicios.height}")
            Log.d("SETUP_EXERCICIOS", "Adapter count: ${exerciciosAdapter.itemCount}")
        }

        Log.d("SETUP_EXERCICIOS", "===========================")
    }

    private fun addExercicioToFicha(fichaPosition: Int, nomeExercicio: String) {
        Log.d("ADD_EXERCICIO", "=== ADICIONANDO EXERCÍCIO ===")
        Log.d("ADD_EXERCICIO", "Posição da ficha: $fichaPosition")
        Log.d("ADD_EXERCICIO", "Nome do exercício: $nomeExercicio")

        if (fichaPosition >= 0 && fichaPosition < fichas.size) {
            val ficha = fichas[fichaPosition]
            Log.d("ADD_EXERCICIO", "Ficha encontrada: ${ficha.letra}")
            Log.d("ADD_EXERCICIO", "Exercícios antes: ${ficha.exercicios.size}")

            // Criar novo exercício com uma série padrão
            val novoExercicio = com.example.unipump.models.exercicioFun(
                id = "ex_${System.currentTimeMillis()}",
                nome = nomeExercicio,
                series = mutableListOf(
                    com.example.unipump.models.serieFun(
                        id = "serie_${System.currentTimeMillis()}",
                        numero = 1,
                        repeticoes = 12,
                        peso = "",
                        tempo = "60"
                    )
                )
            )

            // Adicionar à lista de exercícios da ficha
            ficha.exercicios.add(novoExercicio)
            Log.d("ADD_EXERCICIO", "Exercícios depois: ${ficha.exercicios.size}")

            // Notificar que a ficha foi alterada
            onFichaAlterada?.invoke(ficha, fichaPosition)

            // ATUALIZAR CONTADOR E ADAPTER ESPECÍFICO
            try {
                val viewHolder = recyclerView?.findViewHolderForAdapterPosition(fichaPosition) as? FichaViewHolder
                val exerciciosAdapter = viewHolder?.recyclerViewExercicios?.adapter as? exercicioFunAdapter

                if (exerciciosAdapter != null && viewHolder != null) {
                    Log.d("ADD_EXERCICIO", "Notificando adapter específico dos exercícios")
                    exerciciosAdapter.notifyItemInserted(ficha.exercicios.size - 1)

                    // ATUALIZAR CONTADOR NO VIEWHOLDER ESPECÍFICO
                    atualizarContadorExercicios(viewHolder, ficha)
                } else {
                    Log.w("ADD_EXERCICIO", "Adapter dos exercícios não encontrado")
                    // Fallback: atualizar item completo
                    notifyItemChanged(fichaPosition)
                }
            } catch (e: Exception) {
                Log.e("ADD_EXERCICIO", "Erro ao notificar adapter específico", e)
                // Fallback: atualizar item completo
                notifyItemChanged(fichaPosition)
            }

            Log.d("ADD_EXERCICIO", "Exercício '$nomeExercicio' adicionado à ficha ${ficha.letra}")
        } else {
            Log.e("ADD_EXERCICIO", "Posição inválida: $fichaPosition (total: ${fichas.size})")
        }

        Log.d("ADD_EXERCICIO", "============================")
    }

    private fun removeExercicioFromFicha(fichaPosition: Int, exercicioPosition: Int) {
        Log.d("REMOVE_EXERCICIO", "Removendo exercício $exercicioPosition da ficha $fichaPosition")

        if (fichaPosition >= 0 && fichaPosition < fichas.size) {
            val ficha = fichas[fichaPosition]
            if (exercicioPosition >= 0 && exercicioPosition < ficha.exercicios.size) {
                val exercicioRemovido = ficha.exercicios.removeAt(exercicioPosition)

                // Tentar atualizar o adapter específico
                try {
                    val viewHolder = recyclerView?.findViewHolderForAdapterPosition(fichaPosition) as? FichaViewHolder
                    val exerciciosAdapter = viewHolder?.recyclerViewExercicios?.adapter as? exercicioFunAdapter

                    if (exerciciosAdapter != null && viewHolder != null) {
                        exerciciosAdapter.notifyItemRemoved(exercicioPosition)
                        exerciciosAdapter.notifyItemRangeChanged(exercicioPosition, ficha.exercicios.size)

                        // ATUALIZAR CONTADOR NO VIEWHOLDER ESPECÍFICO
                        atualizarContadorExercicios(viewHolder, ficha)
                    } else {
                        // Fallback: atualizar item completo
                        notifyItemChanged(fichaPosition)
                    }
                } catch (e: Exception) {
                    Log.e("REMOVE_EXERCICIO", "Erro ao notificar adapter específico", e)
                    // Fallback: atualizar item completo
                    notifyItemChanged(fichaPosition)
                }

                // Notificar que a ficha foi alterada
                onFichaAlterada?.invoke(ficha, fichaPosition)

                Log.d("REMOVE_EXERCICIO", "Exercício '${exercicioRemovido.nome}' removido da ficha ${ficha.letra}")
            }
        }
    }

    override fun getItemCount(): Int {
        Log.d("ADAPTER_DEBUG", "getItemCount: ${fichas.size} itens")
        return fichas.size
    }

    private fun showAddExercicioDialog(context: android.content.Context, onAdd: (String) -> Unit) {
        val builder = android.app.AlertDialog.Builder(context)
        val input = android.widget.EditText(context).apply {
            hint = "Nome do exercício"
            setPadding(32, 32, 32, 32)
        }

        builder.setTitle("Adicionar Exercício")
            .setView(input)
            .setPositiveButton("Adicionar") { _, _ ->
                val nomeExercicio = input.text.toString().trim()
                if (nomeExercicio.isNotEmpty()) {
                    onAdd(nomeExercicio)
                } else {
                    android.widget.Toast.makeText(context, "Digite o nome do exercício", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteFichaDialog(context: android.content.Context, onDelete: () -> Unit) {
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Excluir Ficha")
            .setMessage("Tem certeza que deseja excluir esta ficha? Esta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                onDelete()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // MÉTODO CRÍTICO: Salvar alterações pendentes dos campos de texto
    fun salvarTodasAlteracoesPendentes() {
        Log.d("FICHA_ADAPTER", "=== SALVANDO ALTERAÇÕES PENDENTES ===")

        try {
            recyclerView?.let { recyclerView ->
                for (i in 0 until recyclerView.childCount) {
                    val child = recyclerView.getChildAt(i)
                    val viewHolder = recyclerView.getChildViewHolder(child) as? FichaViewHolder

                    viewHolder?.let { holder ->
                        val position = holder.adapterPosition
                        if (position >= 0 && position < fichas.size) {
                            val ficha = fichas[position]

                            // CRÍTICO: Capturar valores atuais dos EditTexts
                            val letraAtual = holder.etLetraFicha.text.toString().trim().uppercase()
                            val nomeAtual = holder.etNomeFicha.text.toString().trim()

                            var houveMudanca = false

                            // Atualizar se houve mudança na letra
                            if (letraAtual != ficha.letra && letraAtual.isNotEmpty()) {
                                ficha.letra = letraAtual
                                houveMudanca = true
                                Log.d("SAVE_PENDING", "Letra atualizada para: '$letraAtual' na ficha $position")
                            }

                            // Atualizar se houve mudança no nome
                            if (nomeAtual != ficha.nome && nomeAtual.isNotEmpty()) {
                                ficha.nome = nomeAtual
                                houveMudanca = true
                                Log.d("SAVE_PENDING", "Nome atualizado para: '$nomeAtual' na ficha $position")
                            }

                            // Notificar alteração se houve mudança
                            if (houveMudanca) {
                                onFichaAlterada?.invoke(ficha, position)
                            }

                            // Salvar alterações dos exercícios também
                            val exerciciosAdapter = holder.recyclerViewExercicios.adapter as? exercicioFunAdapter
                            exerciciosAdapter?.salvarAlteracoesPendentes()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FICHA_ADAPTER", "Erro ao salvar alterações pendentes", e)
        }

        Log.d("FICHA_ADAPTER", "=== SALVAMENTO CONCLUÍDO ===")
    }

    // Métodos adicionais
    fun addFicha() {
        val proximaLetra = when (fichas.size) {
            0 -> "A"
            1 -> "B"
            2 -> "C"
            3 -> "D"
            4 -> "E"
            5 -> "F"
            else -> "${('A'.code + fichas.size).toChar()}"
        }

        val novaFicha = fichaTreinoFun(
            id = "ficha_${System.currentTimeMillis()}",
            letra = proximaLetra,
            nome = "Nova Ficha",
            exercicios = mutableListOf()
        )

        fichas.add(novaFicha)
        notifyItemInserted(fichas.size - 1)

        Log.d("ADAPTER_DEBUG", "Nova ficha '$proximaLetra' adicionada")
    }

    fun removeFicha(position: Int) {
        if (position >= 0 && position < fichas.size) {
            val fichaRemovida = fichas.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, fichas.size)

            Log.d("ADAPTER_DEBUG", "Ficha '${fichaRemovida.letra}' removida")
        }
    }

    // Método para obter todas as fichas (útil para salvar no Firebase)
    fun getAllFichas(): List<fichaTreinoFun> = fichas.toList()

    // Método para atualizar uma ficha específica
    fun updateFicha(position: Int, updatedFicha: fichaTreinoFun) {
        if (position >= 0 && position < fichas.size) {
            fichas[position] = updatedFicha
            notifyItemChanged(position)
        }
    }

    // Método público para forçar atualização do contador
    fun atualizarContadores() {
        try {
            recyclerView?.let { recyclerView ->
                for (i in 0 until recyclerView.childCount) {
                    val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i)) as? FichaViewHolder
                    viewHolder?.let { holder ->
                        val position = holder.adapterPosition
                        if (position >= 0 && position < fichas.size) {
                            atualizarContadorExercicios(holder, fichas[position])
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FICHA_ADAPTER", "Erro ao atualizar contadores", e)
        }
    }
}*/
