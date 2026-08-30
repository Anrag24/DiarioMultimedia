# Documentación Técnica: Diario de Multimedia

## 1. Arquitectura General del Sistema

La aplicación "Diario de Multimedia" está construida sobre la arquitectura **MVVM (Model-View-ViewModel)**, recomendada por Google para el desarrollo moderno en Android. Esta arquitectura separa claramente las responsabilidades, facilitando el mantenimiento, la escalabilidad y las pruebas del software.

### Patrón MVVM (Model-View-ViewModel)
* **Model (Capa de Datos):** Responsable de la lógica de negocio y la gestión de datos. Incluye la base de datos local (típicamente implementada con Room) y el sistema de archivos del dispositivo, donde se persisten de manera segura los recursos multimedia (audio, foto, video).
* **View (Capa de Presentación):** Compuesta por las Actividades y Fragmentos (`MainActivity`, `NuevaEntradaActivity`, `DetalleActivity`). Su única responsabilidad es renderizar el estado actual de la interfaz y capturar las interacciones del usuario. No contiene lógica de negocio compleja.
* **ViewModel:** Actúa como intermediario entre la Vista y el Modelo. Expone los datos a la Vista mediante componentes observables (como `LiveData` o `StateFlow`) y sobrevive a los cambios de configuración (como rotaciones de pantalla), evitando la recarga innecesaria de datos.

### Flujo de Datos
1. La **Vista** emite una acción del usuario al **ViewModel**.
2. El **ViewModel** solicita operaciones al **Modelo** (ej., insertar una nueva entrada, recuperar una imagen).
3. El **Modelo** interactúa con la base de datos o el sistema de archivos y retorna la respuesta.
4. El **ViewModel** actualiza los flujos de datos observables.
5. La **Vista**, que está suscrita a estos flujos, se actualiza automáticamente con el nuevo estado, garantizando una interfaz reactiva.

## 2. Componentes de la Interfaz de Usuario e Interacciones

El flujo principal de la aplicación se distribuye en tres componentes principales:

### `MainActivity`
* **Propósito:** Actúa como el punto de entrada principal, presentando un listado cronológico de las entradas registradas (usando `RecyclerView`).
* **Interacciones Clave:** 
    * Toque simple: Navega hacia `DetalleActivity` pasando el ID de la entrada.
    * Pulsación larga (`onLongClick`): Dispara un `AlertDialog` para confirmar la eliminación de un registro específico, ejecutando la limpieza tanto en base de datos como en el almacenamiento físico.

### `NuevaEntradaActivity`
* **Propósito:** Proporciona los formularios y controles para la creación de un nuevo registro multimedia.
* **Interacciones Clave:**
    * Invocación de la cámara o galería para la captura/selección de imágenes y videos.
    * Interfaz de grabación de voz.
    * Lógica crítica de importación: Al seleccionar un recurso externo, se lee el flujo de datos (`InputStream`) de la URI proporcionada y se escribe en el almacenamiento privado de la aplicación (`context.filesDir`), asegurando la persistencia a largo plazo.

### `DetalleActivity`
* **Propósito:** Mostrar la información completa de una entrada seleccionada y reproducir los medios asociados.
* **Interacciones Clave:**
    * Renderizado de la imagen o miniatura del video utilizando la ruta absoluta guardada en el almacenamiento privado.
    * Controles de reproducción de audio acoplados a un `MediaPlayer` y sincronizados dinámicamente con un `SeekBar` utilizando un `Handler` para las actualizaciones de tiempo real.

## 3. Detalle de Implementación de Funcionalidades

### RF-01: Importación de Fotos desde la Galería

El uso de `PickVisualMedia` permite delegar la selección de archivos al sistema operativo, manteniendo la privacidad. Es **crítico no almacenar la URI original** devuelta por el selector. Las URIs externas pueden volverse inválidas si el usuario mueve, elimina o revoca el acceso al archivo original. La aplicación debe realizar una copia física hacia su almacenamiento privado.

```kotlin
// Registro del contrato para seleccionar una imagen
val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val localPath = copyFileToInternalStorage(uri, fileName)
        if (localPath != null) {
            // Guardar ruta local en persistencia
            viewModel.setPhotoPath(localPath)
            renderImage(localPath)
        }
    }
}

// Copia del archivo al almacenamiento privado
private fun copyFileToInternalStorage(uri: Uri, fileName: String): String? {
    return try {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val outputFile = File(filesDir, fileName)
        val outputStream = FileOutputStream(outputFile)
        
        inputStream.copyTo(outputStream)
        
        inputStream.close()
        outputStream.close()
        
        outputFile.absolutePath
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

// Invocacion del selector
private fun openGallery() {
    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
}
```

### RF-02: Reproductor de Audio con Barra de Progreso (`SeekBar`)

La sincronización del `SeekBar` y el formato de tiempo requieren un manejo concurrente y control estricto del ciclo de vida para evitar cuelgues o fugas de memoria.

