package com.example.studyplanner.ui.screens

import android.widget.NumberPicker
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter


@Composable
fun PlanScreen() {
    // In-memory list with stable IDs; dueAt replaces dueWeek
    var nextId by remember { mutableLongStateOf(4L) }
    val assessments = remember {
        mutableStateListOf(
            Assessment(1, "Assessment 1", defaultNextHour()),
            Assessment(2, "Assessment 2", defaultNextHour().plusDays(2)),
            Assessment(3, "Assessment 3", defaultNextHour().plusDays(5))
        )
    }
    var showAdd by remember { mutableStateOf(false) }
    val fmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Plan", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = { showAdd = true }) { Text("Add assessment") }
            }
            Text(
                "Add courses and assessments, then decompose into study sessions.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        items(assessments, key = { it.id }) { plan ->
            val context = LocalContext.current
            var showDecompose by remember { mutableStateOf(false) }
            var showAuto by remember { mutableStateOf(false) }
            var showDelete by remember { mutableStateOf(false) }
            var showEdit by remember { mutableStateOf(false) }

            Card {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(plan.title, style = MaterialTheme.typography.titleMedium)
                        Row {
                            IconButton(onClick = { showEdit = true }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit assessment")
                            }
                            IconButton(onClick = { showDelete = true }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete assessment")
                            }
                        }
                    }
                    Text("Due: ${plan.dueAt.format(fmt)}")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showDecompose = true }) { Text("Decompose Tasks") }
                        OutlinedButton(onClick = { showAuto = true }) { Text("Auto-schedule") }
                    }
                }
            }

            // Edit dialog (title + date/time wheel)
            if (showEdit) {
                var title by remember { mutableStateOf(plan.title) }
                var dueAt by remember { mutableStateOf(plan.dueAt) }
                var openWheel by remember { mutableStateOf(false) }
                val canSave = title.isNotBlank()

                AlertDialog(
                    onDismissRequest = { showEdit = false },
                    title = { Text("Edit assessment") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Title") },
                                singleLine = true
                            )
                            Text("Due at: ${dueAt.format(fmt)}", style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = { openWheel = true }) { Text("Due Date") }
                            Text("Changes are in-memory only.", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = canSave,
                            onClick = {
                                val idx = assessments.indexOfFirst { it.id == plan.id }
                                if (idx >= 0) {
                                    assessments[idx] = plan.copy(title = title.trim(), dueAt = dueAt)
                                    Toast.makeText(context, "Updated ${assessments[idx].title}", Toast.LENGTH_SHORT).show()
                                }
                                showEdit = false
                            }
                        ) { Text("Save") }
                    },
                    dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Cancel") } }
                )

                if (openWheel) {
                    DateTimeWheelDialog(
                        initial = dueAt,
                        onConfirm = { picked -> dueAt = picked; openWheel = false },
                        onDismiss = { openWheel = false }
                    )
                }
            }

            // Delete dialog
            if (showDelete) {
                AlertDialog(
                    onDismissRequest = { showDelete = false },
                    title = { Text("Delete assessment") },
                    text = { Text("Remove \"${plan.title}\" from your plan? This cannot be undone.") },
                    confirmButton = {
                        TextButton(onClick = {
                            assessments.remove(plan)
                            showDelete = false
                            Toast.makeText(context, "Deleted ${plan.title}", Toast.LENGTH_SHORT).show()
                        }) { Text("Delete") }
                    },
                    dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
                )
            }

            // Minimal "Decompose" dialog (no persistence)
            if (showDecompose) {
                var name by remember { mutableStateOf("") }
                var hoursText by remember { mutableStateOf("") }
                val hours = hoursText.toIntOrNull()
                val canAdd = name.isNotBlank() && (hours ?: 0) > 0

                AlertDialog(
                    onDismissRequest = { showDecompose = false },
                    title = { Text("Add a subtask") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Subtask name") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = hoursText,
                                onValueChange = { hoursText = it.filter(Char::isDigit) },
                                label = { Text("Estimated hours") },
                                singleLine = true
                            )
                            Text("Lightweight placeholder — wire to real state later.", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = canAdd,
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Added: \"$name\" (${hours}h) for ${plan.title}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                showDecompose = false
                            }
                        ) { Text("Add") }
                    },
                    dismissButton = { TextButton(onClick = { showDecompose = false }) { Text("Cancel") } }
                )
            }

            // "Auto-schedule" dialog (date/time based)
            if (showAuto) {
                var startAt by remember { mutableStateOf(defaultNextHour()) }
                var openWheel by remember { mutableStateOf(false) }
                var totalHoursText by remember { mutableStateOf("6") }
                var perSessionText by remember { mutableStateOf("2") }

                val totalHours = totalHoursText.toIntOrNull()
                val perSession = perSessionText.toIntOrNull()
                val valid = totalHours != null && totalHours > 0 &&
                        perSession != null && perSession > 0 &&
                        startAt < plan.dueAt

                AlertDialog(
                    onDismissRequest = { showAuto = false },
                    title = { Text("Auto-schedule (preview)") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Start at: ${startAt.format(fmt)}", style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = { openWheel = true }) { Text("Pick start date & time") }
                            OutlinedTextField(
                                value = totalHoursText,
                                onValueChange = { totalHoursText = it.filter(Char::isDigit) },
                                label = { Text("Total planned hours") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = perSessionText,
                                onValueChange = { perSessionText = it.filter(Char::isDigit) },
                                label = { Text("Hours per session") },
                                singleLine = true
                            )
                            Text(
                                "Will spread sessions before ${plan.dueAt.format(fmt)} (no saving yet).",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = valid,
                            onClick = {
                                val th = totalHours ?: return@TextButton
                                val ps = perSession ?: return@TextButton
                                val sessions = (th + ps - 1) / ps
                                Toast.makeText(
                                    context,
                                    "Generated $sessions sessions for ${plan.title} from ${startAt.format(fmt)} to ${plan.dueAt.format(fmt)}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                showAuto = false
                            }
                        ) { Text("Generate") }
                    },
                    dismissButton = { TextButton(onClick = { showAuto = false }) { Text("Cancel") } }
                )

                if (openWheel) {
                    DateTimeWheelDialog(
                        initial = startAt,
                        onConfirm = { picked -> startAt = picked; openWheel = false },
                        onDismiss = { openWheel = false }
                    )
                }
            }
        }
    }

    // Add Assessment dialog (outside LazyColumn)
    if (showAdd) {
        val context = LocalContext.current
        var title by remember { mutableStateOf("Assessment ${assessments.size + 1}") }
        var dueAt by remember { mutableStateOf(defaultNextHour()) }
        var openWheel by remember { mutableStateOf(false) }
        val canAdd = title.isNotBlank()
        val fmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("New assessment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true
                    )
                    Text("Due at: ${dueAt.format(fmt)}", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { openWheel = true }) { Text("Due Date") }
                    Text("Note: stored in memory only (clears on restart).", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = canAdd,
                    onClick = {
                        assessments.add(
                            Assessment(
                                id = nextId++,
                                title = title.trim(),
                                dueAt = dueAt
                            )
                        )
                        Toast.makeText(context, "Added ${title.trim()}", Toast.LENGTH_SHORT).show()
                        showAdd = false
                    }
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )

        if (openWheel) {
            DateTimeWheelDialog(
                initial = dueAt,
                onConfirm = { picked -> dueAt = picked; openWheel = false },
                onDismiss = { openWheel = false }
            )
        }
    }
}

