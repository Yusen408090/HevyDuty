package com.example.myapplication.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.domain.model.*
import com.example.myapplication.domain.usecase.WorkoutStats
import com.example.myapplication.domain.usecase.searchKey
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.*
import com.example.myapplication.utils.*
import com.example.myapplication.viewmodel.WorkoutViewModel
import kotlinx.coroutines.delay

@Composable
fun WorkoutScreen(model: WorkoutViewModel, onBack: () -> Unit, onFinished: () -> Unit) {
    val workout by model.workout.collectAsStateWithLifecycle()
    val exercises by model.exercises.collectAsStateWithLifecycle()
    val history by model.history.collectAsStateWithLifecycle()
    val pending by model.pending.collectAsStateWithLifecycle()
    val restUntil by model.restUntil.collectAsStateWithLifecycle()
    val notes by model.notes.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var discard by remember { mutableStateOf(false) }
    var finish by remember { mutableStateOf(false) }
    var addExercise by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val locate = rememberPermissionAction(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        "La ubicación permite guardar dónde realizaste esta sesión.", model::saveLocation
    )
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    BackHandler { if (pending == 0) onBack() }
    val session = workout
    if (session == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    val rest = ((restUntil - maxOf(now, System.currentTimeMillis()) + 999) / 1000).coerceAtLeast(0)
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { ScreenHeader("SESIÓN EN CURSO", session.name, { if (pending == 0) onBack() }) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric("Tiempo", clockLabel((now - session.startedAt) / 1000), Icons.Outlined.Schedule, Modifier.weight(1f))
                Metric("Volumen · kg", session.volume.display(), Icons.Outlined.FitnessCenter, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Timer, null, tint = Lime)
                Column(Modifier.weight(1f)) {
                    Text(if (rest > 0) "Descanso · " + clockLabel(rest) else "Temporizador de descanso", style = MaterialTheme.typography.titleMedium)
                    Text("90 s al completar una serie", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { model.rest(if (rest > 0) 0 else 90) }) { Text(if (rest > 0) "Omitir" else "Iniciar") }
            }
        }
        item { Text("SERIE                         KG          REPS          HECHO", color = Muted, style = MaterialTheme.typography.labelSmall) }
        items(session.exercises, key = { it.id }) { exercise ->
            val previous = history.firstNotNullOfOrNull { past ->
                past.exercises.firstOrNull { it.exerciseId == exercise.exerciseId && it.sets.any { set -> set.completed } }
            }?.sets?.filter { it.completed }?.maxByOrNull { it.weightKg }
            PanelCard {
                Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                Text(exercise.muscle.label + (previous?.let { " · Anterior: " + it.weightKg.display() + " kg × " + it.reps } ?: " · Primera marca"), color = Muted, style = MaterialTheme.typography.bodySmall)
                exercise.sets.forEachIndexed { index, set ->
                    key(set.id) {
                        SetRow(set, index + 1,
                            onEdit = { weight, reps -> model.updateSet(set.id, weight, reps, false) },
                            onComplete = { weight, reps, completed -> model.updateSet(set.id, weight, reps, completed, startRest = completed) },
                            onDelete = { model.deleteSet(set.id) },
                            onInvalid = { model.notify("Revisa el peso y las repeticiones antes de completar la serie.") })
                    }
                }
                OutlinedButton(onClick = { model.addSet(exercise.id) }, enabled = pending == 0 && exercise.sets.size < 20, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp)); Text("Añadir serie")
                }
            }
        }
        item { OutlinedButton(onClick = { addExercise = true }, modifier = Modifier.fillMaxWidth()) { Text("Añadir ejercicio") } }
        item { OutlinedTextField(notes, model::notes, label = { Text("Notas de la sesión") }, modifier = Modifier.fillMaxWidth(), maxLines = 4) }
        item {
            OutlinedButton(onClick = locate, enabled = pending == 0, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp)); Text(if (session.latitude == null) "Guardar ubicación del gimnasio" else "Actualizar ubicación guardada")
            }
        }
        item {
            Button(onClick = { finish = true }, enabled = session.completedSets.isNotEmpty() && pending == 0, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.CheckCircle, null); Spacer(Modifier.width(8.dp)); Text("Finalizar entrenamiento")
            }
        }
        item {
            Text("Las series se guardan automáticamente. Solo las marcadas como completadas cuentan para tus estadísticas.", color = Muted, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { discard = true }, enabled = pending == 0) { Text("Descartar sesión", color = MaterialTheme.colorScheme.error) }
        }
    }
    if (finish) ConfirmDialog("Guardar entrenamiento",
        session.completedSets.size.toString() + " series · " + session.volume.display() + " kg de volumen. Las series sin completar no se incluirán en tus marcas.",
        { finish = false }, { finish = false; model.finish(onFinished) }, "Guardar")
    if (discard) ConfirmDialog("Descartar entrenamiento", "Se borrarán esta sesión y todas sus series.",
        { discard = false }, { discard = false; model.discard(onBack) }, "Descartar")
    if (addExercise) AlertDialog(
        onDismissRequest = { addExercise = false }, title = { Text("Añadir ejercicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(query, { query = it }, singleLine = true, placeholder = { Text("Buscar") }, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(exercises.filter { e -> session.exercises.none { it.exerciseId == e.id } && e.name.searchKey().contains(query.searchKey()) }) { exercise ->
                        ExerciseRow(exercise, { model.addExercise(exercise.id); addExercise = false })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { addExercise = false }) { Text("Cerrar") } })
}

@Composable
private fun SetRow(
    set: TrainingSet, number: Int, onEdit: (Double, Int) -> Unit,
    onComplete: (Double, Int, Boolean) -> Unit, onDelete: () -> Unit, onInvalid: () -> Unit
) {
    var weight by rememberSaveable(set.id) { mutableStateOf(set.weightKg.display()) }
    var reps by rememberSaveable(set.id) { mutableStateOf(set.reps.toString()) }
    val parsedWeight = weight.decimal()
    val parsedReps = reps.toIntOrNull()
    val valid = parsedWeight != null && parsedReps != null && WorkoutStats.validSet(parsedWeight, parsedReps)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledLabelColor = Muted, disabledBorderColor = Lime.copy(alpha = .25f)
    )
    fun persist(w: String, r: String) {
        val kg = w.decimal(); val count = r.toIntOrNull()
        if (kg != null && count != null && WorkoutStats.validSet(kg, count)) onEdit(kg, count)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(number.toString(), modifier = Modifier.width(22.dp), color = if (set.completed) Lime else Muted)
        OutlinedTextField(
            weight, { if (it.length <= 8) { weight = it; persist(it, reps) } },
            modifier = Modifier.weight(1f), enabled = !set.completed, singleLine = true,
            colors = fieldColors,
            label = { Text("kg") }, isError = parsedWeight == null || parsedWeight !in 0.0..2000.0,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        OutlinedTextField(
            reps, { if (it.length <= 4) { reps = it; persist(weight, it) } },
            modifier = Modifier.weight(1f), enabled = !set.completed, singleLine = true,
            colors = fieldColors,
            label = { Text("reps") }, isError = parsedReps == null || parsedReps !in 1..1000,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Checkbox(checked = set.completed, onCheckedChange = { checked ->
            if (valid) onComplete(checkNotNull(parsedWeight), checkNotNull(parsedReps), checked) else onInvalid()
        })
        if (!set.completed) IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Close, "Eliminar serie " + number, modifier = Modifier.size(17.dp), tint = Muted)
        } else Spacer(Modifier.width(32.dp))
    }
}
