package io.github.jreyn419.circuittrainer.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jreyn419.circuittrainer.util.formatDuration

/**
 * A labelled +/- stepper for integer values (rounds, seconds, ...).
 */
@Composable
fun StepperRow(
    label: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    decrementEnabled: Boolean = true,
    incrementEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        FilledTonalIconButton(onClick = onDecrement, enabled = decrementEnabled) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease $label")
        }
        Text(
            text = valueText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(64.dp),
        )
        FilledTonalIconButton(onClick = onIncrement, enabled = incrementEnabled) {
            Icon(Icons.Default.Add, contentDescription = "Increase $label")
        }
    }
}

/**
 * Chip that shows a duration ("Work 0:45") and opens a picker dialog when tapped.
 */
@Composable
fun DurationChip(
    label: String,
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minSeconds: Int = 0,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    AssistChip(
        onClick = { showDialog = true },
        label = { Text("$label ${formatDuration(seconds)}") },
        modifier = modifier,
    )
    if (showDialog) {
        DurationPickerDialog(
            title = label,
            initialSeconds = seconds,
            minSeconds = minSeconds,
            onConfirm = {
                onSecondsChange(it)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

private val presetSeconds = listOf(15, 20, 30, 45, 60, 90, 120)

/**
 * Dialog to pick a duration: quick presets, +/- 5 s steps and direct min/sec entry.
 */
@Composable
fun DurationPickerDialog(
    title: String,
    initialSeconds: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    minSeconds: Int = 0,
) {
    var seconds by rememberSaveable { mutableIntStateOf(initialSeconds) }
    var minutesText by rememberSaveable { mutableStateOf((initialSeconds / 60).toString()) }
    var secondsText by rememberSaveable { mutableStateOf((initialSeconds % 60).toString()) }

    fun applyFields(newMinutes: String, newSeconds: String) {
        minutesText = newMinutes.filter(Char::isDigit).take(2)
        secondsText = newSeconds.filter(Char::isDigit).take(2)
        val m = minutesText.toIntOrNull() ?: 0
        val s = secondsText.toIntOrNull() ?: 0
        seconds = (m * 60 + s).coerceIn(minSeconds, 99 * 60)
    }

    fun setSeconds(newValue: Int) {
        seconds = newValue.coerceIn(minSeconds, 99 * 60)
        minutesText = (seconds / 60).toString()
        secondsText = (seconds % 60).toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(onClick = { setSeconds(seconds - 5) }) {
                        Icon(Icons.Default.Remove, contentDescription = "Minus five seconds")
                    }
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { applyFields(it, secondsText) },
                        label = { Text("min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = { applyFields(minutesText, it) },
                        label = { Text("sec") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    FilledTonalIconButton(onClick = { setSeconds(seconds + 5) }) {
                        Icon(Icons.Default.Add, contentDescription = "Plus five seconds")
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presetSeconds.filter { it >= minSeconds }.forEach { preset ->
                        SuggestionChip(
                            onClick = { setSeconds(preset) },
                            label = { Text(formatDuration(preset)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(seconds.coerceAtLeast(minSeconds)) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
