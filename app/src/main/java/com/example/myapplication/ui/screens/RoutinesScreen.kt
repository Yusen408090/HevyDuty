package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.domain.model.*
import com.example.myapplication.domain.usecase.searchKey
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.*
import com.example.myapplication.viewmodel.RoutineViewModel

@Composable
fun RoutinesScreen(model: RoutineViewModel, onEdit: (Routine?) -> Unit, onStart: (Long) -> Unit) {
    val routines by model.routines.collectAsStateWithLifecycle()
    val active by model.active.collectAsStateWithLifecycle()
    val pending by model.pending.collectAsStateWithLifecycle()
    var delete by remember { mutableStateOf<Routine?>(null) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { ScreenHeader("TU PLAN. TUS REGLAS.", "Entrenar") }
        active?.let { session ->
            item {
                Button(onClick = { onStart(session.id) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Continuar: " + session.name)
                }
            }
        }
        item {
            OutlinedButton(onClick = { onEdit(null) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Crear rutina")
            }
        }
        item { SectionTitle("Mis rutinas") { Text(routines.size.toString(), color = Muted) } }
        if (routines.isEmpty()) item { EmptyState("Diseña tu entrenamiento", "Crea una rutina y elige los ejercicios, series y repeticiones que quieras registrar.") }
        items(routines, key = { it.id }) { routine ->
            PanelCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(routine.name, style = MaterialTheme.typography.titleLarge)
                            Text(routine.exercises.size.toString() + " ejercicios · " + routine.exercises.sumOf { it.targetSets } + " series", color = Lime, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    IconButton(onClick = { onEdit(routine) }) { Icon(Icons.Outlined.Edit, "Editar " + routine.name, tint = Muted) }
                    IconButton(onClick = { delete = routine }) { Icon(Icons.Outlined.DeleteOutline, "Eliminar " + routine.name, tint = Muted) }
                }
                if (routine.notes.isNotBlank()) Text(routine.notes, color = Muted)
                Text(routine.exercises.joinToString(" · ") { it.exercise.name }, style = MaterialTheme.typography.bodySmall, color = Muted)
                Button(onClick = { model.start(routine.id, onStart) }, enabled = pending == 0, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(6.dp))
                    Text(if (active == null) "Iniciar entrenamiento" else "Volver a la sesión activa")
                }
            }
        }
    }
    delete?.let { routine ->
        ConfirmDialog("Eliminar rutina", "Se eliminará “" + routine.name + "”. Tus entrenamientos anteriores se conservarán.",
            onDismiss = { delete = null }, onConfirm = { model.delete(routine.id); delete = null })
    }
}

@Composable
fun RoutineEditorScreen(model: RoutineViewModel, onBack: () -> Unit) {
    val name by model.name.collectAsStateWithLifecycle()
    val notes by model.notes.collectAsStateWithLifecycle()
    val encoded by model.entries.collectAsStateWithLifecycle()
    val all by model.exercises.collectAsStateWithLifecycle()
    val pending by model.pending.collectAsStateWithLifecycle()
    val entries = remember(encoded) { model.decoded() }
    var query by rememberSaveable { mutableStateOf("") }
    var discard by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler { discard = true }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeader("CONSTRUYE TU PLAN", "Tu rutina", { discard = true }) }
        item { OutlinedTextField(name, model::name, label = { Text("Nombre de la rutina") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(notes, model::notes, label = { Text("Notas · opcional") }, modifier = Modifier.fillMaxWidth(), maxLines = 3) }
        item { SectionTitle("Ejercicios seleccionados") }
        if (entries.isEmpty()) item { Text("Selecciona ejercicios de la biblioteca de abajo.", color = Muted) }
        items(entries, key = { "selected-" + it.exerciseId }) { entry ->
            val exercise = all.firstOrNull { it.id == entry.exerciseId }
            PanelCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(exercise?.name ?: "Ejercicio", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { model.move(entry.exerciseId, -1) }, enabled = entries.first() != entry) { Icon(Icons.Outlined.KeyboardArrowUp, "Subir ejercicio") }
                    IconButton(onClick = { model.move(entry.exerciseId, 1) }, enabled = entries.last() != entry) { Icon(Icons.Outlined.KeyboardArrowDown, "Bajar ejercicio") }
                    IconButton(onClick = { model.toggleExercise(entry.exerciseId) }) { Icon(Icons.Outlined.Close, "Quitar ejercicio") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Stepper("Series", entry.sets, { model.changeEntry(entry.exerciseId, entry.sets - 1, entry.reps) }, { model.changeEntry(entry.exerciseId, entry.sets + 1, entry.reps) }, Modifier.weight(1f))
                    Stepper("Reps", entry.reps, { model.changeEntry(entry.exerciseId, entry.sets, entry.reps - 1) }, { model.changeEntry(entry.exerciseId, entry.sets, entry.reps + 1) }, Modifier.weight(1f))
                }
            }
        }
        item {
            Button(onClick = { model.save(onBack) }, enabled = name.isNotBlank() && entries.isNotEmpty() && pending == 0, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Check, null); Spacer(Modifier.width(8.dp)); Text("Guardar rutina")
            }
        }
        item { SectionTitle("Añadir ejercicios") }
        item { OutlinedTextField(query, { query = it }, singleLine = true, placeholder = { Text("Buscar en la biblioteca") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, modifier = Modifier.fillMaxWidth()) }
        items(all.filter { it.name.searchKey().contains(query.searchKey()) }, key = { "library-" + it.id }) {
            ExerciseRow(it, { model.toggleExercise(it.id) }, selected = entries.any { entry -> entry.exerciseId == it.id })
        }
    }
    if (discard) ConfirmDialog("Salir del editor", "Los cambios sin guardar se perderán.", { discard = false }, { discard = false; onBack() }, "Salir")
}

@Composable
private fun Stepper(label: String, value: Int, minus: () -> Unit, plus: () -> Unit, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Muted, style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = minus, enabled = value > 1) { Icon(Icons.Outlined.Remove, "Reducir " + label, modifier = Modifier.size(18.dp)) }
            Text(value.toString(), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = plus) { Icon(Icons.Outlined.Add, "Aumentar " + label, modifier = Modifier.size(18.dp)) }
        }
    }
}
