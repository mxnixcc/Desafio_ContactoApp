package com.app.contactos.utils

import android.content.Context
import android.net.Uri
import com.app.contactos.model.Contacto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Utilidades para gestionar la copia de seguridad y restauración de contactos.
 * Nota: Requiere la librería Gson en build.gradle: implementation 'com.google.code.gson:gson:2.9.0'
 */
object BackupUtils {

    /**
     * Escribe la lista de contactos en formato JSON en la URI proporcionada.
     */
    fun escribirBackup(context: Context, contactos: List<Contacto>, uri: Uri): Boolean {
        val gson = Gson()
        val jsonString = gson.toJson(contactos)
        try {
            // Usa el ContentResolver para abrir un flujo de salida a la URI seleccionada por el usuario
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.writer().use {
                    it.write(jsonString)
                }
            }
            return true
        } catch (e: Exception) {
            // Manejar errores de escritura de archivo
            e.printStackTrace()
            return false
        }
    }

    /**
     * Lee un archivo de backup desde una URI y lo convierte en una lista de contactos.
     */
    fun restaurarDesdeBackup(context: Context, uri: Uri): List<Contacto>? {
        val gson = Gson()
        // Define el tipo de dato esperado para la deserialización (una lista de Contacto)
        val listType = object : TypeToken<List<Contacto>>() {}.type

        try {
            // Usa el ContentResolver para abrir un flujo de entrada desde la URI seleccionada
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    return gson.fromJson(reader, listType)
                }
            }
            return null
        } catch (e: Exception) {
            // Manejar errores de lectura o formato JSON incorrecto
            e.printStackTrace()
            return null
        }
    }
}