```kotlin
private var mediaPlayer: MediaPlayer? = null
private val handler = Handler(Looper.getMainLooper())
private lateinit var runnable: Runnable

// Inicializacion del reproductor y listener de barra de progreso
private fun setupAudioPlayer(audioPath: String) {
    mediaPlayer = MediaPlayer().apply {
        setDataSource(audioPath)
        prepare()
        
        seekBar.max = duration
        updateTimeLabels(0, duration)
    }
    
    seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) mediaPlayer?.seekTo(progress)
            updateTimeLabels(progress, mediaPlayer?.duration ?: 0)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    })
}

// Ciclo asincrono de actualizacion visual cada 500ms
private fun startAudioPlayback() {
    mediaPlayer?.start()
    runnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                seekBar.progress = player.currentPosition
                updateTimeLabels(player.currentPosition, player.duration)
            }
            handler.postDelayed(this, 500)
        }
    }
    handler.postDelayed(runnable, 0)
}

// Interrupcion del audio y cancelacion de tareas pendientes
private fun stopAudioPlayback() {
    mediaPlayer?.pause()
    handler.removeCallbacksAndMessages(null)
}

// Formateo estricto mm:ss / mm:ss
private fun updateTimeLabels(current: Int, total: Int) {
    val currentFormatted = String.format("%02d:%02d", 
        TimeUnit.MILLISECONDS.toMinutes(current.toLong()),
        TimeUnit.MILLISECONDS.toSeconds(current.toLong()) % 60
    )
    val totalFormatted = String.format("%02d:%02d", 
        TimeUnit.MILLISECONDS.toMinutes(total.toLong()),
        TimeUnit.MILLISECONDS.toSeconds(total.toLong()) % 60
    )
    timeTextView.text = "$currentFormatted / $totalFormatted"
}

// Control del ciclo de vida en destruccion
override fun onDestroy() {
    super.onDestroy()
    stopAudioPlayback()
    mediaPlayer?.release()
    mediaPlayer = null
}
```

### RF-03: Eliminación de Entradas con Limpieza de Disco (Manejo Defensivo)

La eliminación en cascada es fundamental para no agotar el almacenamiento del dispositivo con archivos huérfanos. Se utiliza código defensivo para garantizar que el proceso no falle si el archivo fue eliminado por otros medios.

```kotlin
// Interfaz de confirmacion disparada por onLongClick
private fun showDeleteConfirmationDialog(entry: MultimediaEntry) {
    AlertDialog.Builder(this)
        .setTitle("Eliminar Entrada")
        .setMessage("¿Deseas eliminar este registro de forma permanente?")
        .setPositiveButton("Eliminar") { _, _ ->
            deleteEntryAndFiles(entry)
        }
        .setNegativeButton("Cancelar", null)
        .show()
}

// Eliminacion en cascada del registro y sus activos
private fun deleteEntryAndFiles(entry: MultimediaEntry) {
    deleteFileDefensively(entry.photoPath)
    deleteFileDefensively(entry.audioPath)
    deleteFileDefensively(entry.videoPath)
    
    viewModel.deleteEntry(entry)
}

// Manejo defensivo obligatorio de archivos fisicos
private fun deleteFileDefensively(path: String?) {
    if (path.isNullOrEmpty()) return
    
    try {
        val file = File(path)
        if (file.exists()) {
            val deleted = file.delete()
            if (!deleted) {
                Log.w("FileManager", "Archivo no pudo ser eliminado: $path")
            }
        }
    } catch (e: SecurityException) {
        Log.e("FileManager", "Error de seguridad en E/S: ${e.message}")
    } catch (e: Exception) {
        Log.e("FileManager", "Error inesperado en E/S: ${e.message}")
    }
}
```

## 4. Guía de Defensa Oral y Preguntas Técnicas de Análisis

Este banco de preguntas está diseñado para simular la evaluación de un panel técnico respecto a las decisiones de implementación.

**P1: ¿Cuál es el riesgo de no cancelar el ciclo de un `Handler` cuando la actividad se destruye y cómo se previene en su código?**
**R:** Si no se cancela un `Handler` que ejecuta un `Runnable` iterativo, este seguirá intentando acceder a los componentes de la interfaz de usuario incluso después de que la Actividad haya sido destruida. Esto genera fugas de memoria (*memory leaks*) porque el recolector de basura no puede liberar la instancia de la Actividad mientras el subproceso mantenga una referencia a ella. Eventualmente, resultará en un `NullPointerException`. Se previene invocando explícitamente `handler.removeCallbacksAndMessages(null)` en el método `onDestroy()` de la Actividad.

**P2: ¿Por qué la aplicación realiza una copia obligatoria del archivo multimedia al almacenamiento privado (`context.filesDir`) en lugar de almacenar la URI devuelta por la galería?**
**R:** Las URIs externas representan permisos de acceso otorgados temporalmente sobre archivos fuera de nuestro control. Si el usuario revoca permisos, o mueve, elimina o renombra el archivo desde la galería u otro explorador, la URI almacenada en nuestra base de datos quedaría inválida y apuntaría a un recurso inexistente. Al copiar el recurso directamente al almacenamiento privado (`filesDir`), la aplicación asume control total sobre el ciclo de vida del archivo, aislando la data y garantizando que las entradas del diario mantengan su integridad sin importar las acciones externas en el sistema.

**P3: Durante la eliminación de entradas, ¿por qué es imperativo el uso de código defensivo (`try-catch` y validaciones `file.exists()`) para las operaciones de disco (E/S)?**
**R:** El sistema de archivos representa un entorno con variables fuera del control estricto de la capa de ejecución (como bloqueos de hardware o eliminaciones forzadas previas por el usuario). Si se intenta operar sobre un archivo inexistente o con restricciones repentinas de acceso sin validaciones pre-ejecución ni encapsulamiento en bloques `try-catch`, cualquier error no manejado (*unhandled exception*) provocará el cierre forzoso de la aplicación (*crash*). El manejo defensivo permite procesar gracefully estos fallos, continuar el flujo y depurar exitosamente el registro inconsistente en la base de datos sin interrumpir la experiencia del usuario.
