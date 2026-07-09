package io.github.jreyn419.circuittrainer.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/**
 * Stores workouts as a small JSON file in app-private storage.
 * Deliberately avoids a database dependency to keep the app lightweight.
 */
class WorkoutRepository(context: Context) {

    private val store = JsonListFile(
        File(context.filesDir, "workouts.json"),
        ListSerializer(Workout.serializer()),
    )

    private val _workouts = MutableStateFlow(store.loadOr { sampleWorkouts() })
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
        store.persistAsync(_workouts.value)
    }

    fun delete(id: String) {
        _workouts.value = _workouts.value.filterNot { it.id == id }
        store.persistAsync(_workouts.value)
    }

    fun duplicate(id: String) {
        val original = workout(id) ?: return
        val copy = original.copy(
            id = newId(),
            name = original.name + " (copy)",
            exercises = original.exercises.map { it.copy(id = newId()) },
        )
        _workouts.value = _workouts.value + copy
        store.persistAsync(_workouts.value)
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
