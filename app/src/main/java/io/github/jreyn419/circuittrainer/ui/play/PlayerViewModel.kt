package io.github.jreyn419.circuittrainer.ui.play

import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.jreyn419.circuittrainer.CircuitTrainerApplication
import io.github.jreyn419.circuittrainer.data.Workout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PhaseKind { PREP, WORK, REST, ROUND_REST }

data class Segment(
    val kind: PhaseKind,
    val title: String,
    val durationMillis: Long,
    /** Index of the related exercise within the round; -1 for prep / round breaks. */
    val exerciseIndex: Int,
    /** 1-based round this segment belongs to. */
    val round: Int,
)

data class PlayerUiState(
    val workoutName: String = "",
    val segments: List<Segment> = emptyList(),
    val segmentIndex: Int = 0,
    val remainingMillis: Long = 0L,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    /** True when the app paused itself because it went to the background. */
    val wasAutoPaused: Boolean = false,
    val totalRounds: Int = 1,
    val exercisesPerRound: Int = 0,
    val elapsedActiveMillis: Long = 0L,
    val workoutMissing: Boolean = false,
) {
    val currentSegment: Segment? get() = segments.getOrNull(segmentIndex)

    /** Name of the next exercise coming up after the current segment, if any. */
    val nextWorkTitle: String?
        get() = segments.drop(segmentIndex + 1).firstOrNull { it.kind == PhaseKind.WORK }?.title
}

/**
 * Drives a running workout session.
 *
 * The timer derives the remaining time from [SystemClock.elapsedRealtime] instead of
 * counting ticks, so it can never drift or freeze: every tick recomputes the truth.
 * The session lives in a ViewModel, so rotating the screen never loses progress.
 */
