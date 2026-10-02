package com.example.myapplication.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.domain.model.Workout
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.*
import com.example.myapplication.utils.*
import com.example.myapplication.viewmodel.ProgressViewModel
import java.util.Locale

@Composable
fun ProgressScreen(model: ProgressViewModel, onDetail: (Long) -> Unit) {
    val history by model.history.collectAsStateWithLifecycle()
    val exercises by model.exercises.collectAsStateWithLifecycle()
    val selected by model.selected.collectAsStateWithLifecycle()
    val points by model.points.collectAsStateWithLifecycle()
    var choose by remember { mutableStateOf(false) }
    var metric by rememberSaveable { mutableStateOf("volume") }
    val selectedName = exercises.firstOrNull { it.id == selected }?.name ?: "Todos los ejercicios"
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { ScreenHeader("EL ESFUERZO SE VE", "Tu progreso") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric("Sesiones", history.size.toString(), Icons.Outlined.CalendarMonth, Modifier.weight(1f))
                Metric("Volumen · kg", history.sumOf { it.volume }.display(), Icons.AutoMirrored.Outlined.TrendingUp, Modifier.weight(1f))
            }
        }
        item {
            PanelCard {
                Text("Evolución de fuerza", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = { choose = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedName); Icon(Icons.Outlined.ExpandMore, null) }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(metric == "volume", { metric = "volume" }, label = { Text("Volumen") })
                    FilterChip(metric == "weight", { metric = "weight" }, label = { Text("Peso máximo") }, enabled = selected != null)
                }
                if (selected == null) Text("Selecciona un ejercicio para comparar su peso máximo.", color = Muted, style = MaterialTheme.typography.bodySmall)
                val useWeight = metric == "weight" && selected != null
                Text((if (useWeight) points.maxOfOrNull { it.maxWeight } else points.lastOrNull()?.volume)?.display()?.plus(" kg") ?: "—",
                    style = MaterialTheme.typography.headlineLarge, color = Lime)
                Text(if (useWeight) "Mejor carga registrada" else "Volumen de la última sesión", color = Muted, style = MaterialTheme.typography.bodySmall)
                TrendChart(points.map { it.date to if (useWeight) it.maxWeight else it.volume }, "kg")
                Text("Volumen = Σ peso × repeticiones de series completadas. En ejercicios sin carga externa, 0 kg no representa ausencia de esfuerzo.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item { SectionTitle("Historial") }
        if (history.isEmpty()) item { EmptyState("Aún no hay entrenamientos", "Finaliza una sesión para comenzar tu historial.", Icons.Outlined.Insights) }
        items(history, key = { it.id }) { workout ->
            Surface(onClick = { onDetail(workout.id) }, color = Panel, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(dateLabel(workout.startedAt), color = Lime, style = MaterialTheme.typography.labelSmall)
                    Text(workout.name, style = MaterialTheme.typography.titleLarge)
                    Text(workout.volume.display() + " kg · " + workout.completedSets.size + " series · " + workout.durationMinutes + " min", color = Muted)
                    Text("Ver entrenamiento →", color = Lime, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if (choose) AlertDialog(
        onDismissRequest = { choose = false }, title = { Text("Comparar ejercicio") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { TextButton(onClick = { model.select(null); metric = "volume"; choose = false }) { Text("Todos los ejercicios") } }
                items(exercises) { exercise -> ExerciseRow(exercise, { model.select(exercise.id); choose = false }, selected == exercise.id) }
            }
        }, confirmButton = { TextButton(onClick = { choose = false }) { Text("Cerrar") } })
}

@Composable
fun WorkoutDetailScreen(workout: Workout?, onBack: () -> Unit, onDelete: (Long) -> Unit) {
    var delete by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { ScreenHeader("HISTORIAL", "Entrenamiento", onBack) }
        if (workout == null) item { EmptyState("Entrenamiento no disponible", "Vuelve al historial para seleccionar una sesión.") }
        else {
            item {
                PanelCard {
                    Text(dateLabel(workout.startedAt), color = Lime)
                    Text(workout.name, style = MaterialTheme.typography.headlineMedium)
                    Text(workout.volume.display() + " kg · " + workout.completedSets.size + " series · " + workout.durationMinutes + " min", color = Muted)
                    if (workout.notes.isNotBlank()) Text(workout.notes)
                    if (workout.latitude != null && workout.longitude != null) Text("Ubicación: " + String.format(Locale.US, "%.5f, %.5f", workout.latitude, workout.longitude), color = Muted)
                }
            }
            items(workout.exercises, key = { it.id }) { exercise ->
                PanelCard {
                    Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                    val completed = exercise.sets.filter { it.completed }
                    if (completed.isEmpty()) Text("Sin series completadas", color = Muted)
                    completed.forEachIndexed { index, set ->
                        Text("Serie " + (index + 1) + "     " + set.weightKg.display() + " kg × " + set.reps + " repeticiones", color = Muted)
                    }
                }
            }
            item { TextButton(onClick = { delete = true }) { Text("Eliminar entrenamiento", color = MaterialTheme.colorScheme.error) } }
        }
    }
    if (delete && workout != null) ConfirmDialog("Eliminar entrenamiento", "Se eliminarán sus series y se recalcularán las estadísticas.",
        { delete = false }, { delete = false; onDelete(workout.id) })
}
