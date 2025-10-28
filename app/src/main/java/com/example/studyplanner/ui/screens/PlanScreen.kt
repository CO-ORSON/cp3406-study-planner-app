package com.example.studyplanner.ui.screens

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

@Composable
fun PlanScreen() {
    // In-memory dynamic list (no persistence)
    val assessments = remember {
        mutableStateListOf(
            Assessment("Assessment 1", 3),
            Assessment("Assessment 2", 4),
            Assessment("Assessment 3", 5)
        )
    }
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
                TextButton(onClick = { showAdd = true }) { Text("Add assessment") }
            }
            Text(
                "Add courses and assessments, then decompose into study sessions.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        items(assessments, key = { it.title }) { plan ->
            val context = LocalContext.current
            val dueWeek = plan.dueWeek
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
                    Text("Due: Week $dueWeek Friday 23:59")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showDecompose = true }) { Text("Decompose Tasks") }
                        OutlinedButton(onClick = { showAuto = true }) { Text("Auto-schedule") }
                    }
                }
            }

            // Edit dialog
            if (showEdit) {
                var title by remember { mutableStateOf(plan.title) }
                var dueText by remember { mutableStateOf(plan.dueWeek.toString()) }
                val due = dueText.toIntOrNull()
                val canSave = title.isNotBlank() && (due ?: 0) in 1..13

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
                            OutlinedTextField(
                                value = dueText,
                                onValueChange = { dueText = it.filter(Char::isDigit) },
                                label = { Text("Due week (1..13)") },
                                singleLine = true
                            )
                            Text("Changes are in-memory only.", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = canSave,
                            onClick = {
                                val idx = assessments.indexOf(plan)
                                if (idx >= 0) {
                                    assessments[idx] = plan.copy(title = title.trim(), dueWeek = due!!)
                                    Toast.makeText(context, "Updated ${assessments[idx].title}", Toast.LENGTH_SHORT).show()
                                }
                                showEdit = false
                            }
                        ) { Text("Save") }
                    },
                    dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Cancel") } }
                )
            }

            // Confirm delete dialog
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
                    dismissButton = {
                        TextButton(onClick = { showDelete = false }) { Text("Cancel") }
                    }
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
                            Text(
                                "Lightweight placeholder — wire to real state later.",
                                style = MaterialTheme.typography.bodySmall
                            )
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
                    dismissButton = {
                        TextButton(onClick = { showDecompose = false }) { Text("Cancel") }
                    }
                )
            }

            // Minimal "Auto-schedule" dialog (no persistence)
            if (showAuto) {
                var startWeekText by remember { mutableStateOf((dueWeek - 2).coerceAtLeast(1).toString()) }
                var totalHoursText by remember { mutableStateOf("6") }
                var perSessionText by remember { mutableStateOf("2") }

                val startWeek = startWeekText.toIntOrNull()
                val totalHours = totalHoursText.toIntOrNull()
                val perSession = perSessionText.toIntOrNull()

                val valid = startWeek != null &&
                        totalHours != null && totalHours > 0 &&
                        perSession != null && perSession > 0 &&
                        startWeek < dueWeek

                AlertDialog(
                    onDismissRequest = { showAuto = false },
                    title = { Text("Auto-schedule (preview)") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = startWeekText,
                                onValueChange = { startWeekText = it.filter(Char::isDigit) },
                                label = { Text("Start week") },
                                singleLine = true
                            )
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
                                "Will spread sessions before Week $dueWeek (no saving yet).",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = valid,
                            onClick = {
                                val st = startWeek ?: return@TextButton
                                val th = totalHours ?: return@TextButton
                                val ps = perSession ?: return@TextButton
                                val sessions = (th + ps - 1) / ps
                                Toast.makeText(
                                    context,
                                    "Generated $sessions sessions for ${plan.title} from Week $st to before Week $dueWeek",
                                    Toast.LENGTH_SHORT
                                ).show()
                                showAuto = false
                            }
                        ) { Text("Generate") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAuto = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }

    // Add Assessment dialog (outside LazyColumn)
    if (showAdd) {
        var title by remember { mutableStateOf("Assessment ${assessments.size + 1}") }
        var dueText by remember { mutableStateOf("6") }
        val due = dueText.toIntOrNull()
        val canAdd = title.isNotBlank() && (due ?: 0) > 0

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
                    OutlinedTextField(
                        value = dueText,
                        onValueChange = { dueText = it.filter(Char::isDigit) },
                        label = { Text("Due week (1..13)") },
                        singleLine = true
                    )
                    Text(
                        "Note: stored in memory only (clears on restart).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = canAdd,
                    onClick = {
                        assessments.add(Assessment(title.trim(), due!!))
                        showAdd = false
                    }
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

// Minimal model for the list (in-memory only)
private data class Assessment(val title: String, val dueWeek: Int)
