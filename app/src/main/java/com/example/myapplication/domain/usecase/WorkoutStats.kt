package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.*
import java.text.Normalizer
import java.util.Locale

object WorkoutStats {
    fun progress(history: List<Workout>, exerciseId: Long?): List<ProgressPoint> =
        history.filter { it.finishedAt != null }.sortedBy { it.startedAt }.mapNotNull { workout ->
            val sets = workout.exercises.filter { exerciseId == null || it.exerciseId == exerciseId }
                .flatMap { it.sets }.filter { it.completed }
            if (sets.isEmpty()) null else ProgressPoint(
                workout.id, workout.startedAt, sets.sumOf { it.weightKg * it.reps },
                sets.maxOf { it.weightKg }
            )
        }

    fun validSet(weight: Double, reps: Int) =
        weight.isFinite() && weight in 0.0..2000.0 && reps in 1..1000

    fun validateRoutine(name: String, entries: List<RoutineEntry>) {
        require(name.trim().length in 1..80) { "Escribe un nombre de hasta 80 caracteres." }
        require(entries.isNotEmpty()) { "Añade al menos un ejercicio." }
        require(entries.map { it.exerciseId }.distinct().size == entries.size) { "No repitas ejercicios." }
        require(entries.all { it.sets in 1..20 && it.reps in 1..1000 }) { "Revisa las series y repeticiones." }
    }
}

fun String.searchKey(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).trim()
