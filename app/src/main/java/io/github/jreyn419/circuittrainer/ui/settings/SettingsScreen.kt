package io.github.jreyn419.circuittrainer.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jreyn419.circuittrainer.CircuitTrainerApplication
import io.github.jreyn419.circuittrainer.data.BackupData
import io.github.jreyn419.circuittrainer.data.ImportMode
import io.github.jreyn419.circuittrainer.ui.components.StepperRow
import io.github.jreyn419.circuittrainer.util.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as CircuitTrainerApplication
    val repository = app.settingsRepository
    val settings by repository.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    var pendingImport by remember { mutableStateOf<BackupData?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val result = try {
                    context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                        it.write(app.backupManager.exportJson())
                    } ?: throw IllegalStateException("Could not open file")
                    "Backup saved"
                } catch (_: Exception) {
                    "Export failed - couldn't write to that location"
                }
                withContext(Dispatchers.Main) { toast(result) }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val text = try {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                } catch (_: Exception) {
                    null
                }
                val backup = text?.let { app.backupManager.parse(it) }
                withContext(Dispatchers.Main) {
                    if (backup == null || backup.isEmpty) {
                        toast("That file doesn't contain Circuit Trainer data")
                    } else {
                        pendingImport = backup
                    }
                }
            }
        }
    }

    pendingImport?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Import backup") },
            text = {
                Text(
                    "This backup contains ${backup.workouts.size} workout(s) and " +
                        "${backup.exerciseLibrary.size} library exercise(s).\n\n" +
                        "Merge keeps your current data and adds/updates entries from the backup. " +
                        "Replace deletes your current workouts and library first."
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        app.backupManager.apply(backup, ImportMode.REPLACE)
                        pendingImport = null
                        toast("Backup restored")
                    }) { Text("Replace") }
                    TextButton(onClick = {
                        app.backupManager.apply(backup, ImportMode.MERGE)
                        pendingImport = null
                        toast("Backup merged")
                    }) { Text("Merge") }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            SwitchRow(
                title = "Sound",
                subtitle = "Beeps for the countdown and phase changes",
                checked = settings.soundEnabled,
                onCheckedChange = { checked -> repository.update { it.copy(soundEnabled = checked) } },
            )
            HorizontalDivider()
            SwitchRow(
                title = "Vibration",
                subtitle = "Vibrate when a new phase starts",
                checked = settings.vibrationEnabled,
                onCheckedChange = { checked -> repository.update { it.copy(vibrationEnabled = checked) } },
            )
            HorizontalDivider()
            StepperRow(
                label = "Get-ready countdown",
                valueText = formatDuration(settings.prepSeconds),
                onDecrement = { repository.update { it.copy(prepSeconds = (it.prepSeconds - 5).coerceAtLeast(0)) } },
                onIncrement = { repository.update { it.copy(prepSeconds = (it.prepSeconds + 5).coerceAtMost(60)) } },
                decrementEnabled = settings.prepSeconds > 0,
                incrementEnabled = settings.prepSeconds < 60,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Text(
                text = "Time to get in position before the first exercise starts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "Data",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
            )
            HorizontalDivider()
            ActionRow(
                title = "Export data",
                subtitle = "Save workouts, exercises and settings to a JSON file",
                icon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                onClick = {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    exportLauncher.launch("circuit-trainer-backup-$date.json")
                },
            )
            HorizontalDivider()
            ActionRow(
                title = "Import data",
                subtitle = "Restore from a previously exported backup",
                icon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                onClick = { importLauncher.launch(arrayOf("*/*")) },
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
