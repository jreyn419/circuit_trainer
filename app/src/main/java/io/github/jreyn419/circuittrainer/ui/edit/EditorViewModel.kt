package io.github.jreyn419.circuittrainer.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.jreyn419.circuittrainer.CircuitTrainerApplication
import io.github.jreyn419.circuittrainer.data.Exercise
import io.github.jreyn419.circuittrainer.data.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditorViewModel(
    application: CircuitTrainerApplication,
    workoutId: String?,
) : ViewModel() {

    private val repository = application.workoutRepository

    val isNew = workoutId == null
    private val initial: Workout = workoutId?.let { repository.workout(it) }
        ?: Workout(name = "", exercises = listOf(Exercise()))

    private val _workout = MutableStateFlow(initial)
    val workout: StateFlow<Workout> = _workout.asStateFlow()

    val isDirty: Boolean get() = _workout.value != initial

    fun setName(name: String) {
        _workout.value = _workout.value.copy(name = name)
    }

    fun setRounds(rounds: Int) {
        _workout.value = _workout.value.copy(rounds = rounds.coerceIn(1, 20))
    }

    fun setRoundRestSeconds(seconds: Int) {
        _workout.value = _workout.value.copy(roundRestSeconds = seconds.coerceIn(0, 99 * 60))
    }

    fun updateExercise(id: String, transform: (Exercise) -> Exercise) {
        _workout.value = _workout.value.copy(
            exercises = _workout.value.exercises.map { if (it.id == id) transform(it) else it }
        )
    }

    fun addExercise() {
        val exercises = _workout.value.exercises
        // New exercises inherit the durations of the previous one - usually what you want.
        val template = exercises.lastOrNull()
        _workout.value = _workout.value.copy(
            exercises = exercises + Exercise(
                workSeconds = template?.workSeconds ?: 40,
                restSeconds = template?.restSeconds ?: 15,
            )
        )
    }

    fun duplicateExercise(id: String) {
        val exercises = _workout.value.exercises
        val index = exercises.indexOfFirst { it.id == id }
        if (index < 0) return
        val copy = exercises[index].copy(id = io.github.jreyn419.circuittrainer.data.newId())
        _workout.value = _workout.value.copy(
            exercises = exercises.toMutableList().apply { add(index + 1, copy) }
        )
    }

    fun removeExercise(id: String) {
        _workout.value = _workout.value.copy(
            exercises = _workout.value.exercises.filterNot { it.id == id }
        )
    }

    /** Moves the exercise up (delta = -1) or down (delta = +1) in the list. */
    fun moveExercise(id: String, delta: Int) {
        val exercises = _workout.value.exercises.toMutableList()
        val index = exercises.indexOfFirst { it.id == id }
        val target = index + delta
        if (index < 0 || target < 0 || target > exercises.lastIndex) return
        val item = exercises.removeAt(index)
        exercises.add(target, item)
        _workout.value = _workout.value.copy(exercises = exercises)
    }

    val canSave: Boolean get() = _workout.value.exercises.isNotEmpty()

    fun save() {
        val edited = _workout.value
        if (edited.exercises.isEmpty()) return
        val cleaned = edited.copy(
            name = edited.name.trim().ifBlank { "Workout" },
            exercises = edited.exercises.mapIndexed { index, exercise ->
                exercise.copy(name = exercise.name.trim().ifBlank { "Exercise ${index + 1}" })
            },
        )
        repository.upsert(cleaned)
    }

    companion object {
        fun factory(workoutId: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as CircuitTrainerApplication
                EditorViewModel(app, workoutId)
            }
        }
    }
}
