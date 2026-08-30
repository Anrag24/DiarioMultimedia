package com.example.diariomultimedia.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.diariomultimedia.R
import com.example.diariomultimedia.data.Entrada
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EntradaAdapter(private val lista: MutableList<Entrada> = mutableListOf()) : BaseAdapter() {

    fun actualizar(nuevaLista: List<Entrada>) {
        lista.clear()
        lista.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = lista.size
    override fun getItem(pos: Int): Entrada = lista[pos]
    override fun getItemId(pos: Int): Long = lista[pos].id

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.item_entrada, parent, false)

        val entrada = lista[pos]
        val sdf     = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())

        view.findViewById<TextView>(R.id.tvTituloItem).text = entrada.titulo
        view.findViewById<TextView>(R.id.tvFechaItem).text  = sdf.format(Date(entrada.fecha))

        // Iconos de contenido multimedia
        view.findViewById<ImageView>(R.id.icAudio).visibility =
            if (entrada.rutaAudio != null) View.VISIBLE else View.GONE
        view.findViewById<ImageView>(R.id.icFoto).visibility  =
            if (entrada.rutaFoto  != null) View.VISIBLE else View.GONE
        view.findViewById<ImageView>(R.id.icVideo).visibility =
            if (entrada.rutaVideo != null) View.VISIBLE else View.GONE

        return view
    }
}
