package com.app.contactos.database

import androidx.room.Entity


 // Tabla de unión para la relación muchos a muchos entre Contacto y Grupo.

@Entity(tableName = "contacto_grupo_cross_ref", primaryKeys = ["contactoId", "grupoId"])
data class ContactoGrupoCrossRef(
    val contactoId: Int,
    val grupoId: Int
)