package com.app.contactos

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.app.contactos.database.ContactosDatabase
import com.app.contactos.databinding.ActivityAgregarContactoBinding
import com.app.contactos.model.Categoria
import com.app.contactos.model.Contacto
import com.app.contactos.model.Grupo
import com.app.contactos.repository.ContactosRepository
import com.app.contactos.utils.ValidationUtils
import com.app.contactos.viewmodel.AgregarContactoViewModel
import com.app.contactos.viewmodel.AgregarContactoViewModelFactory

/**
      Actividad para crear o editar un contacto.
 */
class AgregarContactoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAgregarContactoBinding
    private var contactoId: Int? = null
    private var contactoExistente: Contacto? = null

    private var listaCategorias = listOf<Categoria>()
    private var categoriaSeleccionadaId: Int = -1

    // Listas para manejar la selección de grupos
    private var listaTodosLosGrupos = listOf<Grupo>()
    private var idsGruposSeleccionados = mutableSetOf<Int>()

    private val viewModel: AgregarContactoViewModel by viewModels {
        val database = ContactosDatabase.getDatabase(
            context = applicationContext,
            coroutineScope = lifecycleScope
        )
        val repository = ContactosRepository(
            database.contactoDao(),
            database.categoriaDao(),
            database.grupoDao()
        )
        AgregarContactoViewModelFactory(repository)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgregarContactoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactoId = intent.getIntExtra(EXTRA_CONTACTO_ID, -1).takeIf { it != -1 }

        setupViews()
        setupListeners()
        observeViewModel()
        setupSpinner()
        observarCategorias()
        setupGroupSelection()
    }

    /**
     * Configura las vistas, cargando datos si se está editando un contacto.
     */
    @SuppressLint("SetTextI18n")
    private fun setupViews() {
        if (contactoId != null) {
            title = "✎ Editar Contacto"
            binding.textViewTitulo.text = title
            binding.buttonGuardar.text = "Actualizar contacto"

            //Observamos el contacto
            viewModel.getContactoById(contactoId!!).observe(this) { contacto ->
                contactoExistente = contacto //Actualizamos var externa

                if (contacto != null) {
                    binding.editTextNombre.setText(contacto.nombre)
                    binding.editTextTelefono.setText(contacto.telefono)
                    binding.editTextEmail.setText(contacto.email)
                    binding.etLinkedin.setText(contacto.linkedin)
                    binding.etWebsite.setText(contacto.website)
                } else {
                    Toast.makeText(this, " ⃠ Contacto no encontrado", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        } else {
            title = "Agregar Contacto"
            binding.textViewTitulo.text = title
        }
    }

    /**
     * Configura el listener del botón de guardar.
     */
    private fun setupListeners() {
        binding.buttonGuardar.setOnClickListener {
            guardarContacto()
        }
    }

    /**
     * Observa los LiveData del ViewModel.
     */
    private fun observeViewModel() {
        viewModel.estadoGuardado.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Contacto guardado", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure {
                Toast.makeText(this, " ⃠ Error al guardar: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Valida los campos y guarda el contacto */
    private fun guardarContacto() {
        val nombre = binding.editTextNombre.text.toString().trim()
        val telefono = binding.editTextTelefono.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val categoriaId = categoriaSeleccionadaId
        val linkedin = binding.etLinkedin.text.toString().trim()
        val website = binding.etWebsite.text.toString().trim()

        // Limpia errores previos
        binding.textFieldNombre.error = null
        binding.textFieldTelefono.error = null
        binding.textFieldEmail.error = null

        // Realiza la validación
        if (!ValidationUtils.isNombreValido(nombre)) {
            binding.textFieldNombre.error = "El nombre es obligatorio"
            return
        }
        if (!ValidationUtils.isTelefonoValido(telefono)) {
            binding.textFieldTelefono.error = "El teléfono es obligatorio"
            return
        }
        if (!ValidationUtils.isEmailValido(email)) {
            binding.textFieldEmail.error = "Email no válido"
            return
        }

        // Crea el objeto Contacto y lo guarda
        val contacto = Contacto(
            id = contactoId ?: 0, // Si es nuevo, el id es 0 y Room lo autogenera
            nombre = nombre,
            telefono = telefono,
            email = email,
            categoriaId = categoriaId,
            linkedin = linkedin,
            website = website
        )

        /*if (contactoId == null) {
            viewModel.insertarContacto(contacto)
        } else {
            viewModel.actualizarContacto(contacto)
        }*/
        viewModel.guardarContactoYAsociarGrupos(contacto, idsGruposSeleccionados.toList())
    }

    /**
     * Obtiene las categorías y las muestra en el Spinner.
     */
    private fun observarCategorias() {
        viewModel.todasLasCategorias.observe(this) { categorias ->
            categorias?.let {
                listaCategorias = it
                val nombresCategorias = it.map { cat -> cat.nombre }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresCategorias)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerCategoria.adapter = adapter

                // Si estamos editando, pre-seleccionar la categoria correcta
                contactoExistente?.let { contacto ->
                    val categoriaDelContacto =
                        listaCategorias.find { c -> c.id == contacto.categoriaId }
                    val posicion = listaCategorias.indexOf(categoriaDelContacto)
                    if (posicion != -1) {
                        binding.spinnerCategoria.setSelection(posicion)
                    }
                }
            }
        }
    }

    /**
     * Configura el listener del Spinner para obtener la categoría seleccionada.
     */
    private fun setupSpinner() {
        binding.spinnerCategoria.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (listaCategorias.isNotEmpty()) {
                        categoriaSeleccionadaId = listaCategorias[position].id
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    /** Configura la selección de grupos. */
    private fun setupGroupSelection() {
        // Observar la lista de todos los grupos disponibles
        viewModel.todosLosGrupos.observe(this) { grupos ->
            listaTodosLosGrupos = grupos
        }

        // Observar los grupos a los que ya pertenece el contacto (si se está editando)
        viewModel.gruposDelContacto.observe(this) { contactoConGrupos ->
            idsGruposSeleccionados = contactoConGrupos.grupos.map { it.id }.toMutableSet()
            actualizarTextoGruposSeleccionados()
        }

        binding.btnSeleccionarGrupos.setOnClickListener {
            mostrarDialogoSeleccionGrupos()
        }
    }

    /**
     * Muestra un diálogo para seleccionar grupos.
     */
    private fun mostrarDialogoSeleccionGrupos() {
        val nombresGrupos = listaTodosLosGrupos.map { it.nombre }.toTypedArray()
        val checkedItems = listaTodosLosGrupos.map { idsGruposSeleccionados.contains(it.id) }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle("Seleccionar Grupos")
            .setMultiChoiceItems(nombresGrupos, checkedItems) { _, which, isChecked ->
                val id = listaTodosLosGrupos[which].id
                if (isChecked) {
                    idsGruposSeleccionados.add(id)
                } else {
                    idsGruposSeleccionados.remove(id)
                }
            }
            .setPositiveButton("Aceptar") { dialog, _ ->
                actualizarTextoGruposSeleccionados()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Crear Grupo") { _, _ ->
                // No cerramos el diálogo actual, sino que abrimos otro encima.
                mostrarDialogoCrearGrupo()
            }
            .show()
    }

    private fun mostrarDialogoCrearGrupo() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Crear Nuevo Grupo")

        // Crear un EditText para la entrada del usuario
        val input = EditText(this)
        input.hint = "Nombre del grupo"
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val nombreGrupo = input.text.toString().trim()
            if (nombreGrupo.isNotEmpty()) {
                viewModel.crearNuevoGrupo(nombreGrupo)
                Toast.makeText(this, "Grupo '$nombreGrupo' creado.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                // Idealmente, aquí se debería reabrir el diálogo de selección para
                // que el nuevo grupo aparezca inmediatamente. Por simplicidad,
                // el usuario necesitará tocar "Seleccionar Grupos" de nuevo.
            } else {
                Toast.makeText(this, "El nombre no puede estar vacío.", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    /** Actualiza el texto de los grupos seleccionados. */
    private fun actualizarTextoGruposSeleccionados() {
        val nombresSeleccionados = listaTodosLosGrupos
            .filter { idsGruposSeleccionados.contains(it.id) }
            .joinToString(separator = ", ") { it.nombre }

        binding.tvGruposSeleccionados.text = if (nombresSeleccionados.isEmpty()) "Ninguno" else nombresSeleccionados
    }

    /**
     * Obtiene el ID del contacto de la actividad anterior.
     */
    companion object {
        const val EXTRA_CONTACTO_ID = "EXTRA_CONTACTO_ID"
    }
}