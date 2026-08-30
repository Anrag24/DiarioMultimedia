package com.example.diariomultimedia.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.diariomultimedia.R
import com.example.diariomultimedia.data.Entrada
import com.example.diariomultimedia.viemodels.DiarioViewModel
import java.io.File

class NuevaEntradaActivity : AppCompatActivity() {

    private val vm: DiarioViewModel by viewModels()

    // Estado multimedia
    private var recorder:   MediaRecorder? = null
    private var grabando    = false
    private var rutaAudio   = ""
    private var fotoUri: Uri? = null
    private var videoUri: Uri? = null

    // Vistas
    private lateinit var etTitulo:       EditText
    private lateinit var btnAudio:       Button
    private lateinit var btnFoto:        Button
    private lateinit var btnVideo:       Button
    private lateinit var btnGuardar:     Button
    private lateinit var imgPreviewFoto: ImageView
    private lateinit var videoPreview:   VideoView

    // ── Launchers ─────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.S)
    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) iniciarGrabacion()
        else Toast.makeText(this, "Permiso de micrófono denegado", Toast.LENGTH_SHORT).show()
    }

    private val fotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito) {
            imgPreviewFoto.visibility = View.VISIBLE
            Glide.with(this).load(fotoUri).centerCrop().into(imgPreviewFoto)
        }
    }

    private val videoLauncher = registerForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { exito ->
        if (exito) {
            videoPreview.visibility = View.VISIBLE
            videoPreview.setVideoURI(videoUri)
            videoPreview.start()
        }
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nueva_entrada)
        supportActionBar?.title = "Nueva entrada"

        etTitulo       = findViewById(R.id.etTitulo)
        btnAudio       = findViewById(R.id.btnGrabarAudio)
        btnFoto        = findViewById(R.id.btnTomarFoto)
        btnVideo       = findViewById(R.id.btnGrabarVideo)
        btnGuardar     = findViewById(R.id.btnGuardar)
        imgPreviewFoto = findViewById(R.id.imgPreviewFoto)
        videoPreview   = findViewById(R.id.videoPreview)

        btnAudio.setOnClickListener   { onAudioClick() }
        btnFoto.setOnClickListener    { onFotoClick() }
        btnVideo.setOnClickListener   { onVideoClick() }
        btnGuardar.setOnClickListener { onGuardarClick() }

        findViewById<Button>(R.id.btnCancelar).setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onStop() {
        super.onStop()
        detenerGrabacionSiActiva()
    }

    // ── Audio ─────────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.S)
    private fun onAudioClick() {
        if (!grabando) {
            val permiso = Manifest.permission.RECORD_AUDIO
            if (checkSelfPermission(permiso) == PackageManager.PERMISSION_GRANTED) {
                iniciarGrabacion()
            } else {
                permLauncher.launch(permiso)
            }
        } else {
            detenerGrabacionSiActiva()
            Toast.makeText(this, "Audio guardado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun iniciarGrabacion() {
        rutaAudio = "${externalCacheDir?.absolutePath}/audio_${System.currentTimeMillis()}.mp4"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            recorder  = MediaRecorder(this).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(rutaAudio)
                prepare()
                start()
            }
        }
        grabando       = true
        btnAudio.text  = "⏹ Detener grabación"
    }

    private fun detenerGrabacionSiActiva() {
        if (grabando) {
            recorder?.apply { stop(); release() }
            recorder      = null
            grabando       = false
            btnAudio.text  = "🎙 Grabar audio"
        }
    }

    // ── Foto ──────────────────────────────────────────────────────────────

    private fun onFotoClick() {
        fotoUri = crearUri(Environment.DIRECTORY_PICTURES, "foto", "jpg")
        fotoLauncher.launch(fotoUri!!)
    }

    // ── Video ─────────────────────────────────────────────────────────────

    private fun onVideoClick() {
        videoUri = crearUri(Environment.DIRECTORY_MOVIES, "video", "mp4")
        videoLauncher.launch(videoUri!!)
    }

    // ── Guardar ───────────────────────────────────────────────────────────

    private fun onGuardarClick() {
        val titulo = etTitulo.text.toString().trim()
        if (titulo.isEmpty()) {
            etTitulo.error = "El título es obligatorio"
            return
        }
        if (rutaAudio.isEmpty() && fotoUri == null && videoUri == null) {
            Toast.makeText(this, "Agrega al menos audio, foto o video", Toast.LENGTH_SHORT).show()
            return
        }
        vm.guardar(
            Entrada(
                titulo = titulo,
                rutaAudio = rutaAudio.ifEmpty { null },
                rutaFoto = fotoUri?.toString(),
                rutaVideo = videoUri?.toString()
            )
        )
        setResult(RESULT_OK)
        finish()
    }

    // ── Utilidades ────────────────────────────────────────────────────────

    private fun crearUri(directorio: String, prefijo: String, extension: String): Uri {
        val archivo = File(
            getExternalFilesDir(directorio),
            "${prefijo}_${System.currentTimeMillis()}.$extension"
        )
        return FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            archivo
        )
    }
}
