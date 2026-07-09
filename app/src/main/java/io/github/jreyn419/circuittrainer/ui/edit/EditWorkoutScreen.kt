package io.github.jreyn419.circuittrainer.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jreyn419.circuittrainer.data.Exercise
import io.github.jreyn419.circuittrainer.ui.components.DurationChip
import io.github.jreyn419.circuittrainer.ui.components.StepperRow
import io.github.jreyn419.circuittrainer.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutScreen(
    workoutId: String?,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel(factory = EditorViewModel.factory(workoutId)),
) {
    val workout by viewModel.workout.collectAsStateWithLifecycle()
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    fun attemptBack() {
        if (viewModel.isDirty) showDiscardDialog = true else onBack()
    }

    BackHandler { attemptBack() }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("Your edits to this workout will not be saved.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep editing") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "New workout" else "Edit workout") },
                navigationIcon = {
                    IconButton(onClick = { attemptBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.save()
                            onBack()
                        },
                        enabled = viewModel.canSave,
                    ) { Text("Save") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = workout.name,
                    onValueChange = viewModel::setName,
                    label = { Text("Workout name") },
                    placeholder = { Text("e.g. Morning circuit") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                Card {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        StepperRow(
                            label = "Rounds",
                            valueText = workout.rounds.toString(),
                            onDecrement = { viewModel.setRounds(workout.rounds - 1) },
                            onIncrement = { viewModel.setRounds(workout.rounds + 1) },
                            decrementEnabled = workout.rounds > 1,
                            incrementEnabled = workout.rounds < 20,
                        )
                        if (workout.rounds > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Rest between rounds",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                DurationChip(
                                    label = "Break",
                                    seconds = workout.roundRestSeconds,
                                    onSecondsChange = viewModel::setRoundRestSeconds,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    text = "Exercises",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(workout.exercises, key = { it.id }) { exercise ->
                val index = workout.exercises.indexOfFirst { it.id == exercise.id }
                ExerciseCard(
                    exercise = exercise,
                    index = index,
                    isFirst = index == 0,
                    isLast = index == workout.exercises.lastIndex,
                    showRest = index != workout.exercises.lastIndex,
                    onNameChange = { name -> viewModel.updateExercise(exercise.id) { it.copy(name = name) } },
                    onWorkChange = { s -> viewModel.updateExercise(exercise.id) { it.copy(workSeconds = s) } },
                    onRestChange = { s -> viewModel.updateExercise(exercise.id) { it.copy(restSeconds = s) } },
                    onMoveUp = { viewModel.moveExercise(exercise.id, -1) },
                    onMoveDown = { viewModel.moveExercise(exercise.id, +1) },
                    onDuplicate = { viewModel.duplicateExercise(exercise.id) },
                    onDelete = { viewModel.removeExercise(exercise.id) },
                )
            }
            item {
                FilledTonalButton(
                    onClick = viewModel::addExercise,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Add exercise")
                }
            }
            item {
                Text(
                    text = "Total workout time: ${formatDuration(workout.totalSeconds())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    showRest: Boolean,
    onNameChange: (String) -> Unit,
    onWorkChange: (Int) -> Unit,
    onRestChange: (Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = exercise.name,
                onValueChange = onNameChange,
                label = { Text("Exercise ${index + 1}") },
                placeholder = { Text("e.g. Push-ups") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                DurationChip(
                    label = "Work",
                    seconds = exercise.workSeconds,
                    onSecondsChange = onWorkChange,
                    minSeconds = 5,
                )
                if (showRest) {
                    DurationChip(
                        label = "Rest",
                        seconds = exercise.restSeconds,
                        onSecondsChange = onRestChange,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                }
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
