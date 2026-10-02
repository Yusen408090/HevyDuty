package com.example.myapplication

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.myapplication.data.local.GymDatabase
import com.example.myapplication.data.repository.RoomGymRepository
import com.example.myapplication.domain.model.RoutineEntry
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class RoomGymRepositoryTest {
    private lateinit var db: GymDatabase
    private lateinit var repository: RoomGymRepository

    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), GymDatabase::class.java).build()
        repository = RoomGymRepository(db)
        repository.initialize()
    }
    @After fun close() { db.close() }

    @Test fun seedingIsIdempotent() = runBlocking {
        repository.initialize()
        assertEquals(36, repository.exercises().first().size)
        assertEquals(3, repository.routines().first().size)
    }

    @Test fun editingAndDeletingTemplateDoesNotChangeHistory() = runBlocking {
        val routine = repository.routines().first().first()
        val id = repository.startWorkout(routine.id)
        val set = repository.workout(id).first()!!.exercises.first().sets.first()
        repository.updateSet(id, set.id, 60.0, 10, true)
        repository.finishWorkout(id, "Sesión original")
        repository.saveRoutine(routine.id, "Otro nombre", "", listOf(RoutineEntry(2, 1, 5)))
        repository.deleteRoutine(routine.id)
        val workout = repository.history().first().single()
        assertEquals(routine.name, workout.name)
        assertEquals(routine.exercises.size, workout.exercises.size)
        assertEquals(600.0, workout.volume, 0.001)
    }

    @Test fun simultaneousStartsReturnOneActiveSession() = runBlocking {
        val routine = repository.routines().first().first()
        val ids = coroutineScope {
            val first = async { repository.startWorkout(routine.id) }
            val second = async { repository.startWorkout(routine.id) }
            listOf(first.await(), second.await())
        }
        assertEquals(ids[0], ids[1])
    }

    @Test fun finishingEmptyWorkoutFailsAndLeavesItActive() = runBlocking {
        val id = repository.startWorkout(repository.routines().first().first().id)
        try { repository.finishWorkout(id, ""); fail("Expected rejection") } catch (_: IllegalArgumentException) { }
        assertEquals(id, repository.activeWorkout().first()!!.id)
        assertTrue(repository.history().first().isEmpty())
    }

    @Test fun invalidRoutineEditRollsBack() = runBlocking {
        val original = repository.routines().first().first()
        try {
            repository.saveRoutine(original.id, "Cambio inválido", "", listOf(RoutineEntry(Long.MAX_VALUE)))
            fail("Expected rollback")
        } catch (_: IllegalArgumentException) { }
        val unchanged = repository.routines().first().first { it.id == original.id }
        assertEquals(original, unchanged)
    }

    @Test fun cannotUpdateFinishedWorkout() = runBlocking {
        val id = repository.startWorkout(repository.routines().first().first().id)
        val set = repository.workout(id).first()!!.exercises.first().sets.first()
        repository.updateSet(id, set.id, 40.0, 8, true)
        repository.finishWorkout(id, "")
        try { repository.updateSet(id, set.id, 100.0, 8, true); fail("Expected rejection") }
        catch (_: IllegalArgumentException) { }
        assertEquals(320.0, repository.history().first().single().volume, 0.001)
    }

    @Test fun dataSurvivesDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "persistence-test.db"
        context.deleteDatabase(name)
        var disk = Room.databaseBuilder(context, GymDatabase::class.java, name).build()
        try {
            var repo = RoomGymRepository(disk)
            repo.initialize()
            val id = repo.startWorkout(repo.routines().first().first().id)
            val set = repo.workout(id).first()!!.exercises.first().sets.first()
            repo.updateSet(id, set.id, 55.0, 9, true)
            disk.close()
            disk = Room.databaseBuilder(context, GymDatabase::class.java, name).build()
            repo = RoomGymRepository(disk)
            val restored = repo.activeWorkout().first()!!
            assertEquals(id, restored.id)
            assertEquals(495.0, restored.volume, 0.001)
        } finally { disk.close(); context.deleteDatabase(name) }
    }

    @Test fun deletingWorkoutCascadesToExercisesAndSets() = runBlocking {
        val id = repository.startWorkout(repository.routines().first().first().id)
        repository.deleteWorkout(id)
        val connection = db.openHelper.readableDatabase
        connection.query("SELECT COUNT(*) FROM training_sets").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
        connection.query("SELECT COUNT(*) FROM workout_exercises").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
    }
}