/* ===================== Helpers & models ===================== */

private data class Assessment(
    val id: Long,
    val title: String,
    val dueAt: LocalDateTime
)

private fun defaultNextHour(): LocalDateTime {
    val now = LocalDateTime.now().withSecond(0).withNano(0)
    return if (now.minute == 0) now.plusHours(1) else now.withMinute(0).plusHours(1)
}

/* ---------- Scrollable wheel Date/Time picker using NumberPicker ---------- */

@Composable
private fun DateTimeWheelDialog(
    initial: LocalDateTime,
    onConfirm: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableIntStateOf(initial.monthValue) }
    var day by remember { mutableIntStateOf(initial.dayOfMonth) }
    var hour by remember { mutableIntStateOf(initial.hour) }
    var minute by remember { mutableIntStateOf(initial.minute) }

    // Hard bounds: now .. now + 1 year
    val minDT = remember { LocalDateTime.now().withSecond(0).withNano(0) }
    val maxDT = remember { minDT.plusYears(1) }

    // Year range
    val yearMin = minDT.year
    val yearMax = maxDT.year
    year = year.coerceIn(yearMin, yearMax)

    // Month range depends on selected year
    val monthMin = if (year == yearMin) minDT.monthValue else 1
    val monthMax = if (year == yearMax) maxDT.monthValue else 12
    month = month.coerceIn(monthMin, monthMax)

    // Day range depends on selected year+month and bounds
    val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
    val dayMin = if (year == yearMin && month == minDT.monthValue) minDT.dayOfMonth else 1
    val dayMax = if (year == yearMax && month == maxDT.monthValue) minOf(daysInMonth, maxDT.dayOfMonth) else daysInMonth
    day = day.coerceIn(dayMin, dayMax)

    // Hour range depends on selected date and bounds
    val hourMin = if (year == yearMin && month == minDT.monthValue && day == minDT.dayOfMonth) minDT.hour else 0
    val hourMax = if (year == yearMax && month == maxDT.monthValue && day == maxDT.dayOfMonth) maxDT.hour else 23
    hour = hour.coerceIn(hourMin, hourMax)

    // Minute range depends on selected date+hour and bounds
    val minuteMin = if (
        year == yearMin && month == minDT.monthValue && day == minDT.dayOfMonth && hour == minDT.hour
    ) minDT.minute else 0
    val minuteMax = if (
        year == yearMax && month == maxDT.monthValue && day == maxDT.dayOfMonth && hour == maxDT.hour
    ) maxDT.minute else 59
    minute = minute.coerceIn(minuteMin, minuteMax)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Due Date") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NumberWheel("Year", year, yearMin..yearMax, { year = it }, Modifier.weight(1f))
                    NumberWheel("Month", month, monthMin..monthMax, { month = it }, Modifier.weight(1f))
                    NumberWheel("Day", day, dayMin..dayMax, { day = it }, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NumberWheel("Hour", hour, hourMin..hourMax, { hour = it }, Modifier.weight(1f))
                    NumberWheel("Minute", minute, minuteMin..minuteMax, { minute = it }, Modifier.weight(1f))
                }
                Text("Past time will auto-adjust to now.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val picked = LocalDateTime.of(year, month, day, hour, minute)
                val clamped = when {
                    picked.isBefore(minDT) -> minDT
                    picked.isAfter(maxDT)  -> maxDT
                    else -> picked
                }
                onConfirm(clamped)
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun NumberWheel(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        AndroidView(
            modifier = Modifier.height(120.dp).fillMaxWidth(),
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = range.first
                    maxValue = range.last
                    wrapSelectorWheel = true
                    this.value = value.coerceIn(minValue, maxValue)
                    setOnValueChangedListener { _, _, newVal -> onValueChange(newVal) }
                }
            },
            update = { picker ->
                if (picker.minValue != range.first || picker.maxValue != range.last) {
                    picker.minValue = range.first
                    picker.maxValue = range.last
                }
                val desired = value.coerceIn(picker.minValue, picker.maxValue)
                if (picker.value != desired) picker.value = desired

            }
        )
    }
}
