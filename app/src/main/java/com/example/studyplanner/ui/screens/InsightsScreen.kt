package com.example.studyplanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class WeeklyStats(val focusedHrs: Int, val completionPct: Int, val burnDownProgress: Float)

@Composable
fun InsightsScreen() {
    val stats = WeeklyStats(focusedHrs = 3, completionPct = 40, burnDownProgress = 0.4f)

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Insights", style = MaterialTheme.typography.headlineSmall)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("This Week", style = MaterialTheme.typography.titleMedium)
                Text("Focused: ${stats.focusedHrs} hrs • Completion: ${stats.completionPct}%")
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { stats.burnDownProgress })
            }
        }

        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Streak: 2 days")
                Text("Average session length: 42 min")
            }
        }
    }
}
