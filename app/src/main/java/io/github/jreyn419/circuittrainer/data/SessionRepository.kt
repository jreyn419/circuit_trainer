package io.github.jreyn419.circuittrainer.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/**
 * Workout history: every recorded [WorkoutSession], stored as a JSON file next to
 * the workouts. Oldest first on disk; callers sort for display.
 */
class SessionRepository(context: Context) {

    private val store = JsonListFile(
        File(context.filesDir, "sessions.json"),
        ListSerializer(WorkoutSession.serializer()),
    )

    private val _sessions = MutableStateFlow(store.loadOr { emptyList() })
    val sessions: StateFlow<List<WorkoutSession>> = _sessions.asStateFlow()

    fun add(session: WorkoutSession) {
        _sessions.value = _sessions.value.filterNot { it.id == session.id } + session
        store.persistAsync(_sessions.value)
    }

    fun delete(id: String) {
        _sessions.value = _sessions.value.filterNot { it.id == id }
        store.persistAsync(_sessions.value)
    }

    /** Replaces the whole list (backup restore). */
    fun replaceAll(items: List<WorkoutSession>) {
        _sessions.value = items
        store.persistAsync(_sessions.value)
    }

    /** Upserts each item by id, keeping everything else (backup merge). */
    fun mergeById(items: List<WorkoutSession>) {
        if (items.isEmpty()) return
        val merged = _sessions.value.toMutableList()
        for (item in items) {
            val index = merged.indexOfFirst { it.id == item.id }
            if (index >= 0) merged[index] = item else merged += item
        }
        _sessions.value = merged
        store.persistAsync(_sessions.value)
    }
}
