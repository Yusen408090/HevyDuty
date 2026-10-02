package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.*
import com.example.myapplication.utils.*
import com.example.myapplication.viewmodel.AppViewModel
import java.time.*
import java.time.temporal.TemporalAdjusters

@Composable
fun HomeScreen(model: AppViewModel, onTrain: () -> Unit, onProgress: () -> Unit, onResume: (Long) -> Unit) {
    val history by model.history.collectAsStateWithLifecycle()
    val active by model.active.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val week = history.filter {
        val date = Instant.ofEpochMilli(it.startedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        !date.isBefore(start) && !date.isAfter(today)
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item { ScreenHeader("HEAVY DUTY / TU DIARIO DE FUERZA", "Cada serie cuenta.") }
        item {
            Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Lime, androidx.compose.ui.graphics.Color(0xFF8BBE41))), RoundedCornerShape(24.dp)).padding(22.dp)) {
                Icon(Icons.Outlined.FitnessCenter, null, tint = Ink.copy(alpha = .12f), modifier = Modifier.align(Alignment.CenterEnd).size(122.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("CONSTRUYE TU MEJOR VERSIÓN", color = Ink, style = MaterialTheme.typography.labelSmall)
                    Text("Tu próximo\nrécord empieza hoy.", style = MaterialTheme.typography.headlineMedium, color = Ink)
                    Text("Una sesión. Un paso más.", color = Ink.copy(alpha = .7f))
                    Button(onClick = onTrain, colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Lime)) {
                        Text("Vamos a entrenar"); Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        active?.let { session ->
            item {
                PanelCard {
                    Text("ENTRENAMIENTO EN CURSO", color = Lime, style = MaterialTheme.typography.labelSmall)
                    Text(session.name, style = MaterialTheme.typography.titleLarge)
                    Text(session.completedSets.size.toString() + " series completadas · " + session.volume.display() + " kg", color = Muted)
                    Button(onClick = { onResume(session.id) }, modifier = Modifier.fillMaxWidth()) { Text("Continuar sesión") }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionTitle("Esta semana") { Text(week.size.toString() + " sesiones", color = Muted, style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Metric("Volumen · kg", week.sumOf { it.volume }.display(), Icons.Outlined.FitnessCenter, Modifier.weight(1f))
                    Metric("Tiempo · min", week.sumOf { it.durationMinutes }.toString(), Icons.Outlined.Schedule, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { index, label ->
                        val day = start.plusDays(index.toLong())
                        val trained = week.any { Instant.ofEpochMilli(it.startedAt).atZone(ZoneId.systemDefault()).toLocalDate() == day }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Box(Modifier.size(36.dp).background(if (trained) Lime else Panel, CircleShape), contentAlignment = Alignment.Center) {
                                if (trained) Icon(Icons.Outlined.Check, "Entrenaste", tint = Ink, modifier = Modifier.size(19.dp))
                                else Text(day.dayOfMonth.toString(), color = if (day == today) Lime else Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        item { SectionTitle("Tu actividad") { TextButton(onClick = onProgress) { Text("Ver todo") } } }
        if (history.isEmpty()) item {
            EmptyState("Tu historia empieza aquí", "Completa tu primer entrenamiento para ver tus marcas, volumen y constancia.")
        } else items(minOf(3, history.size)) { index ->
            val workout = history[index]
            PanelCard {
                Text(dateLabel(workout.startedAt), color = Lime, style = MaterialTheme.typography.labelSmall)
                Text(workout.name, style = MaterialTheme.typography.titleLarge)
                Text(workout.volume.display() + " kg · " + workout.completedSets.size + " series · " + workout.durationMinutes + " min", color = Muted)
            }
        }
        item { Text("PROGRESO REAL. A TU RITMO.", style = MaterialTheme.typography.labelSmall, color = Muted, modifier = Modifier.padding(bottom = 8.dp)) }
    }
}
