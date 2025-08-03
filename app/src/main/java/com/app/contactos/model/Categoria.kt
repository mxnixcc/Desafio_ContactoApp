package com.app.contactos.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tenemos categoría para un contacto.
 */
@Entity(tableName = "categorias")
data class Categoria(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String
)