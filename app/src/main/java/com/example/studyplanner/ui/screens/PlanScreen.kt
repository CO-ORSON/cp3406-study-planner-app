@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.example.studyplanner.ui.screens

import android.widget.NumberPicker
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlaylistAddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studyplanner.ui.plan.PlanViewModel
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun PlanScreen(vm: PlanViewModel = viewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    val fmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Plan", style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.PlaylistAddCircle, contentDescription = "Add assessment")
                }
            }
            Text(
                "Add courses and assessments, then decompose into subtasks with due dates.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        items(items, key = { it.id }) { plan ->
            val context = LocalContext.current
            var showDecompose by remember { mutableStateOf(false) }
            var showRemark by remember { mutableStateOf(false) }   // ← was showAuto
            var showDelete by remember { mutableStateOf(false) }
            var showEdit by remember { mutableStateOf(false) }

            // subtask edit/delete state
            var editingSubId by remember { mutableStateOf<Long?>(null) }
            var editingSubName by remember { mutableStateOf("") }
            var editingSubDue by remember { mutableStateOf(plan.dueAt) }
            var openSubWheel by remember { mutableStateOf(false) }

            var deletingSubId by remember { mutableStateOf<Long?>(null) }
            var deletingSubName by remember { mutableStateOf("") }

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
                        OutlinedButton(onClick = { showRemark = true }) { Text("Remark") }
                    }

                    if (plan.subtasks.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Subtasks:", style = MaterialTheme.typography.labelLarge)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            plan.subtasks.forEach { st ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "• ${st.name} — due ${st.dueAt.format(fmt)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row {
                                        IconButton(onClick = {
                                            editingSubId = st.id
                                            editingSubName = st.name
                                            editingSubDue = st.dueAt
                                        }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit subtask")
                                        }
                                        IconButton(onClick = {
                                            deletingSubId = st.id
                                            deletingSubName = st.name
                                        }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete subtask")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ✅ show saved remark from DB
                    if (plan.remark.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Remark: ${plan.remark}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Edit assessment dialog
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
                            OutlinedButton(onClick = { openWheel = true }) { Text("Pick due date") }
                            Text("Changes are saved to local database.", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = canSave,
                            onClick = {
                                vm.updateAssessment(plan.id, title.trim(), dueAt)
                                Toast.makeText(context, "Updated ${title.trim()}", Toast.LENGTH_SHORT).show()
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

            // Edit subtask dialog
            if (editingSubId != null) {
                val canSaveSub = editingSubName.isNotBlank() && editingSubDue <= plan.dueAt
                AlertDialog(
                    onDismissRequest = { editingSubId = null },
                    title = { Text("Edit subtask") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = editingSubName,
                                onValueChange = { editingSubName = it },
                                label = { Text("Subtask name") },
                                singleLine = true
                            )
                            Text("Due at: ${editingSubDue.format(fmt)}", style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = { openSubWheel = true }) { Text("Pick subtask due date") }
                            Text(
                                "Tip: subtask due should not exceed the assessment due date (${plan.dueAt.format(fmt)}).",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = canSaveSub,
                            onClick = {
                                vm.updateSubtask(plan.id, editingSubId!!, editingSubName.trim(), editingSubDue)
                                Toast.makeText(context, "Subtask updated", Toast.LENGTH_SHORT).show()
                                editingSubId = null
                            }
                        ) { Text("Save") }
                    },
                    dismissButton = { TextButton(onClick = { editingSubId = null }) { Text("Cancel") } }
                )

                if (openSubWheel) {
                    DateTimeWheelDialog(
                        initial = editingSubDue,
                        onConfirm = { picked -> editingSubDue = picked; openSubWheel = false },
                        onDismiss = { openSubWheel = false },
                        min = LocalDateTime.now().withSecond(0).withNano(0),
                        max = plan.dueAt
                    )
                }
            }

            // Delete assessment dialog
            if (showDelete) {
                AlertDialog(
                    onDismissRequest = { showDelete = false },
                    title = { Text("Delete assessment") },
                    text = { Text("Remove \"${plan.title}\" from your plan? This cannot be undone.") },
                    confirmButton = {
                        TextButton(onClick = {
                            vm.deleteAssessment(plan.id)
                            showDelete = false
                            Toast.makeText(context, "Deleted ${plan.title}", Toast.LENGTH_SHORT).show()
                        }) { Text("Delete") }
                    },
                    dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
                )
            }

            // Delete subtask dialog
            if (deletingSubId != null) {
                AlertDialog(
                    onDismissRequest = { deletingSubId = null },
                    title = { Text("Delete subtask") },
                    text = { Text("Remove \"$deletingSubName\" from ${plan.title}? This cannot be undone.") },
                    confirmButton = {
                        TextButton(onClick = {
                            vm.deleteSubtask(plan.id, deletingSubId!!)
                            deletingSubId = null
                            Toast.makeText(context, "Subtask deleted", Toast.LENGTH_SHORT).show()
                        }) { Text("Delete") }
                    },
                    dismissButton = { TextButton(onClick = { deletingSubId = null }) { Text("Cancel") } }
                )
            }

            // Decompose dialog
            if (showDecompose) {
                var name by remember { mutableStateOf("") }
                var dueAt by remember { mutableStateOf(minOf(defaultNextHour(), plan.dueAt)) }
                var openWheel by remember { mutableStateOf(false) }
                val canAdd = name.isNotBlank() && dueAt <= plan.dueAt

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
                            Text("Due at: ${dueAt.format(fmt)}", style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = { openWheel = true }) { Text("Pick subtask due date") }
                            Text(
                                "Tip: subtask due should not exceed the assessment due date.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = canAdd,
                            onClick = {
                                vm.addSubtask(plan.id, name.trim(), dueAt)
                                name = ""
                                dueAt = minOf(defaultNextHour(), plan.dueAt)
                                Toast.makeText(context, "Added subtask to ${plan.title}", Toast.LENGTH_SHORT).show()
                            }
                        ) { Text("Add") }
                    },
                    dismissButton = { TextButton(onClick = { showDecompose = false }) { Text("Done") } }
                )

                if (openWheel) {
                    DateTimeWheelDialog(
                        initial = dueAt,
                        onConfirm = { picked -> dueAt = picked; openWheel = false },
                        onDismiss = { openWheel = false },
                        min = LocalDateTime.now().withSecond(0).withNano(0),
                        max = plan.dueAt
                    )
                }
            }

            // NEW: Remark dialog (DB-backed)
            if (showRemark) {
                // start from DB value
                var tmp by remember(plan.remark) { mutableStateOf(plan.remark) }

                AlertDialog(
                    onDismissRequest = { showRemark = false },
                    title = { Text("Remark for ${plan.title}") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = tmp,
                                onValueChange = { tmp = it },
                                label = { Text("Write a note / remark") },
                                minLines = 3
                            )
                            Text(
                                "Tip: record progress, lecturer comments, or exam scope.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            vm.updateRemark(plan.id, tmp)       // 🔥 persist to Room
                            Toast.makeText(context, "Remark saved", Toast.LENGTH_SHORT).show()
                            showRemark = false
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRemark = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }

    // Add Assessment dialog
    if (showAdd) {
        val context = LocalContext.current
        var title by remember { mutableStateOf("Assessment ${items.size + 1}") }
        var dueAt by remember { mutableStateOf(defaultNextHour()) }
        var openWheel by remember { mutableStateOf(false) }
        val canAdd = title.isNotBlank()
        val fmtLocal = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }

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
                    Text("Due at: ${dueAt.format(fmtLocal)}", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { openWheel = true }) { Text("Pick due date") }
                    Text("Note: saved to local database.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = canAdd,
                    onClick = {
                        vm.addAssessment(title.trim(), dueAt)
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

/* ===================== Helpers (same as before) ===================== */

private fun defaultNextHour(): LocalDateTime {
    val now = LocalDateTime.now().withSecond(0).withNano(0)
    return if (now.minute == 0) now.plusHours(1) else now.withMinute(0).plusHours(1)
}

@Composable
private fun DateTimeWheelDialog(
    initial: LocalDateTime,
    onConfirm: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit,
    min: LocalDateTime? = null,
    max: LocalDateTime? = null
) {
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableIntStateOf(initial.monthValue) }
    var day by remember { mutableIntStateOf(initial.dayOfMonth) }
    var hour by remember { mutableIntStateOf(initial.hour) }
    var minute by remember { mutableIntStateOf(initial.minute) }

    val defaultMin = remember { LocalDateTime.now().withSecond(0).withNano(0) }
    val minDT = min ?: defaultMin
    val maxDT = max ?: minDT.plusYears(1)

    val yearMin = minDT.year
    val yearMax = maxDT.year
    year = year.coerceIn(yearMin, yearMax)

    val monthMin = if (year == yearMin) minDT.monthValue else 1
    val monthMax = if (year == yearMax) maxDT.monthValue else 12
    month = month.coerceIn(monthMin, monthMax)

    val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
    val dayMin = if (year == yearMin && month == minDT.monthValue) minDT.dayOfMonth else 1
    val dayMax = if (year == yearMax && month == maxDT.monthValue) minOf(daysInMonth, maxDT.dayOfMonth) else daysInMonth
    day = day.coerceIn(dayMin, dayMax)

    val hourMin = if (year == yearMin && month == minDT.monthValue && day == minDT.dayOfMonth) minDT.hour else 0
    val hourMax = if (year == yearMax && month == maxDT.monthValue && day == maxDT.dayOfMonth) maxDT.hour else 23
    hour = hour.coerceIn(hourMin, hourMax)

    val minuteMin =
        if (year == yearMin && month == minDT.monthValue && day == minDT.dayOfMonth && hour == minDT.hour) minDT.minute else 0
    val minuteMax =
        if (year == yearMax && month == maxDT.monthValue && day == maxDT.dayOfMonth && hour == maxDT.hour) maxDT.minute else 59
    minute = minute.coerceIn(minuteMin, minuteMax)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Date & Time") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberWheel("Year", year, yearMin..yearMax, { year = it }, Modifier.weight(1f))
                    NumberWheel("Month", month, monthMin..monthMax, { month = it }, Modifier.weight(1f))
                    NumberWheel("Day", day, dayMin..dayMax, { day = it }, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberWheel("Hour", hour, hourMin..hourMax, { hour = it }, Modifier.weight(1f))
                    NumberWheel("Minute", minute, minuteMin..minuteMax, { minute = it }, Modifier.weight(1f))
                }
                Text(
                    "Range: ${minDT.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))} → ${maxDT.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val picked = LocalDateTime.of(year, month, day, hour, minute)
                val clamped = when {
                    picked.isBefore(minDT) -> minDT
                    picked.isAfter(maxDT) -> maxDT
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
            modifier = Modifier
                .height(120.dp)
                .fillMaxWidth(),
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
