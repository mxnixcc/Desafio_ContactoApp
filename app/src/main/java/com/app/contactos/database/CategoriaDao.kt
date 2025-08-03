package com.app.contactos.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.contactos.model.Categoria

/**
 * Objeto de Acceso a Datos (DAO) para la entidad Categoria.
 */
@Dao
interface CategoriaDao {


     // Inserta una nueva categoría en la base de datos.

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(categoria: Categoria)


    // Obtiene todas las categorías de la base de datos como LiveData.

    @Query("SELECT * FROM categorias ORDER BY nombre ASC")
    fun getAllCategorias(): LiveData<List<Categoria>>
}
