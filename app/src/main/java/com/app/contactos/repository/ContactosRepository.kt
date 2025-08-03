package com.app.contactos.repository

import android.content.ContentResolver
import android.provider.ContactsContract
import androidx.lifecycle.LiveData
import com.app.contactos.database.CategoriaDao
import com.app.contactos.database.ContactoConGrupos
import com.app.contactos.database.ContactoDao
import com.app.contactos.database.ContactoGrupoCrossRef
import com.app.contactos.database.GrupoDao
import com.app.contactos.model.Categoria
import com.app.contactos.model.Contacto
import com.app.contactos.model.Grupo

/**
 * Repositorio que maneja las operaciones de datos para los contactos y categorías.
 */
class ContactosRepository(
    private val contactoDao: ContactoDao,
    private val categoriaDao: CategoriaDao,
    private val grupoDao: GrupoDao
) {

    /**
     * Obtiene todos los contactos como un objeto LiveData.
     */
    val todosLosContactos: LiveData<List<Contacto>> = contactoDao.getAllContactos()

    /**
     * Obtiene todas las categorías como un objeto LiveData.
     */
    val todasLasCategorias: LiveData<List<Categoria>> = categoriaDao.getAllCategorias()

    /**
     * Busca contactos que coincidan con la consulta de búsqueda.
     */
    fun buscarContactos(query: String): LiveData<List<Contacto>> {
        return contactoDao.searchContactos("%$query%")
    }

    /**
     * Inserta un nuevo contacto en la base de datos.
     */
    suspend fun insertarContacto(contacto: Contacto): Long {
        return contactoDao.insert(contacto)
    }

    /**
     * Actualiza un contacto existente en la base de datos.
     */
    suspend fun actualizarContacto(contacto: Contacto) {
        contactoDao.update(contacto)
    }

    /**
     * Elimina un contacto de la base de datos.
     */
    suspend fun eliminarContacto(contacto: Contacto) {
        contactoDao.delete(contacto)
    }

    /**
     * Inserta una nueva categoría en la base de datos.
     */
    suspend fun insertarCategoria(categoria: Categoria) {
        categoriaDao.insert(categoria)
    }


    /**
     * Obtiene todos los contactos para realizar una copia de seguridad.
     */
    suspend fun obtenerContactosParaBackup(): List<Contacto> {
        return contactoDao.getAllContactosForBackup()
    }

    /**
     * Restaura los contactos a partir de una lista.
     */
    suspend fun restaurarContactos(contactos: List<Contacto>) {
        contactos.forEach { contacto ->
            contactoDao.insert(contacto)
        }
    }

    /**
     * Obtiene un contacto por su ID desde la Bd.
     */
    fun getContactoById(contactoId: Int): LiveData<Contacto> {
        return contactoDao.getContactoById(contactoId)
    }

    /**
     * Lee los contactos del dispositivo y los inserta en la BBDD local si no existen.
     */
    suspend fun importarDesdeDispositivo(contentResolver: ContentResolver) {
        val nuevosContactos = mutableListOf<Contacto>()
        val telefonosExistentes = contactoDao.getTodosComoLista().map { it.telefono }.toSet()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val nombre = it.getString(nameIndex)
                val telefono = it.getString(numberIndex).replace("\\s".toRegex(), "") // Limpiar espacios

                // Evitar duplicados basados en el número de teléfono
                if (telefono.isNotBlank() && !telefonosExistentes.contains(telefono)) {
                    // Por defecto, se asigna a la categoría 1 (o la que se prefiera)
                    nuevosContactos.add(Contacto(nombre = nombre, telefono = telefono, email = "", categoriaId = 1))
                }
            }
        }

        if (nuevosContactos.isNotEmpty()) {
            contactoDao.insertarVarios(nuevosContactos)
        }
    }

    /** Obtiene todos los grupos. */
    val todosLosGrupos: LiveData<List<Grupo>> = grupoDao.getTodosLosGrupos()

    /** Crea un nuevo grupo. */
    suspend fun crearGrupo(grupo: Grupo) {
        grupoDao.crearGrupo(grupo)
    }

    /** Obtiene los grupos de un contacto. */
    fun getGruposDeUnContacto(contactoId: Int): LiveData<ContactoConGrupos> {
        return grupoDao.getGruposDeUnContacto(contactoId)
    }

    /**
     * Actualiza las asociaciones de un contacto a los grupos.
     * Borra las antiguas y añade las nuevas.
     */
    suspend fun actualizarGruposDeContacto(contactoId: Int, nuevosGrupoIds: List<Int>) {

        // Limpiamos referencias viejas
        //grupoDao.limpiarGruposDeContacto(contactoId)

        // Añadimos las nuevas
        nuevosGrupoIds.forEach { grupoId ->
            grupoDao.addContactoAGrupo(ContactoGrupoCrossRef(contactoId, grupoId))
        }
    }


}