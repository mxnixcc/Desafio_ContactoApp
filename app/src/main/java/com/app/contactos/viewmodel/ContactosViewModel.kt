package com.app.contactos.viewmodel

import android.content.ContentResolver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.app.contactos.model.Contacto
import com.app.contactos.repository.ContactosRepository
import kotlinx.coroutines.launch

/**Enum para representar los estados de la importacion*/
enum class EstadoImportacion { VACIO, CARGANDO, EXITO, ERROR }

/**
 * ViewModel para gestionar la lista principal de contactos.
 */
class ContactosViewModel(private val repository: ContactosRepository) : ViewModel() {

    private val _searchQuery = MutableLiveData<String>("")

    /**LiveData para comunicar el estado de la importación a la UI */
    private val _estadoImportacion = MutableLiveData<EstadoImportacion>(EstadoImportacion.VACIO)
    val estadoImportacion: LiveData<EstadoImportacion> get() = _estadoImportacion

    /**
     * LiveData que expone la lista de contactos, se actualiza según la búsqueda.
     */
    val contactos: LiveData<List<Contacto>> = _searchQuery.switchMap { query ->
        if (query.isNullOrEmpty()) {
            repository.todosLosContactos
        } else {
            repository.buscarContactos(query)
        }
    }

    /**
     * Inicia una búsqueda de contactos.
     */
    fun buscarContacto(query: String) {
        _searchQuery.value = query
    }

    /**
     * Elimina un contacto de la base de datos.
     */
    fun eliminarContacto(contacto: Contacto) = viewModelScope.launch {
        repository.eliminarContacto(contacto)
    }

    /**
     * Devuelve el valor actual de la lista de contactos.
     */
    fun getContactosParaExportar(): List<Contacto>? {
        return contactos.value
    }

    /**
     * Inicia el proceso de importación de contactos desde el dispositivo.
     * @param contentResolver El ContentResolver necesario para consultar los contactos del sistema.
     */
    fun importarContactosDelDispositivo(contentResolver: ContentResolver) {
        viewModelScope.launch {
            _estadoImportacion.value = EstadoImportacion.CARGANDO
            try {
                repository.importarDesdeDispositivo(contentResolver)
                _estadoImportacion.value = EstadoImportacion.EXITO
            } catch (e: Exception) {
                // Puedes pasar el mensaje de error si quieres ser más específico
                _estadoImportacion.value = EstadoImportacion.ERROR
            }
        }
    }

    /**Función para resetear el estado de importación */
    fun resetearEstadoImportacion() {
        _estadoImportacion.value = EstadoImportacion.VACIO
    }
}

/**
 * Fábrica para crear una instancia de ContactosViewModel con dependencias.
 */
class ContactosViewModelFactory(private val repository: ContactosRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContactosViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}