package io.github.jreyn419.circuittrainer.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Stores workouts as a small JSON file in app-private storage.
 * Deliberately avoids a database dependency to keep the app lightweight.
 */
class WorkoutRepository(context: Context) {

    private val file = File(context.filesDir, "workouts.json")
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    private val _workouts = MutableStateFlow(loadFromDisk())
    val workouts: StateFlow<List<Workout>> = _workouts.asStateFlow()

    fun workout(id: String): Workout? = _workouts.value.firstOrNull { it.id == id }

    fun upsert(workout: Workout) {
        val current = _workouts.value
        val index = current.indexOfFirst { it.id == workout.id }
        _workouts.value = if (index >= 0) {
            current.toMutableList().apply { set(index, workout) }
        } else {
            current + workout
        }
        persist()
    }

    fun delete(id: String) {
        _workouts.value = _workouts.value.filterNot { it.id == id }
        persist()
    }

    fun duplicate(id: String) {
        val original = workout(id) ?: return
        val copy = original.copy(
            id = newId(),
            name = original.name + " (copy)",
            exercises = original.exercises.map { it.copy(id = newId()) },
        )
        _workouts.value = _workouts.value + copy
        persist()
    }

    private fun loadFromDisk(): List<Workout> {
        return try {
            if (file.exists()) {
                json.decodeFromString(ListSerializer(Workout.serializer()), file.readText())
            } else {
                sampleWorkouts()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persist() {
        val snapshot = _workouts.value
        ioScope.launch {
            writeMutex.withLock {
                try {
                    val tmp = File(file.parentFile, file.name + ".tmp")
                    tmp.writeText(json.encodeToString(ListSerializer(Workout.serializer()), snapshot))
                    if (!tmp.renameTo(file)) {
                        file.writeText(tmp.readText())
                        tmp.delete()
                    }
                } catch (_: Exception) {
                    // Persisting is best-effort; in-memory state stays authoritative.
                }
            }
        }
    }

    private fun sampleWorkouts(): List<Workout> = listOf(
        Workout(
            name = "Full Body Starter",
            rounds = 2,
            roundRestSeconds = 60,
            exercises = listOf(
                Exercise(name = "Jumping jacks", workSeconds = 45, restSeconds = 15),
                Exercise(name = "Push-ups", workSeconds = 30, restSeconds = 20),
                Exercise(name = "Squats", workSeconds = 45, restSeconds = 15),
                Exercise(name = "Plank", workSeconds = 60, restSeconds = 20),
                Exercise(name = "Mountain climbers", workSeconds = 30, restSeconds = 15),
            ),
        )
    )
}
