package com.app.contactos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.contactos.adapter.ContactosAdapter
import com.app.contactos.adapter.OnContactActionListener
import com.app.contactos.database.ContactosDatabase
import com.app.contactos.databinding.ActivityMainBinding
import com.app.contactos.model.Contacto
import com.app.contactos.repository.ContactosRepository
import com.app.contactos.utils.BackupUtils
import com.app.contactos.utils.VCardUtils
import com.app.contactos.viewmodel.ContactosViewModel
import com.app.contactos.viewmodel.ContactosViewModelFactory
import com.app.contactos.viewmodel.EstadoImportacion
import com.google.android.material.snackbar.Snackbar


/**
 * Actividad principal que muestra la lista de contactos.
 */
class MainActivity : AppCompatActivity(), OnContactActionListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ContactosAdapter

    private val viewModel: ContactosViewModel by viewModels {
        // Inicializa el ViewModel usando la fábrica para inyectar el repositorio.
        val database = ContactosDatabase.getDatabase(context = applicationContext, coroutineScope = lifecycleScope)
        val repository = ContactosRepository(database.contactoDao(), database.categoriaDao(), database.grupoDao())
        ContactosViewModelFactory(repository)
    }

    /**Variable para guardar el número a llamar mientras se pide permiso */
    private var numeroParaLlamar: String? = null

    /**Launcher para solicitar el permiso de llamada */
    private val requestCallPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permiso concedido, intentamos llamar de nuevo.
                numeroParaLlamar?.let { realizarLlamada(it) }
            } else {
                Toast.makeText(this, "Permiso de llamada denegado.", Toast.LENGTH_SHORT).show()
            }
        }

    /**Launcher para crear el archivo de backup*/
    private val crearBackupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            viewModel.contactos.value?.let { contactos ->
                val success = BackupUtils.escribirBackup(this, contactos, it)
                val message = if (success) "Copia de seguridad creada" else "Error al crear la copia"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**Launcher para abrir el archivo de backup*/
    private val restaurarBackupLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val contactosRestaurados = BackupUtils.restaurarDesdeBackup(this, it)
            if (contactosRestaurados != null) {
                // Aquí deberías insertar los contactos en la base de datos a través del ViewModel
                // Por simplicidad, solo mostramos un mensaje.
                Toast.makeText(this, "${contactosRestaurados.size} contactos restaurados.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Error al restaurar la copia de seguridad.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**Launcher para exportar contactos a VCard*/
    private val vcardExportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/vcard")) { uri ->
        uri?.let {
            val contactos = viewModel.getContactosParaExportar()
            if (contactos.isNullOrEmpty()) {
                Toast.makeText(this, "No hay contactos para exportar.", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            try {
                val vcardString = VCardUtils.exportToVCard(contactos)
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(vcardString.toByteArray())
                }
                Toast.makeText(this, "Contactos exportados correctamente.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error al exportar: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Error exportando a VCard", e)
            }
        }
    }

    /**Launcher para solicitar el permiso de lectura de contactos */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permiso concedido, procedemos a importar.
                viewModel.importarContactosDelDispositivo(contentResolver)
            } else {
                // Permiso denegado. Informamos al usuario.
                Toast.makeText(this, "Permiso necesario para importar contactos.", Toast.LENGTH_LONG).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Observar el estado de la importacion
        viewModel.estadoImportacion.observe(this) { estado ->
            when (estado) {
                EstadoImportacion.CARGANDO -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                EstadoImportacion.EXITO -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Contactos importados.", Toast.LENGTH_SHORT).show()
                    viewModel.resetearEstadoImportacion()
                }
                EstadoImportacion.ERROR -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error al importar.", Toast.LENGTH_SHORT).show()
                    viewModel.resetearEstadoImportacion()
                }
                else -> { // VACIO
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    /**
     * Configura el RecyclerView, su adaptador y el gesto de deslizar para eliminar.
     */
    private fun setupRecyclerView() {
        /*adapter = ContactosAdapter { contacto ->
            // Click en un item: abre la actividad para editar el contacto seleccionado
            val intent = Intent(this, AgregarContactoActivity::class.java).apply {
                putExtra(AgregarContactoActivity.EXTRA_CONTACTO_ID, contacto.id)
            }
            startActivity(intent)
        }*/
        adapter = ContactosAdapter(this)
        binding.recyclerViewContactos.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewContactos.adapter = adapter

        // Configura el helper para el gesto de "swipe-to-delete"
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false // No se usa para mover items
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val contactoEliminado = adapter.currentList[position]
                viewModel.eliminarContacto(contactoEliminado)

                // Muestra un Snackbar con opción de deshacer
                Snackbar.make(binding.root, "Contacto eliminado", Snackbar.LENGTH_LONG).show()
                /*.setAction("DESHACER") {
                    // Si el usuario deshace, volvemos a insertar el contacto.
                    // Esta funcionalidad requeriría un método en el ViewModel.
                }*/
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerViewContactos)
    }

    // Implementación de los métodos de la interfaz
    override fun onCallClick(telefono: String) {
        numeroParaLlamar = telefono
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso ya concedido.
                realizarLlamada(telefono)
            }
            else -> {
                // Solicitar permiso.
                requestCallPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        }
    }

    override fun onMessageClick(telefono: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$telefono")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "No se encontró app para enviar SMS.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onItemClick(contacto: Contacto) {
        // La acción de clic en toda la fila sigue funcionando para editar
        val intent = Intent(this, AgregarContactoActivity::class.java)
        intent.putExtra("EXTRA_CONTACTO_ID", contacto.id)
        startActivity(intent)
    }

    override fun onLinkedinClick(profile: String) {
        val url = "https://www.linkedin.com/in/$profile"
        openUrl(url)
    }

    override fun onWebsiteClick(url: String) {
        // Asegurarse de que la URL tenga un esquema
        val properUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "http://$url"
        } else {
            url
        }
        openUrl(properUrl)
    }

    //Función auxiliar para realizar la llamada ---
    private fun realizarLlamada(telefono: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$telefono"))
            startActivity(intent)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Error de seguridad al intentar llamar.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Configura los listeners para la búsqueda y el botón de agregar.
     */
    private fun setupListeners() {
        binding.editTextBusqueda.addTextChangedListener { text ->
            viewModel.buscarContacto(text.toString())
        }

        binding.fabAgregar.setOnClickListener {
            startActivity(Intent(this, AgregarContactoActivity::class.java))
        }
    }

    /**
     * Observa los LiveData del ViewModel para actualizar la UI.
     */
    private fun observeViewModel() {
        viewModel.contactos.observe(this) { contactos ->
            // Muestra un estado de carga o vacío si es necesario
            // binding.progressBar.visibility = View.GONE
            // binding.emptyView.visibility = if (contactos.isEmpty()) View.VISIBLE else View.GONE
            adapter.submitList(contactos)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_backup -> {
                crearBackupLauncher.launch("contactos_backup.json")
                true
            }
            R.id.action_restore -> {
                restaurarBackupLauncher.launch("application/json")
                true
            }
            R.id.action_export_vcard -> {
                exportarContactosAVCard()
                true
            }
            R.id.action_import_device -> {
                iniciarImportacionDeContactos()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exportarContactosAVCard() {
        val nombreArchivo = "contactos_backup.vcf"
        vcardExportLauncher.launch(nombreArchivo)
    }

    /**Función que gestiona la solicitud de permiso e inicia la importación */
    private fun iniciarImportacionDeContactos() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // El permiso ya está concedido.
                viewModel.importarContactosDelDispositivo(contentResolver)
            }
            else -> {
                // El permiso no está concedido, lo solicitamos.
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    /**
     * Función de utilidad para abrir una URL en el navegador.
     */
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se puede abrir el enlace.", Toast.LENGTH_SHORT).show()
        }
    }
}