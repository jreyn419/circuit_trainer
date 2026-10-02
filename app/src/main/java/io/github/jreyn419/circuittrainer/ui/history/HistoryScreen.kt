package io.github.jreyn419.circuittrainer.ui.history

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jreyn419.circuittrainer.CircuitTrainerApplication
import io.github.jreyn419.circuittrainer.data.WorkoutSession
import io.github.jreyn419.circuittrainer.ui.theme.PhaseColors
import io.github.jreyn419.circuittrainer.util.formatDuration
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters

/** Rows of one calendar day, newest session first. */
data class HistoryDay(val date: LocalDate, val sessions: List<WorkoutSession>)

/** Totals shown above the list. */
data class HistorySummary(
    val weekCount: Int,
    val weekActiveSeconds: Int,
    val allCount: Int,
    val allActiveSeconds: Int,
)

/** Groups sessions by local day, newest day and newest session first. */
fun groupByDay(sessions: List<WorkoutSession>, zone: ZoneId): List<HistoryDay> =
    sessions
        .sortedByDescending { it.startedAtMillis }
        .groupBy { Instant.ofEpochMilli(it.startedAtMillis).atZone(zone).toLocalDate() }
        .map { (date, rows) -> HistoryDay(date, rows) }
        .sortedByDescending { it.date }

/** Week runs Monday to Sunday in the device's time zone. */
fun summarize(sessions: List<WorkoutSession>, today: LocalDate, zone: ZoneId): HistorySummary {
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekStartMillis = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
    val thisWeek = sessions.filter { it.startedAtMillis >= weekStartMillis }
    return HistorySummary(
        weekCount = thisWeek.size,
        weekActiveSeconds = (thisWeek.sumOf { it.activeMillis } / 1000).toInt(),
        allCount = sessions.size,
        allActiveSeconds = (sessions.sumOf { it.activeMillis } / 1000).toInt(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onPlayWorkout: (String) -> Unit,
) {
    val app = LocalContext.current.applicationContext as CircuitTrainerApplication
    val sessions by app.sessionRepository.sessions.collectAsStateWithLifecycle()
    val workouts by app.workoutRepository.workouts.collectAsStateWithLifecycle()
    val zone = remember { ZoneId.systemDefault() }
    val today = remember(sessions) { LocalDate.now(zone) }
    val days = remember(sessions) { groupByDay(sessions, zone) }
    val summary = remember(sessions, today) { summarize(sessions, today, zone) }
    val existingWorkoutIds = remember(workouts) { workouts.map { it.id }.toSet() }
    var sessionPendingDelete by rememberSaveable { mutableStateOf<String?>(null) }

    sessionPendingDelete?.let { pendingId ->
        AlertDialog(
            onDismissRequest = { sessionPendingDelete = null },
            title = { Text("Delete this entry?") },
            text = { Text("It will be removed from your history permanently.") },
            confirmButton = {
                TextButton(onClick = {
                    app.sessionRepository.delete(pendingId)
                    sessionPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { sessionPendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp),
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No workouts yet", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Finished workouts show up here, with the time you put in.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "summary") { SummaryCard(summary) }
                for (day in days) {
                    item(key = "day-${day.date}") {
                        Text(
                            text = dayLabel(day.date, today),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                        )
                    }
                    items(day.sessions, key = { it.id }) { session ->
                        SessionRow(
                            session = session,
                            zone = zone,
                            canRepeat = session.workoutId in existingWorkoutIds,
                            onRepeat = { onPlayWorkout(session.workoutId) },
                            onDelete = { sessionPendingDelete = session.id },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: HistorySummary) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Stat(label = "This week", value = pluralWorkouts(summary.weekCount))
            Stat(label = "Active", value = formatDuration(summary.weekActiveSeconds))
            Stat(label = "All time", value = pluralWorkouts(summary.allCount))
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SessionRow(
    session: WorkoutSession,
    zone: ZoneId,
    canRepeat: Boolean,
    onRepeat: () -> Unit,
    onDelete: () -> Unit,
) {
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val startedAt = remember(session.startedAtMillis) {
        Instant.ofEpochMilli(session.startedAtMillis).atZone(zone).toLocalTime().format(timeFormatter)
    }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.workoutName.ifBlank { "Unnamed workout" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$startedAt • ${formatDuration((session.activeMillis / 1000).toInt())} active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                if (session.completed) {
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.labelMedium,
                        color = PhaseColors.work,
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    Text(
                        text = "Ended early • ${session.workSegmentsDone} of ${session.workSegmentsTotal} intervals",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box {
                var menuOpen by remember { mutableStateOf(false) }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (canRepeat) {
                        DropdownMenuItem(
                            text = { Text("Do it again") },
                            onClick = { menuOpen = false; onRepeat() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")
private val dayWithYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")

fun dayLabel(date: LocalDate, today: LocalDate): String = when {
    date == today -> "Today"
    date == today.minusDays(1) -> "Yesterday"
    date.year == today.year -> date.format(dayFormatter)
    else -> date.format(dayWithYearFormatter)
}

private fun pluralWorkouts(count: Int): String =
    if (count == 1) "1 workout" else "$count workouts"
