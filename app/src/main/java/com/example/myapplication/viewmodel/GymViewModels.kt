package com.example.myapplication.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.*
import com.example.myapplication.domain.repository.GymRepository
import com.example.myapplication.domain.usecase.WorkoutStats
import com.example.myapplication.domain.usecase.searchKey
import com.example.myapplication.utils.LocationClient
import com.example.myapplication.utils.PhotoStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

open class ActionViewModel : ViewModel() {
    private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val notices = Channel<String>(Channel.BUFFERED)
    val messages = notices.receiveAsFlow()
    private val _pending = MutableStateFlow(0)
    val pending = _pending.asStateFlow()
    init {
        viewModelScope.launch {
            for (action in queue) {
                try {
                    action()
                } catch (e: TimeoutCancellationException) {
                    notices.send("La ubicación tardó demasiado. Comprueba el GPS y vuelve a intentar.")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    notices.send(e.message ?: "No se pudo completar la operación. Inténtalo de nuevo.")
                } finally {
                    _pending.update { (it - 1).coerceAtLeast(0) }
                }
            }
        }
    }
    protected fun action(block: suspend () -> Unit) {
        _pending.update { it + 1 }
        if (!queue.trySend(block).isSuccess) _pending.update { it - 1 }
    }
    protected suspend fun message(value: String) { notices.send(value) }
    fun notify(value: String) { action { message(value) } }
    protected fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        catch { e -> if (e is CancellationException) throw e else message("No se pudieron cargar los datos.") }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)
}

class AppViewModel(private val repository: GymRepository) : ActionViewModel() {
    private val _ready = MutableStateFlow(false)
    val ready = _ready.asStateFlow()
    private val _initializationError = MutableStateFlow<String?>(null)
    val initializationError = _initializationError.asStateFlow()
    val history = repository.history().state(emptyList())
    val routines = repository.routines().state(emptyList())
    val active = repository.activeWorkout().state(null)
    init { initialize() }
    fun initialize() = action {
        _initializationError.value = null
        try { repository.initialize(); _ready.value = true }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { _initializationError.value = "No se pudo abrir la base de datos. Intenta de nuevo." }
    }
}

class CatalogViewModel(repository: GymRepository, private val savedState: SavedStateHandle) : ActionViewModel() {
    val query = savedState.getStateFlow("query", "")
    val group = savedState.getStateFlow<String?>("group", null)
    val all = repository.exercises().state(emptyList())
    val filtered = combine(all, query, group) { exercises, query, group ->
        exercises.filter { (group == null || it.muscle.name == group) && it.name.searchKey().contains(query.searchKey()) }
    }.state(emptyList())
    fun search(value: String) { savedState["query"] = value }
    fun filter(value: MuscleGroup?) { savedState["group"] = value?.name }
}

class RoutineViewModel(private val repository: GymRepository, private val savedState: SavedStateHandle) : ActionViewModel() {
    val routines = repository.routines().state(emptyList())
    val exercises = repository.exercises().state(emptyList())
    val active = repository.activeWorkout().state(null)
    val name = savedState.getStateFlow("name", "")
    val notes = savedState.getStateFlow("notes", "")
    val entries = savedState.getStateFlow("entries", emptyList<String>())
    fun name(value: String) { savedState["name"] = value.take(80) }
    fun notes(value: String) { savedState["notes"] = value.take(500) }
    fun begin(routine: Routine?) {
        savedState["editingId"] = routine?.id
        name(routine?.name ?: "")
        notes(routine?.notes ?: "")
        savedState["entries"] = routine?.exercises?.map {
            listOf(it.exercise.id, it.targetSets, it.targetReps).joinToString(":")
        } ?: emptyList<String>()
    }
    fun decoded(): List<RoutineEntry> = entries.value.map {
        val fields = it.split(":")
        RoutineEntry(fields[0].toLong(), fields[1].toInt(), fields[2].toInt())
    }
    fun toggleExercise(id: Long) {
        val values = decoded()
        setEntries(if (values.any { it.exerciseId == id }) values.filterNot { it.exerciseId == id } else values + RoutineEntry(id))
    }
    fun changeEntry(id: Long, sets: Int, reps: Int) {
        setEntries(decoded().map { if (it.exerciseId == id) it.copy(sets = sets.coerceIn(1, 20), reps = reps.coerceIn(1, 1000)) else it })
    }
    fun move(id: Long, delta: Int) {
        val list = decoded().toMutableList()
        val index = list.indexOfFirst { it.exerciseId == id }
        if (index >= 0 && index + delta in list.indices) {
            val item = list.removeAt(index); list.add(index + delta, item); setEntries(list)
        }
    }
    private fun setEntries(entries: List<RoutineEntry>) {
        savedState["entries"] = entries.map { listOf(it.exerciseId, it.sets, it.reps).joinToString(":") }
    }
    fun save(onSaved: () -> Unit) {
        val id: Long? = savedState["editingId"]
        val title = name.value
        val description = notes.value
        val items = decoded()
        action { repository.saveRoutine(id, title, description, items); message("Rutina guardada"); onSaved() }
    }
    fun delete(id: Long) = action { repository.deleteRoutine(id); message("Rutina eliminada") }
    fun start(id: Long, onStarted: (Long) -> Unit) = action { onStarted(repository.startWorkout(id)) }
}

