package io.github.jreyn419.circuittrainer.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Settings(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    /** "Get ready" countdown before the first exercise, in seconds. */
    val prepSeconds: Int = 10,
)

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(
        Settings(
            soundEnabled = prefs.getBoolean(KEY_SOUND, true),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true),
            prepSeconds = prefs.getInt(KEY_PREP, 10),
        )
    )
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    fun update(transform: (Settings) -> Settings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        prefs.edit()
            .putBoolean(KEY_SOUND, updated.soundEnabled)
            .putBoolean(KEY_VIBRATION, updated.vibrationEnabled)
            .putInt(KEY_PREP, updated.prepSeconds)
            .apply()
    }

    private companion object {
        const val KEY_SOUND = "sound_enabled"
        const val KEY_VIBRATION = "vibration_enabled"
        const val KEY_PREP = "prep_seconds"
    }
}
