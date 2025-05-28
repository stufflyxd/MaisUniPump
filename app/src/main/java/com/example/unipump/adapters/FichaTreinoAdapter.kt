import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.R
import com.example.unipump.models.FichaTreino

class FichaTreinoAdapter(
    private var fichasTreino: MutableList<FichaTreino>,
    private val clickListener: OnFichaTreinoClickListener
) : RecyclerView.Adapter<FichaTreinoAdapter.FichaTreinoViewHolder>() {

    // Interface para clique nos itens
    interface OnFichaTreinoClickListener {
        fun onFichaTreinoClick(fichaTreino: FichaTreino, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FichaTreinoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ficha_treino, parent, false)
        return FichaTreinoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FichaTreinoViewHolder, position: Int) {
        val ficha = fichasTreino[position]
        holder.bind(ficha, position)
    }

    override fun getItemCount(): Int = fichasTreino.size

    // Método para atualizar a lista
    fun updateList(novasFichas: MutableList<FichaTreino>) {
        this.fichasTreino = novasFichas
        notifyDataSetChanged()
    }

    // ViewHolder
    inner class FichaTreinoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLetraFicha: TextView = itemView.findViewById(R.id.tvLetraFicha)
        private val tvNomeFicha: TextView = itemView.findViewById(R.id.tvNomeFicha)
        private val tvDescricaoFicha: TextView = itemView.findViewById(R.id.tvDescricaoFicha)
        /*private val ivSetaFicha: ImageView = itemView.findViewById(R.id.ivSetaFicha)*/

        init {
            // Configurar clique no item
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    clickListener.onFichaTreinoClick(fichasTreino[position], position)
                }
            }
        }

        fun bind(ficha: FichaTreino, position: Int) {
            tvLetraFicha.text = ficha.letra
            tvNomeFicha.text = ficha.nome
            tvDescricaoFicha.text = ficha.getDescricaoFormatada()
        }
    }
}