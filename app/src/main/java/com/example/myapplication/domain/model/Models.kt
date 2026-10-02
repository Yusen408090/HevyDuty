package com.example.myapplication.domain.model

enum class MuscleGroup(val label: String) {
    CHEST("Pecho"), BACK("Espalda"), LEGS("Pierna"),
    SHOULDERS("Hombro"), ARMS("Brazos"), CORE("Core")
}

data class Exercise(
    val id: Long, val name: String, val muscle: MuscleGroup,
    val equipment: String, val instructions: String
)
data class RoutineExercise(
    val exercise: Exercise, val position: Int,
    val targetSets: Int = 3, val targetReps: Int = 10
)
data class Routine(val id: Long, val name: String, val notes: String, val exercises: List<RoutineExercise>)
data class RoutineEntry(val exerciseId: Long, val sets: Int = 3, val reps: Int = 10)
data class TrainingSet(
    val id: Long, val position: Int, val weightKg: Double,
    val reps: Int, val completed: Boolean
)
data class TrainingExercise(
    val id: Long, val exerciseId: Long, val name: String,
    val muscle: MuscleGroup, val position: Int, val sets: List<TrainingSet>
)
data class Workout(
    val id: Long, val name: String, val startedAt: Long,
    val finishedAt: Long?, val notes: String,
    val latitude: Double?, val longitude: Double?,
    val exercises: List<TrainingExercise>
) {
    val completedSets get() = exercises.flatMap { it.sets }.filter { it.completed }
    val volume get() = completedSets.sumOf { it.weightKg * it.reps }
    val durationMinutes get() = ((finishedAt ?: System.currentTimeMillis()) - startedAt).coerceAtLeast(0) / 60_000
}
data class BodyMeasurement(val id: Long = 0, val recordedAt: Long, val weightKg: Double, val waistCm: Double?)
data class ProgressPhoto(val id: Long = 0, val recordedAt: Long, val filePath: String)
data class GymLocation(val id: Long = 0, val name: String, val latitude: Double, val longitude: Double)
data class ProgressPoint(val workoutId: Long, val date: Long, val volume: Double, val maxWeight: Double)
