package com.example.studyplanner.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun PlanScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Plan", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Add courses and assessments, then decompose into study sessions.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        items(3) { idx ->
            val context = LocalContext.current
            val dueWeek = idx + 3
            var showDecompose by remember { mutableStateOf(false) }

            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Assessment ${idx + 1}", style = MaterialTheme.typography.titleMedium)
                    Text("Due: Week $dueWeek Friday 23:59")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showDecompose = true }) { Text("Decompose Tasks") }
                        OutlinedButton(onClick = { /* TODO: Auto-schedule */ }) { Text("Auto-schedule") }
                    }
                }
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
                                    "Added: \"$name\" (${hours}h) for Assessment ${idx + 1}",
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
        }
    }
}
