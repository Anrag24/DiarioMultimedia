package com.example.diariomultimedia.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Entrada::class], version = 1, exportSchema = false)
abstract class DiarioDatabase : RoomDatabase() {

    abstract fun entradaDao(): EntradaDao

    companion object {
        @Volatile
        private var INSTANCE: DiarioDatabase? = null

        fun getInstance(context: Context): DiarioDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    DiarioDatabase::class.java,
                    "diario.db"
                ).fallbackToDestructiveMigration(false)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}