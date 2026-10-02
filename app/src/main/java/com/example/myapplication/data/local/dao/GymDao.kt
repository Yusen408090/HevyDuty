package com.example.myapplication.data.local.dao

import androidx.room.*
import com.example.myapplication.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    @Query("SELECT * FROM exercises ORDER BY name") fun exercises(): Flow<List<ExerciseEntity>>
    @Query("SELECT * FROM exercises WHERE id = :id") suspend fun exercise(id: Long): ExerciseEntity?
    @Query("SELECT COUNT(*) FROM exercises") suspend fun exerciseCount(): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertExercises(items: List<ExerciseEntity>)
    @Transaction @Query("SELECT * FROM routines ORDER BY id") fun routines(): Flow<List<RoutineRelation>>
    @Transaction @Query("SELECT * FROM routines WHERE id = :id") suspend fun routine(id: Long): RoutineRelation?
    @Insert suspend fun insertRoutine(item: RoutineEntity): Long
    @Update suspend fun updateRoutine(item: RoutineEntity)
    @Insert suspend fun insertRoutineEntries(items: List<RoutineExerciseEntity>)
    @Query("DELETE FROM routine_exercises WHERE routineId = :id") suspend fun clearRoutineEntries(id: Long)
    @Query("DELETE FROM routines WHERE id = :id") suspend fun deleteRoutine(id: Long)
    @Transaction @Query("SELECT * FROM workouts WHERE finishedAt IS NOT NULL ORDER BY startedAt DESC")
    fun history(): Flow<List<WorkoutRelation>>
    @Transaction @Query("SELECT * FROM workouts WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun activeWorkout(): Flow<WorkoutRelation?>
    @Transaction @Query("SELECT * FROM workouts WHERE id = :id") fun workout(id: Long): Flow<WorkoutRelation?>
    @Transaction @Query("SELECT * FROM workouts WHERE id = :id") suspend fun workoutSnapshot(id: Long): WorkoutRelation?
    @Query("SELECT id FROM workouts WHERE finishedAt IS NULL LIMIT 1") suspend fun activeId(): Long?
    @Insert suspend fun insertWorkout(item: WorkoutEntity): Long
    @Insert suspend fun insertWorkoutExercise(item: WorkoutExerciseEntity): Long
    @Insert suspend fun insertSet(item: SetEntity): Long
    @Update suspend fun updateSet(item: SetEntity)
    @Query("DELETE FROM training_sets WHERE id = :id") suspend fun deleteSet(id: Long)
    @Query("UPDATE workouts SET finishedAt = :time, notes = :notes WHERE id = :id AND finishedAt IS NULL")
    suspend fun finishWorkout(id: Long, time: Long, notes: String)
    @Query("UPDATE workouts SET latitude = :latitude, longitude = :longitude WHERE id = :id AND finishedAt IS NULL")
    suspend fun saveWorkoutLocation(id: Long, latitude: Double, longitude: Double)
    @Query("DELETE FROM workouts WHERE id = :id") suspend fun deleteWorkout(id: Long)
    @Query("SELECT * FROM measurements ORDER BY recordedAt DESC") fun measurements(): Flow<List<MeasurementEntity>>
    @Insert suspend fun insertMeasurement(item: MeasurementEntity)
    @Query("SELECT * FROM photos ORDER BY recordedAt DESC") fun photos(): Flow<List<PhotoEntity>>
    @Insert suspend fun insertPhoto(item: PhotoEntity)
    @Query("DELETE FROM photos WHERE id = :id") suspend fun deletePhoto(id: Long)
    @Query("SELECT * FROM locations ORDER BY id DESC") fun locations(): Flow<List<LocationEntity>>
    @Insert suspend fun insertLocation(item: LocationEntity)
}
