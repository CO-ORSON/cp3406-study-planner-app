package com.example.studyplanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CalendarScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Calendar", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Day/Week views will appear here. For MVP, this is a placeholder.")
        Spacer(Modifier.height(16.dp))
        OutlinedCard {
            Column(Modifier.padding(16.dp)) {
                Text("Tue 7–7:45 pm • Focus: Literature Review")
                Text("Thu 8–8:45 pm • Focus: Prototype UI")
                Text("Sat 10–11 am • Focus: Testing")
            }
        }
    }
}
