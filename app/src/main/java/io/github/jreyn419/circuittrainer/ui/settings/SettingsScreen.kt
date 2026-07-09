package io.github.jreyn419.circuittrainer.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jreyn419.circuittrainer.CircuitTrainerApplication
import io.github.jreyn419.circuittrainer.ui.components.StepperRow
import io.github.jreyn419.circuittrainer.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as CircuitTrainerApplication
    val repository = app.settingsRepository
    val settings by repository.settings.collectAsStateWithLifecycle()

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
