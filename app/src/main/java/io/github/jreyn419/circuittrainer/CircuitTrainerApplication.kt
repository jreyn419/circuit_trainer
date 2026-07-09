package io.github.jreyn419.circuittrainer

import android.app.Application
import io.github.jreyn419.circuittrainer.data.BackupManager
import io.github.jreyn419.circuittrainer.data.ExerciseLibraryRepository
import io.github.jreyn419.circuittrainer.data.SettingsRepository
import io.github.jreyn419.circuittrainer.data.WorkoutRepository

class CircuitTrainerApplication : Application() {
    val workoutRepository: WorkoutRepository by lazy { WorkoutRepository(this) }
    val exerciseLibraryRepository: ExerciseLibraryRepository by lazy { ExerciseLibraryRepository(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val backupManager: BackupManager by lazy {
        BackupManager(workoutRepository, exerciseLibraryRepository, settingsRepository)
    }
}
