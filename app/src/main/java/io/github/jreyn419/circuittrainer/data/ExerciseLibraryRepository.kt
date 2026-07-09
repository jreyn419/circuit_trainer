package io.github.jreyn419.circuittrainer.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/**
 * The user's library of reusable exercises, available for quick picking
 * when building or editing a workout.
 */
class ExerciseLibraryRepository(context: Context) {

    private val store = JsonListFile(
        File(context.filesDir, "exercise_library.json"),
        ListSerializer(ExerciseTemplate.serializer()),
    )

    private val _templates = MutableStateFlow(store.loadOr { defaultTemplates() })
    val templates: StateFlow<List<ExerciseTemplate>> = _templates.asStateFlow()

    fun upsert(template: ExerciseTemplate) {
        val current = _templates.value
        val index = current.indexOfFirst { it.id == template.id }
        _templates.value = if (index >= 0) {
            current.toMutableList().apply { set(index, template) }
        } else {
            current + template
        }
        store.persistAsync(_templates.value)
    }

    fun delete(id: String) {
        _templates.value = _templates.value.filterNot { it.id == id }
        store.persistAsync(_templates.value)
    }

    /**
     * Saves an exercise from a workout into the library. If a template with the
     * same name already exists (case-insensitive), its durations are updated.
     */
    fun saveFromWorkout(name: String, workSeconds: Int, restSeconds: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val existing = _templates.value.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        if (existing != null) {
            upsert(existing.copy(workSeconds = workSeconds, restSeconds = restSeconds))
        } else {
            upsert(ExerciseTemplate(name = trimmed, workSeconds = workSeconds, restSeconds = restSeconds))
        }
    }

    private fun defaultTemplates(): List<ExerciseTemplate> = listOf(
        ExerciseTemplate(name = "Jumping jacks", workSeconds = 45, restSeconds = 15),
        ExerciseTemplate(name = "Push-ups", workSeconds = 30, restSeconds = 20),
        ExerciseTemplate(name = "Squats", workSeconds = 45, restSeconds = 15),
        ExerciseTemplate(name = "Plank", workSeconds = 60, restSeconds = 20),
        ExerciseTemplate(name = "Lunges", workSeconds = 40, restSeconds = 20),
        ExerciseTemplate(name = "Mountain climbers", workSeconds = 30, restSeconds = 20),
        ExerciseTemplate(name = "Burpees", workSeconds = 30, restSeconds = 30),
        ExerciseTemplate(name = "High knees", workSeconds = 30, restSeconds = 15),
    )
}
