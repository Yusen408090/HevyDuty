package com.example.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myapplication.data.local.dao.GymDao
import com.example.myapplication.data.local.entities.*

@Database(
    entities = [ExerciseEntity::class, RoutineEntity::class, RoutineExerciseEntity::class,
        WorkoutEntity::class, WorkoutExerciseEntity::class, SetEntity::class,
        MeasurementEntity::class, PhotoEntity::class, LocationEntity::class],
    version = 1, exportSchema = true
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun gymDao(): GymDao
}
