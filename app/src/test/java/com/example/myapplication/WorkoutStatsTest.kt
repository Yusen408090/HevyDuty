package com.example.myapplication

import com.example.myapplication.domain.model.*
import com.example.myapplication.domain.usecase.WorkoutStats
import com.example.myapplication.domain.usecase.searchKey
import org.junit.Assert.*
import org.junit.Test

class WorkoutStatsTest {
    private fun workout(id: Long, date: Long, finished: Boolean, sets: List<TrainingSet>, exerciseId: Long = 1) =
        Workout(id, "Prueba", date, if (finished) date + 60_000 else null, "", null, null,
            listOf(TrainingExercise(id, exerciseId, "Press", MuscleGroup.CHEST, 0, sets)))

    @Test fun volumeCountsOnlyCompletedSets() {
        val result = workout(1, 1000, true, listOf(
            TrainingSet(1, 0, 60.0, 10, true),
            TrainingSet(2, 1, 70.0, 8, true),
            TrainingSet(3, 2, 500.0, 100, false)
        ))
        assertEquals(1160.0, result.volume, 0.001)
        assertEquals(2, result.completedSets.size)
    }

    @Test fun progressExcludesActiveSessionsAndSortsByDate() {
        val sets = listOf(TrainingSet(1, 0, 20.0, 10, true))
        val points = WorkoutStats.progress(listOf(
            workout(2, 2000, true, sets), workout(3, 3000, false, sets), workout(1, 1000, true, sets)
        ), null)
        assertEquals(listOf(1L, 2L), points.map { it.workoutId })
    }

    @Test fun maximumIsPerExerciseAndIgnoresUncompletedSets() {
        val history = listOf(
            workout(1, 1000, true, listOf(TrainingSet(1, 0, 60.0, 10, true), TrainingSet(2, 1, 80.0, 8, false))),
            workout(2, 2000, true, listOf(TrainingSet(3, 0, 100.0, 10, true)), exerciseId = 2)
        )
        val points = WorkoutStats.progress(history, 1)
        assertEquals(1, points.size)
        assertEquals(60.0, points.single().maxWeight, 0.001)
        assertEquals(600.0, points.single().volume, 0.001)
    }

    @Test fun bodyweightSetsKeepZeroExternalVolume() {
        val points = WorkoutStats.progress(listOf(workout(1, 1000, true, listOf(TrainingSet(1, 0, 0.0, 20, true)))), null)
        assertEquals(0.0, points.single().volume, 0.0)
    }

    @Test fun noCompletedSetsMeansNoDataPoint() {
        assertTrue(WorkoutStats.progress(listOf(workout(1, 1000, true, listOf(TrainingSet(1, 0, 80.0, 10, false)))), null).isEmpty())
    }

    @Test fun validationRejectsNonFiniteNegativeAndInvalidReps() {
        assertFalse(WorkoutStats.validSet(Double.NaN, 10))
        assertFalse(WorkoutStats.validSet(Double.POSITIVE_INFINITY, 10))
        assertFalse(WorkoutStats.validSet(-1.0, 10))
        assertFalse(WorkoutStats.validSet(20.0, 0))
        assertFalse(WorkoutStats.validSet(20.0, 1001))
        assertTrue(WorkoutStats.validSet(0.0, 1))
        assertTrue(WorkoutStats.validSet(2000.0, 1000))
    }

    @Test(expected = IllegalArgumentException::class)
    fun routineMustHaveExercise() { WorkoutStats.validateRoutine("Rutina", emptyList()) }

    @Test(expected = IllegalArgumentException::class)
    fun routineCannotDuplicateExercise() { WorkoutStats.validateRoutine("Rutina", listOf(RoutineEntry(1), RoutineEntry(1))) }

    @Test(expected = IllegalArgumentException::class)
    fun routineMustHaveName() { WorkoutStats.validateRoutine("  ", listOf(RoutineEntry(1))) }

    @Test(expected = IllegalArgumentException::class)
    fun routineRejectsZeroSets() { WorkoutStats.validateRoutine("Rutina", listOf(RoutineEntry(1, sets = 0))) }

    @Test fun searchIgnoresAccentsCaseAndWhitespace() {
        assertEquals("extension de triceps", "  EXTENSIÓN de Tríceps ".searchKey())
    }

    @Test fun durationNeverNegative() {
        val session = Workout(1, "Test", 2000, 1000, "", null, null, emptyList())
        assertEquals(0L, session.durationMinutes)
    }
}