class PlayerViewModel(
    application: CircuitTrainerApplication,
    workoutId: String,
) : AndroidViewModel(application) {

    private val settingsRepository = application.settingsRepository
    private val feedback = Feedback(application)

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var tickerJob: Job? = null
    private var endAtElapsed = 0L
    private var lastTickElapsed = 0L
    private var lastBeepSecond = 0

    private val soundEnabled get() = settingsRepository.settings.value.soundEnabled
    private val vibrationEnabled get() = settingsRepository.settings.value.vibrationEnabled

    init {
        val workout = application.workoutRepository.workout(workoutId)
        if (workout == null || workout.exercises.isEmpty()) {
            _state.value = PlayerUiState(workoutMissing = true)
        } else {
            val segments = buildSegments(workout, settingsRepository.settings.value.prepSeconds)
            _state.value = PlayerUiState(
                workoutName = workout.name,
                segments = segments,
                segmentIndex = 0,
                remainingMillis = segments.first().durationMillis,
                totalRounds = workout.rounds,
                exercisesPerRound = workout.exercises.size,
            )
            resume()
        }
    }

    fun resume() {
        val s = _state.value
        if (s.isFinished || s.isRunning || s.segments.isEmpty()) return
        _state.value = s.copy(isRunning = true, wasAutoPaused = false)
        startTicker()
    }

    fun pause(auto: Boolean = false) {
        val s = _state.value
        if (!s.isRunning) return
        tickerJob?.cancel()
        val remaining = (endAtElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        _state.value = s.copy(isRunning = false, remainingMillis = remaining, wasAutoPaused = auto)
    }

    fun togglePlayPause() {
        if (_state.value.isRunning) pause() else resume()
    }

    /** Called when the app goes to the background: pause instead of running blind. */
    fun autoPause() {
        pause(auto = true)
    }

    fun skipNext() {
        val s = _state.value
        if (s.isFinished || s.segments.isEmpty()) return
        if (s.segmentIndex >= s.segments.lastIndex) finish() else goToSegment(s.segmentIndex + 1)
    }

    fun skipPrevious() {
        val s = _state.value
        if (s.isFinished || s.segments.isEmpty()) return
        val current = s.currentSegment ?: return
        val elapsedInSegment = current.durationMillis - s.remainingMillis
        // Like a music player: restart the current phase unless we just entered it.
        if (elapsedInSegment > 2000L || s.segmentIndex == 0) {
            goToSegment(s.segmentIndex)
        } else {
            goToSegment(s.segmentIndex - 1)
        }
    }

    fun addTenSeconds() {
        val s = _state.value
        if (s.isFinished || s.segments.isEmpty()) return
        val newRemaining = s.remainingMillis + 10_000L
        endAtElapsed += 10_000L
        _state.value = s.copy(remainingMillis = newRemaining)
    }

    fun restart() {
        val s = _state.value
        if (s.segments.isEmpty()) return
        tickerJob?.cancel()
        lastBeepSecond = 0
        _state.value = s.copy(
            segmentIndex = 0,
            remainingMillis = s.segments.first().durationMillis,
            isRunning = false,
            isFinished = false,
            wasAutoPaused = false,
            elapsedActiveMillis = 0L,
        )
        resume()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        endAtElapsed = SystemClock.elapsedRealtime() + _state.value.remainingMillis
        lastTickElapsed = SystemClock.elapsedRealtime()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(100)
                onTick()
            }
        }
    }

    private fun onTick() {
        val s = _state.value
        if (!s.isRunning) return
        val now = SystemClock.elapsedRealtime()
        val delta = now - lastTickElapsed
        lastTickElapsed = now
        val elapsedActive = s.elapsedActiveMillis + delta
        val remaining = endAtElapsed - now
        if (remaining <= 0L) {
            advance(s.copy(elapsedActiveMillis = elapsedActive), carryMillis = -remaining)
        } else {
            val secondsLeft = ((remaining + 999) / 1000).toInt()
            if (secondsLeft in 1..3 && secondsLeft != lastBeepSecond) {
                lastBeepSecond = secondsLeft
                feedback.countdownTick(soundEnabled)
            }
            _state.value = s.copy(remainingMillis = remaining, elapsedActiveMillis = elapsedActive)
        }
    }

    private fun advance(base: PlayerUiState, carryMillis: Long) {
        val nextIndex = base.segmentIndex + 1
        if (nextIndex >= base.segments.size) {
            tickerJob?.cancel()
            _state.value = base.copy(remainingMillis = 0L, isRunning = false, isFinished = true)
            feedback.workoutFinished(soundEnabled, vibrationEnabled)
        } else {
            val segment = base.segments[nextIndex]
            val newRemaining = (segment.durationMillis - carryMillis).coerceAtLeast(0L)
            lastBeepSecond = 0
            endAtElapsed = SystemClock.elapsedRealtime() + newRemaining
            _state.value = base.copy(segmentIndex = nextIndex, remainingMillis = newRemaining)
            feedback.phaseChange(soundEnabled, vibrationEnabled)
        }
    }

    private fun finish() {
        val s = _state.value
        tickerJob?.cancel()
        _state.value = s.copy(remainingMillis = 0L, isRunning = false, isFinished = true)
        feedback.workoutFinished(soundEnabled, vibrationEnabled)
    }

    private fun goToSegment(index: Int) {
        val s = _state.value
        val target = index.coerceIn(0, s.segments.lastIndex)
        val segment = s.segments[target]
        lastBeepSecond = 0
        endAtElapsed = SystemClock.elapsedRealtime() + segment.durationMillis
        _state.value = s.copy(segmentIndex = target, remainingMillis = segment.durationMillis)
    }

    override fun onCleared() {
        feedback.release()
    }

    companion object {
        fun factory(workoutId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as CircuitTrainerApplication
                PlayerViewModel(app, workoutId)
            }
        }

        fun buildSegments(workout: Workout, prepSeconds: Int): List<Segment> {
            val segments = mutableListOf<Segment>()
            if (workout.exercises.isEmpty()) return segments
            if (prepSeconds > 0) {
                segments += Segment(PhaseKind.PREP, "Get ready", prepSeconds * 1000L, -1, 1)
            }
            for (round in 1..workout.rounds) {
                workout.exercises.forEachIndexed { index, exercise ->
                    if (exercise.workSeconds > 0) {
                        segments += Segment(
                            PhaseKind.WORK,
                            exercise.name.ifBlank { "Exercise ${index + 1}" },
                            exercise.workSeconds * 1000L,
                            index,
                            round,
                        )
                    }
                    if (index != workout.exercises.lastIndex && exercise.restSeconds > 0) {
                        segments += Segment(PhaseKind.REST, "Rest", exercise.restSeconds * 1000L, index, round)
                    }
                }
                if (round != workout.rounds && workout.roundRestSeconds > 0) {
                    segments += Segment(PhaseKind.ROUND_REST, "Round break", workout.roundRestSeconds * 1000L, -1, round)
                }
            }
            return segments
        }
    }
}
