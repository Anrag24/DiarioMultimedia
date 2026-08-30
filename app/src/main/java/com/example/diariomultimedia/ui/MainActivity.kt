package com.example.diariomultimedia.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.diariomultimedia.R
import com.example.diariomultimedia.viemodels.DiarioViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val vm: DiarioViewModel by viewModels()
    private val adapter = EntradaAdapter()

    private val nuevaEntradaLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            vm.cargar()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val listView = findViewById<ListView>(R.id.listViewEntradas)
        listView.adapter = adapter

        // Observar la lista de entradas
        lifecycleScope.launch {
            vm.entradas.collect { lista ->
                adapter.actualizar(lista)
            }
        }

        // Abrir detalle al tocar una entrada
        listView.setOnItemClickListener { _, _, position, _ ->
            val entrada = adapter.getItem(position)
            val intent  = Intent(this, DetalleActivity::class.java)
            intent.putExtra(DetalleActivity.EXTRA_ID, entrada.id)
            startActivity(intent)
        }

        // Eliminar entrada con long click
        listView.setOnItemLongClickListener { _, _, position, _ ->
            val entrada = adapter.getItem(position)
            AlertDialog.Builder(this)
                .setTitle("Eliminar entrada")
                .setMessage("¿Deseas eliminar \"${entrada.titulo}\"?")
                .setPositiveButton("Eliminar") { _, _ -> vm.eliminar(entrada) }
                .setNegativeButton("Cancelar", null)
                .show()
            true
        }

        // Boton nueva entrada
        findViewById<Button>(R.id.btnNuevaEntrada).setOnClickListener {
            val intent = Intent(this, NuevaEntradaActivity::class.java)
            nuevaEntradaLauncher.launch(intent)
        }
    }
}
