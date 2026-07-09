package io.github.jreyn419.circuittrainer

import android.app.Application
import io.github.jreyn419.circuittrainer.data.SettingsRepository
import io.github.jreyn419.circuittrainer.data.WorkoutRepository

class CircuitTrainerApplication : Application() {
    val workoutRepository: WorkoutRepository by lazy { WorkoutRepository(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
