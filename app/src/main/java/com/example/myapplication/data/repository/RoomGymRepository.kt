package com.example.myapplication.data.repository

import androidx.room.withTransaction
import com.example.myapplication.data.local.GymDatabase
import com.example.myapplication.data.local.SeedData
import com.example.myapplication.data.local.entities.*
import com.example.myapplication.domain.model.*
import com.example.myapplication.domain.repository.GymRepository
import com.example.myapplication.domain.usecase.WorkoutStats
import kotlinx.coroutines.flow.map

class RoomGymRepository(private val database: GymDatabase) : GymRepository {
    private val dao = database.gymDao()
    override fun exercises() = dao.exercises().map { it.map(ExerciseEntity::toModel) }
    override fun routines() = dao.routines().map { it.map(RoutineRelation::toModel) }
    override fun history() = dao.history().map { it.map(WorkoutRelation::toModel) }
    override fun activeWorkout() = dao.activeWorkout().map { it?.toModel() }
    override fun workout(id: Long) = dao.workout(id).map { it?.toModel() }
    override fun measurements() = dao.measurements().map { rows ->
        rows.map { BodyMeasurement(it.id, it.recordedAt, it.weightKg, it.waistCm) }
    }
    override fun photos() = dao.photos().map { rows -> rows.map { ProgressPhoto(it.id, it.recordedAt, it.filePath) } }
    override fun locations() = dao.locations().map { rows -> rows.map { GymLocation(it.id, it.name, it.latitude, it.longitude) } }

    override suspend fun initialize() = database.withTransaction {
        if (dao.exerciseCount() == 0) {
            dao.insertExercises(SeedData.exercises)
            seedRoutine("Empuje", "Pecho · Hombro · Tríceps", listOf(1L, 2L, 21L, 22L, 28L))
            seedRoutine("Tirón", "Espalda · Bíceps", listOf(8L, 9L, 10L, 26L, 27L))
            seedRoutine("Pierna y core", "Cuádriceps · Femoral · Abdomen", listOf(13L, 15L, 17L, 18L, 32L))
        }
    }

    private suspend fun seedRoutine(name: String, notes: String, exerciseIds: List<Long>) {
        val id = dao.insertRoutine(RoutineEntity(name = name, notes = notes))
        dao.insertRoutineEntries(exerciseIds.mapIndexed { index, exerciseId ->
            RoutineExerciseEntity(id, exerciseId, index, 3, 10)
        })
    }

    override suspend fun saveRoutine(id: Long?, name: String, notes: String, entries: List<RoutineEntry>): Long =
        database.withTransaction {
            WorkoutStats.validateRoutine(name, entries)
            require(notes.length <= 500) { "Las notas admiten hasta 500 caracteres." }
            if (id != null) requireNotNull(dao.routine(id)) { "La rutina ya no existe." }
            val routineId = id ?: dao.insertRoutine(RoutineEntity(name = name.trim(), notes = notes.trim()))
            if (id != null) dao.updateRoutine(RoutineEntity(id, name.trim(), notes.trim()))
            dao.clearRoutineEntries(routineId)
            dao.insertRoutineEntries(entries.mapIndexed { position, entry ->
                requireNotNull(dao.exercise(entry.exerciseId)) { "Ejercicio no disponible." }
                RoutineExerciseEntity(routineId, entry.exerciseId, position, entry.sets, entry.reps)
            })
            routineId
        }

    override suspend fun deleteRoutine(id: Long) = dao.deleteRoutine(id)

    override suspend fun startWorkout(routineId: Long): Long = database.withTransaction {
        // A transaction makes double taps resume the same session.
        dao.activeId()?.let { return@withTransaction it }
        val routine = requireNotNull(dao.routine(routineId)) { "La rutina ya no existe." }
        require(routine.entries.isNotEmpty()) { "Añade ejercicios a la rutina." }
        val id = dao.insertWorkout(WorkoutEntity(name = routine.routine.name, startedAt = System.currentTimeMillis()))
        routine.entries.sortedBy { it.entry.position }.forEach { item ->
            val exerciseId = dao.insertWorkoutExercise(WorkoutExerciseEntity(
                workoutId = id, exerciseId = item.exercise.id, name = item.exercise.name,
                muscle = item.exercise.muscle, position = item.entry.position
            ))
            repeat(item.entry.targetSets) { index ->
                dao.insertSet(SetEntity(workoutExerciseId = exerciseId, position = index, reps = item.entry.targetReps))
            }
        }
        id
    }

    private suspend fun requireActive(id: Long): WorkoutRelation {
        val workout = requireNotNull(dao.workoutSnapshot(id)) { "Entrenamiento no encontrado." }
        require(workout.workout.finishedAt == null) { "El entrenamiento ya está finalizado." }
        return workout
    }

