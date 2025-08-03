package com.app.contactos.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.app.contactos.model.Grupo

/** Objeto de Acceso a Datos (DAO) para la entidad Grupo. */
@Dao
interface GrupoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun crearGrupo(grupo: Grupo)

    @Query("SELECT * FROM grupos_table ORDER BY nombre ASC")
    fun getTodosLosGrupos(): LiveData<List<Grupo>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addContactoAGrupo(crossRef: ContactoGrupoCrossRef)

    @Delete
    suspend fun removeContactoDeGrupo(crossRef: ContactoGrupoCrossRef)

    @Transaction
    @Query("SELECT * FROM contactos WHERE id = :contactoId")
    fun getGruposDeUnContacto(contactoId: Int): LiveData<ContactoConGrupos>

    @Query("DELETE FROM grupos_table WHERE id = :contactoId")
    fun limpiarGruposDeContacto(contactoId: Int)

}