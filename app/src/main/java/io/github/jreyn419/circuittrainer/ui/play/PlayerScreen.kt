package io.github.jreyn419.circuittrainer.ui.play

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jreyn419.circuittrainer.ui.theme.PhaseColors
import io.github.jreyn419.circuittrainer.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    workoutId: String,
    onExit: () -> Unit,
    viewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory(workoutId)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var wasRunningBeforeDialog by rememberSaveable { mutableStateOf(false) }

    fun attemptExit() {
        if (state.isFinished || state.workoutMissing) {
            onExit()
        } else {
            wasRunningBeforeDialog = state.isRunning
            viewModel.pause()
            showExitDialog = true
        }
    }

    // The system back gesture pauses and asks instead of silently killing the session.
    BackHandler(enabled = !state.isFinished && !state.workoutMissing) { attemptExit() }

    // Pause automatically whenever the app leaves the foreground (e.g. switching to a
    // music app). The session waits, exactly where it was, until the user comes back.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.autoPause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Keep the screen awake for the whole session.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = {
                showExitDialog = false
                if (wasRunningBeforeDialog) viewModel.resume()
            },
            title = { Text("End workout?") },
            text = { Text("Your progress in this session will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onExit()
                }) { Text("End workout") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    if (wasRunningBeforeDialog) viewModel.resume()
                }) { Text("Keep going") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.workoutName) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = { attemptExit() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.workoutMissing -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("This workout has no exercises.", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onExit) { Text("Go back") }
                    }
                }
            }
            state.isFinished -> FinishedContent(
                state = state,
                onRestart = viewModel::restart,
                onDone = onExit,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> RunningContent(
                state = state,
                onTogglePlayPause = viewModel::togglePlayPause,
                onSkipPrevious = viewModel::skipPrevious,
                onSkipNext = viewModel::skipNext,
                onAddTenSeconds = viewModel::addTenSeconds,
                onEnd = { attemptExit() },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }
}

@Composable
private fun RunningContent(
    state: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onAddTenSeconds: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val segment = state.currentSegment ?: return
    val phaseColor = when (segment.kind) {
        PhaseKind.WORK -> PhaseColors.work
        PhaseKind.REST, PhaseKind.ROUND_REST -> PhaseColors.rest
        PhaseKind.PREP -> PhaseColors.prep
    }
    val phaseLabel = when (segment.kind) {
        PhaseKind.WORK -> "WORK"
        PhaseKind.REST -> "REST"
        PhaseKind.ROUND_REST -> "ROUND BREAK"
        PhaseKind.PREP -> "GET READY"
    }
    // During work: describe the current exercise. During breaks: preview the next one.
    val infoSegment = if (segment.kind == PhaseKind.WORK) segment else state.nextWorkSegment
    var showInfoDialog by rememberSaveable { mutableStateOf(false) }
    if (showInfoDialog && infoSegment != null) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(infoSegment.title) },
            text = { Text(infoSegment.description) },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Close") }
            },
        )
    }

    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Round / exercise progress.
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                text = "Round ${segment.round} of ${state.totalRounds}",
                style = MaterialTheme.typography.titleMedium,
            )
            if (segment.exerciseIndex >= 0) {
                Text(
                    text = "Exercise ${segment.exerciseIndex + 1} of ${state.exercisesPerRound}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        AnimatedVisibility(visible = state.wasAutoPaused && !state.isRunning) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
            ) {
                Text(
                    text = "Paused automatically while you were away. Press play to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        TimerRing(
            fraction = if (segment.durationMillis > 0) {
                state.remainingMillis.toFloat() / segment.durationMillis.toFloat()
            } else 0f,
            color = phaseColor,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = phaseLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = phaseColor,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = formatDuration(((state.remainingMillis + 999) / 1000).toInt()),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displayLarge,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (segment.kind == PhaseKind.WORK) segment.title
                        else state.nextWorkTitle?.let { "Up next: $it" } ?: "Almost done!",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (infoSegment != null && infoSegment.description.isNotBlank()) {
                        IconButton(
                            onClick = { showInfoDialog = true },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = "How to do ${infoSegment.title}",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Transport controls.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            FilledTonalIconButton(onClick = onSkipPrevious, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(28.dp))
            }
            FilledIconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.size(88.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = phaseColor),
            ) {
                Icon(
                    imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isRunning) "Pause" else "Resume",
                    modifier = Modifier.size(44.dp),
                    tint = Color.White,
                )
            }
            FilledTonalIconButton(onClick = onSkipNext, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(28.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            OutlinedButton(onClick = onAddTenSeconds) { Text("+10 s") }
            OutlinedButton(onClick = onEnd) { Text("End workout") }
        }
    }
}

@Composable
private fun TimerRing(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "timerFraction",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

@Composable
private fun FinishedContent(
    state: PlayerUiState,
    onRestart: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "🎉", fontSize = 72.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Workout complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Active time: ${formatDuration((state.elapsedActiveMillis / 1000).toInt())}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Do it again")
        }
    }
}
