package com.app.contactos.utils

import android.util.Patterns

/**
 * Objeto de utilidad para realizar validaciones en los formularios.
 */
object ValidationUtils {

    /**
     * Valida que el nombre del contacto no esté vacío.
     */
    fun isNombreValido(nombre: String): Boolean {
        return nombre.isNotBlank()
    }

    /**
     * Valida que el número de teléfono no esté vacío.
     */
    fun isTelefonoValido(telefono: String): Boolean {
        return telefono.isNotBlank()
    }

    /**
     * Valida que el email tenga un formato correcto (si se ha introducido uno).
     */
    fun isEmailValido(email: String): Boolean {
        // El email es opcional, pero si se escribe,tiene que ser válido
        return email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}