class WorkoutViewModel(
    private val repository: GymRepository, private val locationClient: LocationClient,
    private val savedState: SavedStateHandle
) : ActionViewModel() {
    val id: Long = checkNotNull(savedState["workoutId"])
    val workout = repository.workout(id).state(null)
    val exercises = repository.exercises().state(emptyList())
    val history = repository.history().state(emptyList())
    val restUntil = savedState.getStateFlow("restUntil", 0L)
    val notes = savedState.getStateFlow("notes", "")
    fun notes(value: String) { savedState["notes"] = value.take(500) }
    fun rest(seconds: Int) { savedState["restUntil"] = if (seconds == 0) 0L else System.currentTimeMillis() + seconds * 1000L }
    fun updateSet(setId: Long, weight: Double, reps: Int, completed: Boolean, startRest: Boolean = false) = action {
        repository.updateSet(id, setId, weight, reps, completed)
        if (completed && startRest) rest(90)
    }
    fun addSet(exerciseId: Long) = action { repository.addSet(id, exerciseId) }
    fun deleteSet(setId: Long) = action { repository.deleteSet(id, setId) }
    fun addExercise(exerciseId: Long) = action { repository.addExercise(id, exerciseId) }
    fun finish(onFinished: () -> Unit) {
        val value = notes.value
        action { repository.finishWorkout(id, value); rest(0); onFinished() }
    }
    fun discard(onDiscarded: () -> Unit) = action { repository.deleteWorkout(id); onDiscarded() }
    fun saveLocation() = action {
        val (latitude, longitude) = locationClient.current()
        repository.saveWorkoutLocation(id, latitude, longitude)
        message("Ubicación guardada en este entrenamiento")
    }
}

class ProgressViewModel(private val repository: GymRepository, private val savedState: SavedStateHandle) : ActionViewModel() {
    val history = repository.history().state(emptyList())
    val exercises = repository.exercises().state(emptyList())
    val selected = savedState.getStateFlow<Long?>("selectedExercise", null)
    val points = combine(history, selected) { history, id -> WorkoutStats.progress(history, id) }.state(emptyList())
    fun select(id: Long?) { savedState["selectedExercise"] = id }
    fun delete(id: Long) = action { repository.deleteWorkout(id); message("Entrenamiento eliminado") }
}

class ProfileViewModel(
    private val repository: GymRepository, private val photoStore: PhotoStore,
    private val locationClient: LocationClient
) : ActionViewModel() {
    val measurements = repository.measurements().state(emptyList())
    val photos = repository.photos().state(emptyList())
    val locations = repository.locations().state(emptyList())
    fun measure(weight: Double, waist: Double?, onSaved: () -> Unit) = action {
        repository.saveMeasurement(weight, waist); message("Medición guardada"); onSaved()
    }
    fun importPhoto(uri: Uri, onFinished: () -> Unit = {}) = action {
        var path: String? = null
        try {
            path = photoStore.import(uri)
            repository.savePhoto(path)
            message("Foto guardada")
        } catch (e: Exception) {
            path?.let { photoStore.delete(it) }
            throw e
        } finally { onFinished() }
    }
    fun deletePhoto(photo: ProgressPhoto) = action {
        photoStore.delete(photo.filePath)
        repository.deletePhoto(photo.id)
        message("Foto eliminada")
    }
    fun saveGym(name: String, onSaved: () -> Unit) = action {
        val (latitude, longitude) = locationClient.current()
        repository.saveLocation(name, latitude, longitude)
        message("Gimnasio guardado"); onSaved()
    }
}
