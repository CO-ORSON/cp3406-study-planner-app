package com.example.studyplanner.ui.screens

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.max

@SuppressLint("DefaultLocale")
@Composable
fun FocusScreen() {
    val context = LocalContext.current
    val notificationManager =
        remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    var running by rememberSaveable { mutableStateOf(false) }
    val totalSeconds = 45 * 60
    var remaining by rememberSaveable { mutableStateOf(totalSeconds) }

    var dndOn by rememberSaveable { mutableStateOf(false) }

    // Simple countdown tick (1s)
    LaunchedEffect(running) {
        while (running && remaining > 0) {
            delay(1_000)
            remaining = max(0, remaining - 1)
        }
        if (running && remaining == 0) {
            running = false
            Toast.makeText(context, "Session complete!", Toast.LENGTH_SHORT).show()
        }
    }

    val mm = remaining / 60
    val ss = remaining % 60
    val progress = 1f - (remaining.toFloat() / totalSeconds.toFloat())

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Focus", style = MaterialTheme.typography.headlineSmall)
        Text("Next session: CP3406 — Part A Draft")

        Text(String.format("%02d:%02d", mm, ss), style = MaterialTheme.typography.displaySmall)

        LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth(),
        color = ProgressIndicatorDefaults.linearColor,
        trackColor = ProgressIndicatorDefaults.linearTrackColor,
        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!running) {
                Button(
                    onClick = {
                        if (remaining == 0) remaining = totalSeconds
                        running = true
                    }
                ) { Text("Start") }
            } else {
                OutlinedButton(onClick = { running = false }) { Text("Stop") }
            }

            OutlinedButton(
                onClick = {
                    running = false
                    remaining = totalSeconds
                }
            ) { Text("Reset") }

            AssistChip(
                onClick = {
                    val hasAccess = notificationManager.isNotificationPolicyAccessGranted
                    if (!hasAccess) {
                        // Open DND access settings
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        Toast.makeText(
                            context,
                            "Grant 'Do Not Disturb' access to enable toggle.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Toggle DND
                        dndOn = !dndOn
                        val mode = if (dndOn)
                            NotificationManager.INTERRUPTION_FILTER_NONE
                        else
                            NotificationManager.INTERRUPTION_FILTER_ALL
                        try {
                            notificationManager.setInterruptionFilter(mode)
                            Toast.makeText(
                                context,
                                if (dndOn) "DND enabled" else "DND disabled",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (_: SecurityException) {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                },
                label = { Text(if (dndOn) "Disable DND" else "Enable DND") }
            )
        }

        Text("Tip: Keep it simple—45 minutes focus + 10–15 minutes break.")
    }
}
