package com.example.studyplanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FocusScreen() {
    var running by remember { mutableStateOf(false) }
    var minutesLeft by remember { mutableStateOf(45) } // dummy

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Focus", style = MaterialTheme.typography.headlineSmall)
        Text("Next session: CP3406 — Part A Draft")
        Text(
            "$minutesLeft : 00",
            style = MaterialTheme.typography.displaySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!running) {
                Button(onClick = { running = true }) { Text("Start") }
            } else {
                OutlinedButton(onClick = { running = false }) { Text("Stop") }
            }
            AssistChip(onClick = { /* TODO: Toggle DND */ }, label = { Text("Enable DND") })
        }
        Text("Tip: Keep it simple—45 minutes focus + 10–15 minutes break.")
    }
}
