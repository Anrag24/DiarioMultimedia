package com.example.diariomultimedia.repository

import android.content.Context
import com.example.diariomultimedia.data.DiarioDatabase
import com.example.diariomultimedia.data.Entrada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EntradaRepository(context: Context) {

    private val dao = DiarioDatabase.Companion.getInstance(context).entradaDao()

    suspend fun getAll(): List<Entrada> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun getById(id: Long): Entrada? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun insertar(entrada: Entrada): Long = withContext(Dispatchers.IO) {
        dao.insertar(entrada)
    }

    suspend fun eliminar(entrada: Entrada) = withContext(Dispatchers.IO) {
        dao.eliminar(entrada)
    }
}