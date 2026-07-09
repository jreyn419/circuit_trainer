package io.github.jreyn419.circuittrainer.ui.library

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jreyn419.circuittrainer.CircuitTrainerApplication
import io.github.jreyn419.circuittrainer.data.ExerciseTemplate
import io.github.jreyn419.circuittrainer.ui.components.DurationChip
import io.github.jreyn419.circuittrainer.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as CircuitTrainerApplication
    val repository = app.exerciseLibraryRepository
    val templates by repository.templates.collectAsStateWithLifecycle()

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<ExerciseTemplate?>(null) }
    var templatePendingDelete by rememberSaveable { mutableStateOf<String?>(null) }

    if (showCreateDialog) {
        TemplateDialog(
            title = "New exercise",
            initial = null,
            isNameTaken = { candidate ->
                templates.any { it.name.equals(candidate.trim(), ignoreCase = true) }
            },
            onConfirm = { name, description, work, rest ->
                repository.upsert(
                    ExerciseTemplate(name = name, description = description, workSeconds = work, restSeconds = rest)
                )
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    editingTemplate?.let { template ->
        TemplateDialog(
            title = "Edit exercise",
            initial = template,
            isNameTaken = { candidate ->
                templates.any { it.id != template.id && it.name.equals(candidate.trim(), ignoreCase = true) }
            },
            onConfirm = { name, description, work, rest ->
                repository.upsert(
                    template.copy(name = name, description = description, workSeconds = work, restSeconds = rest)
                )
                editingTemplate = null
            },
            onDismiss = { editingTemplate = null },
        )
    }

    templatePendingDelete?.let { pendingId ->
        val pending = templates.firstOrNull { it.id == pendingId }
        AlertDialog(
            onDismissRequest = { templatePendingDelete = null },
            title = { Text("Delete exercise?") },
            text = {
                Text(
                    "\"${pending?.name ?: "This exercise"}\" will be removed from the library. " +
                        "Workouts that already use it are not affected."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    repository.delete(pendingId)
                    templatePendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { templatePendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercises") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New exercise") },
            )
        },
    ) { padding ->
        if (templates.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No exercises yet", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Saved exercises can be added to any workout with one tap.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(templates.sortedBy { it.name.lowercase() }, key = { it.id }) { template ->
                    Card(onClick = { editingTemplate = template }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = template.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Work ${formatDuration(template.workSeconds)} • Rest ${formatDuration(template.restSeconds)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (template.description.isNotBlank()) {
                                    Text(
                                        text = template.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            IconButton(onClick = { editingTemplate = template }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit ${template.name}")
                            }
                            IconButton(onClick = { templatePendingDelete = template.id }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete ${template.name}",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateDialog(
    title: String,
    initial: ExerciseTemplate?,
    isNameTaken: (String) -> Boolean,
    onConfirm: (name: String, description: String, workSeconds: Int, restSeconds: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(initial?.description.orEmpty()) }
    var workSeconds by rememberSaveable { mutableStateOf(initial?.workSeconds ?: 40) }
    var restSeconds by rememberSaveable { mutableStateOf(initial?.restSeconds ?: 15) }
    val nameTaken = isNameTaken(name)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Push-ups") },
                    isError = nameTaken,
                    supportingText = {
                        if (nameTaken) Text("This name is already in the library")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("Form cues, target muscles, ...") },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    minLines = 2,
                    maxLines = 4,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DurationChip(
                        label = "Work",
                        seconds = workSeconds,
                        onSecondsChange = { workSeconds = it },
                        minSeconds = 5,
                    )
                    DurationChip(
                        label = "Rest",
                        seconds = restSeconds,
                        onSecondsChange = { restSeconds = it },
                    )
                }
                Text(
                    text = "Default times — you can still adjust them per workout.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), description.trim(), workSeconds, restSeconds) },
                enabled = name.isNotBlank() && !nameTaken,
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
