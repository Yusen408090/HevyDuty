package com.example.myapplication.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.myapplication.domain.model.Exercise
import com.example.myapplication.ui.theme.*
import com.example.myapplication.utils.display
import com.example.myapplication.utils.shortDate
import com.example.myapplication.viewmodel.ActionViewModel

@Composable
fun ScreenHeader(eyebrow: String, title: String, onBack: (() -> Unit)? = null, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") }
        Column(Modifier.weight(1f)) {
            Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = Lime)
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge)
        }
        action?.invoke()
    }
}

@Composable
fun SectionTitle(title: String, trailing: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun PanelCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .35f), RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content
    )
}

@Composable
fun Metric(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(18.dp)).padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = Lime, modifier = Modifier.size(20.dp))
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

@Composable
fun EmptyState(title: String, description: String, icon: ImageVector = Icons.Outlined.FitnessCenter, action: @Composable (() -> Unit)? = null) {
    PanelCard {
        Icon(icon, null, tint = Lime, modifier = Modifier.size(30.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, color = Muted)
        action?.invoke()
    }
}

@Composable
fun ExerciseRow(exercise: Exercise, onClick: () -> Unit, selected: Boolean? = null) {
    Surface(onClick = onClick, color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(44.dp).background(Lime.copy(alpha = .1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.FitnessCenter, null, tint = Lime, modifier = Modifier.size(23.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                Text(exercise.muscle.label + " · " + exercise.equipment, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            if (selected == null) Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
            else Icon(if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.AddCircleOutline, null, tint = if (selected) Lime else Muted)
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit, confirmLabel: String = "Eliminar") {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
fun ObserveMessages(model: ActionViewModel, snackbar: SnackbarHostState) {
    LaunchedEffect(model, snackbar) { model.messages.collect { snackbar.showSnackbar(it) } }
}

@Composable
fun TrendChart(values: List<Pair<Long, Double>>, unit: String, modifier: Modifier = Modifier) {
    if (values.isEmpty()) {
        Text("Tu gráfica aparecerá al guardar el primer registro.", color = Muted)
        return
    }
    val points = values.takeLast(12)
    val max = (points.maxOf { it.second } * 1.15).coerceAtLeast(1.0)
    val description = points.joinToString("; ") { shortDate(it.first) + ": " + it.second.display() + " " + unit }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text((if (points.size == 1) "Último registro" else "Últimos " + points.size + " registros") + " · " + unit, style = MaterialTheme.typography.bodySmall, color = Muted)
        Canvas(Modifier.fillMaxWidth().height(150.dp).semantics { contentDescription = description }) {
            val inset = 8.dp.toPx()
            val width = size.width - inset * 2
            val height = size.height - inset * 2
            repeat(4) { line ->
                val y = inset + height * line / 3
                drawLine(Muted.copy(alpha = .14f), Offset(inset, y), Offset(size.width - inset, y))
            }
            val offsets = points.mapIndexed { index, point ->
                Offset(if (points.size == 1) size.width / 2 else inset + index.toFloat() / (points.size - 1) * width,
                    inset + height * (1 - point.second / max).toFloat())
            }
            if (offsets.size > 1) {
                val line = Path().apply { moveTo(offsets[0].x, offsets[0].y); offsets.drop(1).forEach { lineTo(it.x, it.y) } }
                val area = Path().apply {
                    addPath(line); lineTo(offsets.last().x, size.height); lineTo(offsets.first().x, size.height); close()
                }
                drawPath(area, Brush.verticalGradient(listOf(Lime.copy(alpha = .22f), Lime.copy(alpha = .01f))))
                drawPath(line, Lime, style = Stroke(width = 2.5.dp.toPx()))
            }
            offsets.forEach { drawCircle(Ink, 5.dp.toPx(), it); drawCircle(Lime, 3.dp.toPx(), it) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(shortDate(points.first().first), style = MaterialTheme.typography.bodySmall, color = Muted)
            Text(shortDate(points.last().first), style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}
