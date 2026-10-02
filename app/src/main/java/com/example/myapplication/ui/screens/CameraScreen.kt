package com.example.myapplication.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.Muted
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CameraScreen(onBack: () -> Unit, onCaptured: (File) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }
    val previewView = remember(context) { PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
    val capture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    var front by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var hasPermission by remember { mutableStateOf(false) }
    val currentOnCaptured by rememberUpdatedState(onCaptured)
    val active = remember { AtomicBoolean(true) }
    DisposableEffect(Unit) { onDispose { active.set(false) } }
    BackHandler { if (!busy) onBack() }
    val request = rememberPermissionAction(listOf(Manifest.permission.CAMERA),
        "Permite la cámara para capturar una foto de progreso.") { hasPermission = true }
    DisposableEffect(lifecycleOwner, context) {
        fun refresh() { hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED }
        refresh()
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(hasPermission, front, retry, lifecycleOwner) {
        var disposed = false
        var boundProvider: ProcessCameraProvider? = null
        var boundPreview: Preview? = null
        ready = false
        error = null
        if (hasPermission) {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                if (!disposed) {
                    try {
                        val provider = future.get()
                        val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                        check(provider.hasCamera(selector)) { "Esta cámara no está disponible. Prueba la otra cámara o la galería." }
                        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                        boundPreview = preview
                        boundProvider = provider
                        capture.targetRotation = previewView.display?.rotation ?: android.view.Surface.ROTATION_0
                        provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                        ready = true
                    } catch (e: Exception) { error = e.message ?: "No se pudo abrir la cámara." }
                }
            }, executor)
        }
        onDispose {
            disposed = true
            boundPreview?.let { boundProvider?.unbind(it, capture) }
        }
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader("TU PROGRESO EN IMÁGENES", "Cámara", { if (!busy) onBack() })
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (hasPermission) AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            if (!hasPermission) EmptyState("Acceso a cámara", "Puedes habilitar la cámara o volver y elegir una foto de tu galería.", Icons.Outlined.PhotoCamera) {
                Button(onClick = request) { Text("Permitir cámara") }
            }
            else if (error != null) PanelCard {
                Text(error.orEmpty())
                TextButton(onClick = { retry++ }) { Text("Reintentar") }
            }
            else if (!ready) CircularProgressIndicator()
        }
        Text("La foto se guardará únicamente en esta app.", color = Muted, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { front = !front }, enabled = hasPermission && !busy) { Icon(Icons.Outlined.Cameraswitch, "Cambiar cámara") }
            Button(onClick = {
                busy = true
                error = null
                val directory = File(context.cacheDir, "captures").apply { mkdirs() }
                val file = File(directory, UUID.randomUUID().toString() + ".jpg")
                capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), executor, object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        busy = false
                        if (active.get()) currentOnCaptured(file) else file.delete()
                    }
                    override fun onError(exception: ImageCaptureException) { busy = false; file.delete(); error = "No se pudo guardar la foto. Vuelve a intentar." }
                })
            }, enabled = hasPermission && ready && !busy, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.PhotoCamera, null); Spacer(Modifier.width(8.dp)); Text(if (busy) "Guardando…" else "Tomar foto")
            }
        }
    }
}
