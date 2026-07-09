package io.github.jreyn419.circuittrainer.ui.edit

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jreyn419.circuittrainer.CircuitTrainerApplication
import io.github.jreyn419.circuittrainer.data.Exercise
import io.github.jreyn419.circuittrainer.data.ExerciseTemplate
import io.github.jreyn419.circuittrainer.ui.components.DurationChip
import io.github.jreyn419.circuittrainer.ui.components.StepperRow
import io.github.jreyn419.circuittrainer.util.formatDuration

/** Relationship between a workout exercise and the exercise library, for the bookmark button. */
private enum class LibrarySaveState { UNNAMED, NEW, ALREADY_SAVED, CONFLICT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutScreen(
    workoutId: String?,
    onBack: () -> Unit,
    onOpenLibrary: () -> Unit,
    viewModel: EditorViewModel = viewModel(factory = EditorViewModel.factory(workoutId)),
) {
    val context = LocalContext.current
    val app = context.applicationContext as CircuitTrainerApplication
    val libraryRepository = app.exerciseLibraryRepository
    val templates by libraryRepository.templates.collectAsStateWithLifecycle()
    val workout by viewModel.workout.collectAsStateWithLifecycle()
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    // Only the tapped exercise shows its full editing controls; the rest stay
    // as light one-line summaries so long workouts scroll without lag.
    var expandedExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    var conflictExercise by remember { mutableStateOf<Exercise?>(null) }
    var renameExercise by remember { mutableStateOf<Exercise?>(null) }

    fun attemptBack() {
        if (viewModel.isDirty) showDiscardDialog = true else onBack()
    }

    BackHandler { attemptBack() }

    fun saveToLibrary(exercise: Exercise) {
        libraryRepository.upsert(
            ExerciseTemplate(
                name = exercise.name.trim(),
                description = exercise.description,
                workSeconds = exercise.workSeconds,
                restSeconds = exercise.restSeconds,
            )
        )
        Toast.makeText(context, "Saved to exercise library", Toast.LENGTH_SHORT).show()
    }

    if (showAddSheet) {
        AddExerciseSheet(
            templates = templates,
            onAddBlank = {
                expandedExerciseId = viewModel.addExercise()
                showAddSheet = false
            },
            onAddTemplates = { selected ->
                viewModel.addFromTemplates(selected)
                showAddSheet = false
            },
            onOpenLibrary = {
                showAddSheet = false
                onOpenLibrary()
            },
            onDismiss = { showAddSheet = false },
        )
    }

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

    conflictExercise?.let { exercise ->
        AlertDialog(
            onDismissRequest = { conflictExercise = null },
            title = { Text("Already in library") },
            text = {
                Text(
                    "\"${exercise.name.trim()}\" is in your exercise library with different settings. " +
                        "Overwrite the library entry, or save this as a new exercise under another name?"
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        renameExercise = exercise
                        conflictExercise = null
                    }) { Text("Save as new…") }
                    TextButton(onClick = {
                        libraryRepository.findByName(exercise.name)?.let { existing ->
                            libraryRepository.upsert(
                                existing.copy(
                                    description = exercise.description,
                                    workSeconds = exercise.workSeconds,
                                    restSeconds = exercise.restSeconds,
                                )
                            )
                            Toast.makeText(context, "Library entry updated", Toast.LENGTH_SHORT).show()
                        }
                        conflictExercise = null
                    }) { Text("Overwrite") }
                }
            },
            dismissButton = {
                TextButton(onClick = { conflictExercise = null }) { Text("Cancel") }
            },
        )
    }

    renameExercise?.let { exercise ->
        RenameForLibraryDialog(
            exercise = exercise,
            isNameTaken = { candidate ->
                templates.any { it.name.equals(candidate.trim(), ignoreCase = true) }
            },
            onConfirm = { newName ->
                saveToLibrary(exercise.copy(name = newName))
                renameExercise = null
            },
            onDismiss = { renameExercise = null },
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
            if (workout.exercises.isEmpty()) {
                item {
                    Text(
                        text = "No exercises yet. Add some below - you can pick several from your library at once.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(workout.exercises, key = { it.id }) { exercise ->
                val index = workout.exercises.indexOfFirst { it.id == exercise.id }
                val expanded = expandedExerciseId == exercise.id
                val libraryState = if (!expanded) {
                    LibrarySaveState.UNNAMED // not shown while collapsed
                } else {
                    val existing = templates.firstOrNull {
                        it.name.equals(exercise.name.trim(), ignoreCase = true)
                    }
                    when {
                        exercise.name.isBlank() -> LibrarySaveState.UNNAMED
                        existing == null -> LibrarySaveState.NEW
                        existing.workSeconds == exercise.workSeconds &&
                            existing.restSeconds == exercise.restSeconds &&
                            existing.description == exercise.description -> LibrarySaveState.ALREADY_SAVED
                        else -> LibrarySaveState.CONFLICT
                    }
                }
                ExerciseCard(
                    exercise = exercise,
                    index = index,
                    isFirst = index == 0,
                    isLast = index == workout.exercises.lastIndex,
                    showRest = index != workout.exercises.lastIndex,
                    expanded = expanded,
                    onToggleExpand = {
                        expandedExerciseId = if (expanded) null else exercise.id
                    },
                    libraryState = libraryState,
                    onNameChange = { name -> viewModel.updateExercise(exercise.id) { it.copy(name = name) } },
                    onDescriptionChange = { text -> viewModel.updateExercise(exercise.id) { it.copy(description = text) } },
                    onWorkChange = { s -> viewModel.updateExercise(exercise.id) { it.copy(workSeconds = s) } },
                    onRestChange = { s -> viewModel.updateExercise(exercise.id) { it.copy(restSeconds = s) } },
                    onMoveUp = { viewModel.moveExercise(exercise.id, -1) },
                    onMoveDown = { viewModel.moveExercise(exercise.id, +1) },
                    onDuplicate = { viewModel.duplicateExercise(exercise.id) },
                    onDelete = { viewModel.removeExercise(exercise.id) },
                    onSaveToLibrary = {
                        when (libraryState) {
                            LibrarySaveState.NEW -> saveToLibrary(exercise)
                            LibrarySaveState.CONFLICT -> conflictExercise = exercise
                            else -> {}
                        }
                    },
                )
            }
            item {
                FilledTonalButton(
                    onClick = { showAddSheet = true },
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

/**
 * Bottom sheet for adding exercises. Library exercises can be multi-selected -
 * tap several, then confirm; they are appended in the order they were tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseSheet(
    templates: List<ExerciseTemplate>,
    onAddBlank: () -> Unit,
    onAddTemplates: (List<ExerciseTemplate>) -> Unit,
    onOpenLibrary: () -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedIds = remember { mutableStateListOf<String>() }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "Add exercise",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddBlank)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(16.dp))
                Text("Blank exercise", style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "From your library - tap to select",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onOpenLibrary) { Text("Manage") }
            }
            if (templates.isEmpty()) {
                Text(
                    text = "No saved exercises yet. Tap \"Manage\" to create some.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(templates.sortedBy { it.name.lowercase() }, key = { it.id }) { template ->
                        val selectionIndex = selectedIds.indexOf(template.id)
                        val isSelected = selectionIndex >= 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedIds.remove(template.id)
                                    else selectedIds.add(template.id)
                                }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                                )
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Text(
                                        text = "${selectionIndex + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.size(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(template.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "Work ${formatDuration(template.workSeconds)} • Rest ${formatDuration(template.restSeconds)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        val byId = templates.associateBy { it.id }
                        onAddTemplates(selectedIds.mapNotNull { byId[it] })
                    },
                    enabled = selectedIds.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                ) {
                    Text(
                        if (selectedIds.isEmpty()) "Select exercises to add"
                        else "Add ${selectedIds.size} exercise${if (selectedIds.size == 1) "" else "s"}"
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameForLibraryDialog(
    exercise: Exercise,
    isNameTaken: (String) -> Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(exercise.name.trim()) }
    val taken = isNameTaken(name)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save as new exercise") },
        text = {
            Column {
                Text("Choose a name that isn't already in your library.")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    isError = taken,
                    supportingText = {
                        if (taken) Text("This name is already in the library")
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank() && !taken,
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    showRest: Boolean,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    libraryState: LibrarySaveState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onWorkChange: (Int) -> Unit,
    onRestChange: (Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onSaveToLibrary: () -> Unit,
) {
    Card(onClick = onToggleExpand) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Always-visible summary row; tap to reveal the editing controls.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name.ifBlank { "Exercise ${index + 1}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            append("Work ${formatDuration(exercise.workSeconds)}")
                            if (showRest) append(" • Rest ${formatDuration(exercise.restSeconds)}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = exercise.name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Push-ups") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = exercise.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description (optional)") },
                    placeholder = { Text("Form cues, target muscles, ...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
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
                    when (libraryState) {
                        LibrarySaveState.ALREADY_SAVED -> IconButton(onClick = {}, enabled = false) {
                            Icon(Icons.Default.BookmarkAdded, contentDescription = "Already in library")
                        }
                        LibrarySaveState.UNNAMED -> IconButton(onClick = {}, enabled = false) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = "Name the exercise to save it")
                        }
                        else -> IconButton(onClick = onSaveToLibrary) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = "Save to exercise library")
                        }
                    }
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
}
