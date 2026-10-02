package com.example.myapplication.data.local.entities

import androidx.room.*

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: Long, val name: String, val muscle: String,
    val equipment: String, val instructions: String
)
@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val notes: String = ""
)
@Entity(
    tableName = "routine_exercises", primaryKeys = ["routineId", "exerciseId"],
    foreignKeys = [
        ForeignKey(entity = RoutineEntity::class, parentColumns = ["id"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.RESTRICT)
    ], indices = [Index("exerciseId")]
)
data class RoutineExerciseEntity(
    val routineId: Long, val exerciseId: Long, val position: Int,
    val targetSets: Int, val targetReps: Int
)
@Entity(tableName = "workouts", indices = [Index("finishedAt")])
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val startedAt: Long, val finishedAt: Long? = null,
    val notes: String = "", val latitude: Double? = null, val longitude: Double? = null
)
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(entity = WorkoutEntity::class, parentColumns = ["id"], childColumns = ["workoutId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.RESTRICT)
    ], indices = [Index("workoutId"), Index("exerciseId")]
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long, val exerciseId: Long, val name: String,
    val muscle: String, val position: Int
)
@Entity(
    tableName = "training_sets",
    foreignKeys = [ForeignKey(entity = WorkoutExerciseEntity::class, parentColumns = ["id"], childColumns = ["workoutExerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("workoutExerciseId")]
)
data class SetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long, val position: Int, val weightKg: Double = 0.0,
    val reps: Int = 10, val completed: Boolean = false
)
@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordedAt: Long, val weightKg: Double, val waistCm: Double?
)
@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordedAt: Long, val filePath: String
)
@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val latitude: Double, val longitude: Double
)

data class RoutineExerciseRelation(
    @Embedded val entry: RoutineExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id") val exercise: ExerciseEntity
)
data class RoutineRelation(
    @Embedded val routine: RoutineEntity,
    @Relation(entity = RoutineExerciseEntity::class, parentColumn = "id", entityColumn = "routineId")
    val entries: List<RoutineExerciseRelation>
)
data class WorkoutExerciseRelation(
    @Embedded val exercise: WorkoutExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId") val sets: List<SetEntity>
)
data class WorkoutRelation(
    @Embedded val workout: WorkoutEntity,
    @Relation(entity = WorkoutExerciseEntity::class, parentColumn = "id", entityColumn = "workoutId")
    val exercises: List<WorkoutExerciseRelation>
)
