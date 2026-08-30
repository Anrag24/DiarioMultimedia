package com.example.diariomultimedia.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entradas")
data class Entrada(
    val titulo:    String,
    val rutaAudio: String? = null,
    val rutaFoto:  String? = null,
    val rutaVideo: String? = null,
    val fecha:     Long   = System.currentTimeMillis(),

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0
)