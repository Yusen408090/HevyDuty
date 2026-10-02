package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.*
import kotlinx.coroutines.flow.Flow

interface GymRepository {
    fun exercises(): Flow<List<Exercise>>
    fun routines(): Flow<List<Routine>>
    fun history(): Flow<List<Workout>>
    fun activeWorkout(): Flow<Workout?>
    fun workout(id: Long): Flow<Workout?>
    fun measurements(): Flow<List<BodyMeasurement>>
    fun photos(): Flow<List<ProgressPhoto>>
    fun locations(): Flow<List<GymLocation>>
    suspend fun initialize()
    suspend fun saveRoutine(id: Long?, name: String, notes: String, entries: List<RoutineEntry>): Long
    suspend fun deleteRoutine(id: Long)
    suspend fun startWorkout(routineId: Long): Long
    suspend fun addExercise(workoutId: Long, exerciseId: Long)
    suspend fun addSet(workoutId: Long, trainingExerciseId: Long)
    suspend fun updateSet(workoutId: Long, setId: Long, weight: Double, reps: Int, completed: Boolean)
    suspend fun deleteSet(workoutId: Long, setId: Long)
    suspend fun finishWorkout(id: Long, notes: String)
    suspend fun deleteWorkout(id: Long)
    suspend fun saveWorkoutLocation(id: Long, latitude: Double, longitude: Double)
    suspend fun saveMeasurement(weight: Double, waist: Double?)
    suspend fun savePhoto(path: String)
    suspend fun deletePhoto(id: Long)
    suspend fun saveLocation(name: String, latitude: Double, longitude: Double)
}
