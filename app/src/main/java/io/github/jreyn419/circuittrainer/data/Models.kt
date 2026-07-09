package io.github.jreyn419.circuittrainer.data

import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

@Serializable
data class Exercise(
    val id: String = newId(),
    val name: String = "",
    /** Optional how-to notes, viewable from the workout player. */
    val description: String = "",
    /** Time spent working on this exercise, in seconds. Set per exercise. */
    val workSeconds: Int = 40,
    /** Rest after this exercise, in seconds. Set per exercise. */
    val restSeconds: Int = 15,
)

/**
 * A reusable exercise saved in the library. Its durations are defaults that get
 * copied into a workout when the exercise is picked, and stay editable per workout.
 */
@Serializable
data class ExerciseTemplate(
    val id: String = newId(),
    val name: String = "",
    /** Optional how-to notes, copied into workouts alongside the durations. */
    val description: String = "",
    val workSeconds: Int = 40,
    val restSeconds: Int = 15,
)

@Serializable
data class Workout(
    val id: String = newId(),
    val name: String = "",
    val rounds: Int = 1,
    /** Rest between rounds, in seconds. */
    val roundRestSeconds: Int = 60,
    val exercises: List<Exercise> = emptyList(),
) {
    /** Planned duration of the whole session in seconds (excluding the get-ready countdown). */
    fun totalSeconds(): Int {
        if (exercises.isEmpty() || rounds <= 0) return 0
        var total = 0
        for (round in 1..rounds) {
            exercises.forEachIndexed { index, exercise ->
                total += exercise.workSeconds
                if (index != exercises.lastIndex) total += exercise.restSeconds
            }
            if (round != rounds) total += roundRestSeconds
        }
        return total
    }
}
