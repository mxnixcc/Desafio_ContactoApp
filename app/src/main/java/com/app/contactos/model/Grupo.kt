package com.app.contactos.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa un grupo de contactos.
 */
@Entity(tableName = "grupos_table")
data class Grupo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String
)