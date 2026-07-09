package io.github.jreyn419.circuittrainer.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Everything the app knows, bundled into one portable JSON document. */
@Serializable
data class BackupData(
    val app: String = "circuit-trainer",
    val format: Int = 1,
    val exportedAtMillis: Long = 0L,
    val workouts: List<Workout> = emptyList(),
    val exerciseLibrary: List<ExerciseTemplate> = emptyList(),
    val settings: Settings? = null,
) {
    val isEmpty: Boolean get() = workouts.isEmpty() && exerciseLibrary.isEmpty()
}

enum class ImportMode {
    /** Upsert by id: matching entries are updated, new ones added, nothing deleted. */
    MERGE,

    /** Wholesale restore: current workouts, library and settings are replaced. */
    REPLACE,
}

class BackupManager(
    private val workoutRepository: WorkoutRepository,
    private val exerciseLibraryRepository: ExerciseLibraryRepository,
    private val settingsRepository: SettingsRepository,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun exportJson(): String = json.encodeToString(
        BackupData.serializer(),
        BackupData(
            exportedAtMillis = System.currentTimeMillis(),
            workouts = workoutRepository.workouts.value,
            exerciseLibrary = exerciseLibraryRepository.templates.value,
            settings = settingsRepository.settings.value,
        ),
    )

    /** Returns null if the text is not a readable backup. */
    fun parse(text: String): BackupData? = try {
        json.decodeFromString(BackupData.serializer(), text)
    } catch (_: Exception) {
        null
    }

    fun apply(backup: BackupData, mode: ImportMode) {
        when (mode) {
            ImportMode.MERGE -> {
                workoutRepository.mergeById(backup.workouts)
                exerciseLibraryRepository.mergeById(backup.exerciseLibrary)
            }
            ImportMode.REPLACE -> {
                workoutRepository.replaceAll(backup.workouts)
                exerciseLibraryRepository.replaceAll(backup.exerciseLibrary)
                backup.settings?.let { restored -> settingsRepository.update { restored } }
            }
        }
    }
}
