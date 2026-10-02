package com.example.myapplication.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.domain.model.*
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.*
import com.example.myapplication.viewmodel.CatalogViewModel

@Composable
fun CatalogScreen(model: CatalogViewModel, onDetail: (Long) -> Unit) {
    val exercises by model.filtered.collectAsStateWithLifecycle()
    val query by model.query.collectAsStateWithLifecycle()
    val group by model.group.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("MOVIMIENTO CON INTENCIÓN", "Ejercicios") }
        item {
            OutlinedTextField(value = query, onValueChange = model::search, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar un ejercicio") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true)
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = group == null, onClick = { model.filter(null) }, label = { Text("Todos") })
                MuscleGroup.entries.forEach {
                    FilterChip(selected = group == it.name, onClick = { model.filter(it) }, label = { Text(it.label) })
                }
            }
        }
        item { Text(exercises.size.toString() + " ejercicios · Encuentra tu siguiente movimiento", color = Muted, style = MaterialTheme.typography.bodySmall) }
        if (exercises.isEmpty()) item { EmptyState("Sin resultados", "Prueba otro nombre o grupo muscular.", Icons.Outlined.SearchOff) }
        MuscleGroup.entries.forEach { muscle ->
            val groupExercises = exercises.filter { it.muscle == muscle }
            if (groupExercises.isNotEmpty()) {
                item(key = "header-" + muscle.name) { Text(muscle.label.uppercase(), color = Lime, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
                items(groupExercises, key = { it.id }) { exercise -> ExerciseRow(exercise, onClick = { onDetail(exercise.id) }) }
            }
        }
    }
}

@Composable
fun ExerciseDetailScreen(exercise: Exercise?, onBack: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { ScreenHeader("BIBLIOTECA", "El movimiento", onBack) }
        if (exercise == null) item { EmptyState("Ejercicio no disponible", "Vuelve al catálogo para seleccionar un ejercicio.") }
        else {
            item {
                PanelCard {
                    Icon(Icons.Outlined.FitnessCenter, null, tint = Lime, modifier = Modifier.size(64.dp))
                    Text(exercise.name, style = MaterialTheme.typography.headlineMedium)
                    Text(exercise.muscle.label + " · " + exercise.equipment, color = Lime)
                }
            }
            item { SectionTitle("Cómo realizarlo") }
            item { Text(exercise.instructions, style = MaterialTheme.typography.bodyLarge) }
            item {
                PanelCard {
                    Text("EN TU REGISTRO", color = Lime, style = MaterialTheme.typography.labelSmall)
                    Text("Registra el peso en kg y las repeticiones de cada serie. Usa 0 kg cuando no haya carga externa.", color = Muted)
                    Text("Mantén el mismo criterio de carga entre sesiones para comparar tu progreso.", color = Muted)
                }
            }
        }
    }
}
