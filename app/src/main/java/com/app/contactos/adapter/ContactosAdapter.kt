package com.app.contactos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.contactos.databinding.ItemContactoBinding
import com.app.contactos.model.Contacto

/** interfaz para manejar los eventos de clic en los botones de llamada y mensaje.*/
interface OnContactActionListener {
    fun onCallClick(telefono: String)
    fun onMessageClick(telefono: String)
    fun onItemClick(contacto: Contacto)
    fun onLinkedinClick(profile: String)
    fun onWebsiteClick(url: String)
}

/**
 * Adaptador para el RecyclerView que muestra la lista de contactos.
 * Utiliza ListAdapter para manejar eficientemente las actualizaciones de la lista.
 * @param onItemClicked Lambda que se ejecuta cuando se hace clic en un contacto.
 */
class ContactosAdapter(private val listener: OnContactActionListener) :
    ListAdapter<Contacto, ContactosAdapter.ContactoViewHolder>(DiffCallback) {


     //ViewHolder que contiene la vista de un solo item de contacto.

    inner class ContactoViewHolder(private var binding: ItemContactoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Vincula los datos del contacto a las vistas del layout.
         */
        fun bind(contacto: Contacto, actionListener: OnContactActionListener) {
            // Usa el data binding para asignar el objeto contacto a la variable del layout
            binding.contacto = contacto
            // Llama a executePendingBindings() para forzar la actualización inmediata del layout.
            // Es una buena práctica para evitar problemas de reciclaje de vistas.
            binding.executePendingBindings()

            //Configurar listeners para los botones y el item completo ---
            binding.ivCall.setOnClickListener {
                actionListener.onCallClick(contacto.telefono)
            }

            binding.ivMessage.setOnClickListener {
                actionListener.onMessageClick(contacto.telefono)
            }

            itemView.setOnClickListener {
                actionListener.onItemClick(contacto)
            }
            binding.ivLinkedin.setOnClickListener {
                if (contacto.linkedin.isNotEmpty()) {
                    actionListener.onLinkedinClick(contacto.linkedin)
                }
            }
            binding.ivWebsite.setOnClickListener {
                if (contacto.website.isNotEmpty()) {
                    actionListener.onWebsiteClick(contacto.website)
                }
            }
        }
    }

    /**
     * Callback para calcular las diferencias entre dos listas de contactos.
     */
    companion object DiffCallback : DiffUtil.ItemCallback<Contacto>() {
        /**
         * Comprueba si los items son los mismos (por su ID).
         */
        override fun areItemsTheSame(oldItem: Contacto, newItem: Contacto): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * Comprueba si el contenido de los items ha cambiado.
         */
        override fun areContentsTheSame(oldItem: Contacto, newItem: Contacto): Boolean {
            // La data class genera automáticamente el método equals(), que compara todos los campos.
            return oldItem == newItem
        }
    }

    /**
     * Crea nuevos ViewHolders cuando el RecyclerView lo necesita.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactoViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        // Infla el layout del item usando ViewBinding
        val binding = ItemContactoBinding.inflate(layoutInflater, parent, false)
        return ContactoViewHolder(binding)
    }

    /**
     * Vincula los datos de un contacto a un ViewHolder en una posición específica.
     */
    override fun onBindViewHolder(holder: ContactoViewHolder, position: Int) {
        val contacto = getItem(position)
        // Configura el listener para el clic en el item
        holder.bind(contacto, listener)
    }
}