    override suspend fun addExercise(workoutId: Long, exerciseId: Long) = database.withTransaction {
        val workout = requireActive(workoutId)
        require(workout.exercises.none { it.exercise.exerciseId == exerciseId }) { "Este ejercicio ya está en la sesión." }
        val exercise = requireNotNull(dao.exercise(exerciseId))
        val id = dao.insertWorkoutExercise(WorkoutExerciseEntity(
            workoutId = workoutId, exerciseId = exerciseId, name = exercise.name,
            muscle = exercise.muscle, position = workout.exercises.size
        ))
        repeat(3) { dao.insertSet(SetEntity(workoutExerciseId = id, position = it)) }
    }

    override suspend fun addSet(workoutId: Long, trainingExerciseId: Long) = database.withTransaction {
        val entry = requireActive(workoutId).exercises.first { it.exercise.id == trainingExerciseId }
        require(entry.sets.size < 20) { "Máximo 20 series por ejercicio." }
        val previous = entry.sets.maxByOrNull { it.position }
        dao.insertSet(SetEntity(
            workoutExerciseId = trainingExerciseId, position = (previous?.position ?: -1) + 1,
            weightKg = previous?.weightKg ?: 0.0, reps = previous?.reps ?: 10
        ))
        Unit
    }

    override suspend fun updateSet(workoutId: Long, setId: Long, weight: Double, reps: Int, completed: Boolean) =
        database.withTransaction {
            require(WorkoutStats.validSet(weight, reps)) { "Usa 0–2000 kg y 1–1000 repeticiones." }
            val set = requireActive(workoutId).exercises.flatMap { it.sets }.first { it.id == setId }
            dao.updateSet(set.copy(weightKg = weight, reps = reps, completed = completed))
        }

    override suspend fun deleteSet(workoutId: Long, setId: Long) = database.withTransaction {
        val workout = requireActive(workoutId)
        require(workout.exercises.any { exercise -> exercise.sets.any { it.id == setId } })
        dao.deleteSet(setId)
    }

    override suspend fun finishWorkout(id: Long, notes: String) = database.withTransaction {
        val workout = requireActive(id)
        require(workout.exercises.flatMap { it.sets }.any { it.completed }) { "Completa al menos una serie antes de guardar." }
        require(notes.length <= 500)
        dao.finishWorkout(id, System.currentTimeMillis(), notes.trim())
    }

    override suspend fun deleteWorkout(id: Long) = dao.deleteWorkout(id)
    override suspend fun saveWorkoutLocation(id: Long, latitude: Double, longitude: Double) =
        database.withTransaction {
            requireActive(id)
            validateCoordinates(latitude, longitude)
            dao.saveWorkoutLocation(id, latitude, longitude)
        }

    override suspend fun saveMeasurement(weight: Double, waist: Double?) {
        require(weight.isFinite() && weight in 20.0..500.0) { "Introduce un peso entre 20 y 500 kg." }
        require(waist == null || (waist.isFinite() && waist in 20.0..300.0)) { "Revisa la medida de cintura." }
        dao.insertMeasurement(MeasurementEntity(recordedAt = System.currentTimeMillis(), weightKg = weight, waistCm = waist))
    }

    override suspend fun savePhoto(path: String) {
        require(path.isNotBlank())
        dao.insertPhoto(PhotoEntity(recordedAt = System.currentTimeMillis(), filePath = path))
    }
    override suspend fun deletePhoto(id: Long) = dao.deletePhoto(id)
    override suspend fun saveLocation(name: String, latitude: Double, longitude: Double) {
        require(name.trim().length in 1..80) { "Escribe un nombre para el gimnasio." }
        validateCoordinates(latitude, longitude)
        dao.insertLocation(LocationEntity(name = name.trim(), latitude = latitude, longitude = longitude))
    }
    private fun validateCoordinates(latitude: Double, longitude: Double) {
        require(latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0)
    }
}

private fun ExerciseEntity.toModel() = Exercise(id, name, MuscleGroup.valueOf(muscle), equipment, instructions)
private fun RoutineRelation.toModel() = Routine(routine.id, routine.name, routine.notes,
    entries.sortedBy { it.entry.position }.map {
        RoutineExercise(it.exercise.toModel(), it.entry.position, it.entry.targetSets, it.entry.targetReps)
    })
private fun WorkoutRelation.toModel() = Workout(
    workout.id, workout.name, workout.startedAt, workout.finishedAt, workout.notes, workout.latitude, workout.longitude,
    exercises.sortedBy { it.exercise.position }.map { entry ->
        TrainingExercise(entry.exercise.id, entry.exercise.exerciseId, entry.exercise.name,
            MuscleGroup.valueOf(entry.exercise.muscle), entry.exercise.position,
            entry.sets.sortedBy { it.position }.map { TrainingSet(it.id, it.position, it.weightKg, it.reps, it.completed) })
    }
)
