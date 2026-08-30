package com.example.diariomultimedia.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EntradaDao {

    @Query("SELECT * FROM entradas ORDER BY fecha DESC")
    suspend fun getAll(): List<Entrada>

    @Query("SELECT * FROM entradas WHERE id = :id")
    suspend fun getById(id: Long): Entrada?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertar(entrada: Entrada): Long

    @Delete
    suspend fun eliminar(entrada: Entrada)
}