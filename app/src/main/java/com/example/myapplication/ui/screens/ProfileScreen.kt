package com.example.myapplication.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.myapplication.domain.model.ProgressPhoto
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.*
import com.example.myapplication.utils.*
import com.example.myapplication.viewmodel.ProfileViewModel
import java.io.File
import java.util.Locale

@Composable
fun ProfileScreen(model: ProfileViewModel, onCamera: () -> Unit) {
    val measurements by model.measurements.collectAsStateWithLifecycle()
    val photos by model.photos.collectAsStateWithLifecycle()
    val locations by model.locations.collectAsStateWithLifecycle()
    val pending by model.pending.collectAsStateWithLifecycle()
    var measure by rememberSaveable { mutableStateOf(false) }
    var weight by rememberSaveable { mutableStateOf("") }
    var waist by rememberSaveable { mutableStateOf("") }
    var gym by rememberSaveable { mutableStateOf(false) }
    var gymName by rememberSaveable { mutableStateOf("") }
    var photo by remember { mutableStateOf<ProgressPhoto?>(null) }
    var removePhoto by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { model.importPhoto(it) }
    }
    val camera = rememberPermissionAction(listOf(Manifest.permission.CAMERA),
        "La cámara permite tomar una foto de progreso y guardarla en tu dispositivo.", onCamera)
    val locate = rememberPermissionAction(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        "La ubicación permite guardar las coordenadas de tu gimnasio.",
        onGranted = { model.saveGym(gymName) { gym = false; gymName = "" } }
    )
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { ScreenHeader("MÁS ALLÁ DE LOS NÚMEROS", "Mi espacio") }
        item {
            PanelCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(shape = RoundedCornerShape(18.dp), color = Lime.copy(alpha = .12f), modifier = Modifier.size(64.dp)) {
                        if (photos.isEmpty()) Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Person, null, tint = Lime, modifier = Modifier.size(36.dp)) }
                        else AsyncImage(File(photos.first().filePath), "Tu última foto de progreso", contentScale = ContentScale.Crop)
                    }
                    Column {
                        Text("Tu mejor versión", style = MaterialTheme.typography.titleLarge)
                        Text("Diario personal · Guardado en este dispositivo", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item { SectionTitle("Mediciones") { TextButton(onClick = { measure = true }) { Text("+ Añadir") } } }
        item {
            PanelCard {
                Text(measurements.firstOrNull()?.weightKg?.display()?.plus(" kg") ?: "Sin mediciones", style = MaterialTheme.typography.headlineMedium)
                Text("Peso corporal", color = Muted)
                TrendChart(measurements.asReversed().map { it.recordedAt to it.weightKg }, "kg")
            }
        }
        items(measurements.take(5), key = { "measurement-" + it.id }) { measurement ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dateLabel(measurement.recordedAt), color = Muted)
                Text(measurement.weightKg.display() + " kg" + (measurement.waistCm?.let { " · " + it.display() + " cm cintura" } ?: ""))
            }
        }
        item { SectionTitle("Fotos de progreso") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = camera, enabled = pending == 0, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.PhotoCamera, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Cámara")
                }
                OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled = pending == 0, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.PhotoLibrary, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Galería")
                }
            }
        }
        if (photos.isEmpty()) item { EmptyState("Un registro visual de tu camino", "Toma una foto o elige una de tu galería. Las fotos se guardan en el almacenamiento privado de la app.", Icons.Outlined.PhotoCamera) }
        else item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(photos, key = { it.id }) { item ->
                    Surface(onClick = { photo = item }, shape = RoundedCornerShape(16.dp), color = Panel) {
                        Column {
                            AsyncImage(File(item.filePath), "Foto del " + dateLabel(item.recordedAt), modifier = Modifier.width(145.dp).height(180.dp), contentScale = ContentScale.Crop)
                            Text(shortDate(item.recordedAt), modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        item { SectionTitle("Mis gimnasios") { TextButton(onClick = { gym = true }) { Text("+ Guardar") } } }
        if (locations.isEmpty()) item { EmptyState("Tu lugar para entrenar", "Guarda la ubicación actual y dale un nombre. También puedes asociar la ubicación a una sesión.", Icons.Outlined.LocationOn) }
        items(locations, key = { "location-" + it.id }) { location ->
            PanelCard {
                Text(location.name, style = MaterialTheme.typography.titleMedium)
                Text(String.format(Locale.US, "%.5f, %.5f", location.latitude, location.longitude), color = Muted)
            }
        }
        item { Text("HEAVY DUTY · Diario de entrenamiento\nTu progreso se guarda localmente, sin necesidad de crear una cuenta.", color = Muted, style = MaterialTheme.typography.bodySmall) }
    }
    if (measure) {
        val validWeight = weight.decimal()?.let { it in 20.0..500.0 } == true
        val validWaist = waist.isBlank() || waist.decimal()?.let { it in 20.0..300.0 } == true
        AlertDialog(onDismissRequest = { if (pending == 0) measure = false }, title = { Text("Nueva medición") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(weight, { weight = it.take(8) }, label = { Text("Peso corporal · kg") }, singleLine = true,
                        isError = weight.isNotEmpty() && !validWeight, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(waist, { waist = it.take(8) }, label = { Text("Cintura · cm (opcional)") }, singleLine = true,
                        isError = !validWaist, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            }, confirmButton = {
                TextButton(onClick = { model.measure(checkNotNull(weight.decimal()), waist.decimal()) { measure = false; weight = ""; waist = "" } },
                    enabled = validWeight && validWaist && pending == 0) { Text("Guardar") }
            }, dismissButton = { TextButton(onClick = { measure = false }, enabled = pending == 0) { Text("Cancelar") } })
    }
    if (gym) AlertDialog(
        onDismissRequest = { if (pending == 0) gym = false }, title = { Text("Guardar gimnasio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Se guardará tu ubicación actual. También funciona con ubicación aproximada.", color = Muted)
                OutlinedTextField(gymName, { gymName = it.take(80) }, label = { Text("Nombre del gimnasio") }, singleLine = true)
                if (pending > 0) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }, confirmButton = { TextButton(onClick = locate, enabled = gymName.isNotBlank() && pending == 0) { Text("Guardar ubicación") } },
        dismissButton = { TextButton(onClick = { gym = false }, enabled = pending == 0) { Text("Cancelar") } })
    photo?.let { current ->
        Dialog(onDismissRequest = { photo = null }) {
            PanelCard {
                AsyncImage(File(current.filePath), "Foto de progreso", modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp), contentScale = ContentScale.Fit)
                Text(dateLabel(current.recordedAt), color = Muted)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { removePhoto = true }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { photo = null }) { Text("Cerrar") }
                }
            }
        }
        if (removePhoto) ConfirmDialog("Eliminar foto", "La foto se eliminará del almacenamiento privado de la app.",
            { removePhoto = false }, { model.deletePhoto(current); removePhoto = false; photo = null })
    }
}
