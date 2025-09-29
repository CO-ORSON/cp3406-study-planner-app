package com.example.studyplanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlanScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
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
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Assessment ${idx + 1}", style = MaterialTheme.typography.titleMedium)
                    Text("Due: Week ${idx + 3} Friday 23:59")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { /* TODO: Decompose */ }) { Text("Decompose Tasks") }
                        OutlinedButton(onClick = { /* TODO: Auto-schedule */ }) { Text("Auto-schedule") }
                    }
                }
            }
        }
    }
}
