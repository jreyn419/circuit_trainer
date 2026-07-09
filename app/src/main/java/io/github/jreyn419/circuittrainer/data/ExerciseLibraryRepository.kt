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

    /** Finds a template whose name matches (case-insensitive, trimmed), if any. */
    fun findByName(name: String): ExerciseTemplate? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        return _templates.value.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
    }

    private fun defaultTemplates(): List<ExerciseTemplate> = listOf(
        ExerciseTemplate(
            name = "Jumping jacks", workSeconds = 45, restSeconds = 15,
            description = "Jump while spreading your legs and swinging your arms overhead, then jump back to standing.",
        ),
        ExerciseTemplate(
            name = "Push-ups", workSeconds = 30, restSeconds = 20,
            description = "Keep your body in a straight line from head to heels; lower your chest to just above the floor.",
        ),
        ExerciseTemplate(
            name = "Squats", workSeconds = 45, restSeconds = 15,
            description = "Feet shoulder-width apart, chest up. Sit back and down until your thighs are parallel to the floor.",
        ),
        ExerciseTemplate(
            name = "Plank", workSeconds = 60, restSeconds = 20,
            description = "Forearms on the floor, body straight. Brace your core and don't let your hips sag.",
        ),
        ExerciseTemplate(
            name = "Lunges", workSeconds = 40, restSeconds = 20,
            description = "Step forward and lower until both knees are at 90 degrees. Alternate legs.",
        ),
        ExerciseTemplate(
            name = "Mountain climbers", workSeconds = 30, restSeconds = 20,
            description = "From a push-up position, drive your knees toward your chest one at a time, quickly.",
        ),
        ExerciseTemplate(
            name = "Burpees", workSeconds = 30, restSeconds = 30,
            description = "Squat down, kick back into a push-up, jump your feet forward and leap up with arms overhead.",
        ),
        ExerciseTemplate(
            name = "High knees", workSeconds = 30, restSeconds = 15,
            description = "Run in place, driving your knees up to hip height. Stay light on your feet.",
        ),
    )
}
