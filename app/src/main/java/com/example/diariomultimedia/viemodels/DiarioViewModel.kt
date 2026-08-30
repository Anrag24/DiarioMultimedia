package com.example.diariomultimedia.viemodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.diariomultimedia.data.Entrada
import com.example.diariomultimedia.repository.EntradaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiarioViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = EntradaRepository(app)

    private val _entradas = MutableStateFlow<List<Entrada>>(emptyList())
    val entradas: StateFlow<List<Entrada>> = _entradas.asStateFlow()

    private val _entradaSeleccionada = MutableStateFlow<Entrada?>(null)
    val entradaSeleccionada: StateFlow<Entrada?> = _entradaSeleccionada.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _entradas.value = repo.getAll()
        }
    }

    fun cargarPorId(id: Long) {
        viewModelScope.launch {
            _entradaSeleccionada.value = repo.getById(id)
        }
    }

    fun guardar(entrada: Entrada) {
        viewModelScope.launch {
            repo.insertar(entrada)
            _entradas.value = repo.getAll()
        }
    }

    fun eliminar(entrada: Entrada) {
        viewModelScope.launch {
            repo.eliminar(entrada)
            _entradas.value = repo.getAll()
        }
    }
}