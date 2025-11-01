package com.example.studyplanner.ui.screens

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.NumberPicker
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlin.math.max

@SuppressLint("DefaultLocale")
@Composable
fun FocusScreen() {
    val context = LocalContext.current
    val notificationManager =
        remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    // --- Custom time (wheel) 5..179 minutes (<180) ---
    var selectedMinutes by rememberSaveable { mutableStateOf(45) }

    // --- Timer state ---
    var running by rememberSaveable { mutableStateOf(false) }
    var remaining by rememberSaveable { mutableStateOf(selectedMinutes * 60) }

    // Keep remaining in sync when length changes (only if idle/paused)
    LaunchedEffect(selectedMinutes) {
        if (!running) remaining = max(1, selectedMinutes * 60)
    }

    // 1-second tick
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

    val totalSeconds = max(1, selectedMinutes * 60)
    val paused = !running && remaining in 1 until totalSeconds

    val mm = remaining / 60
    val ss = remaining % 60
    val progress = 1f - (remaining.toFloat() / totalSeconds.toFloat())

    // --- DND toggle ---
    var dndOn by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Focus", style = MaterialTheme.typography.headlineSmall)

        // --- Minutes wheel (NumberPicker) ---
        AndroidView<NumberPicker>(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            factory = { ctx: Context ->
                NumberPicker(ctx).apply {
                    minValue = 5
                    maxValue = 179 // strictly < 180
                    value = selectedMinutes
                    wrapSelectorWheel = false
                    isEnabled = !running
                    setOnValueChangedListener { _, _, newVal ->
                        selectedMinutes = newVal
                    }
                }
            },
            update = { picker ->
                if (picker.value != selectedMinutes) picker.value = selectedMinutes
                picker.isEnabled = !running
            }
        )

        // --- Timer display ---
        Text(String.format("%02d:%02d", mm, ss), style = MaterialTheme.typography.displaySmall)

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )

        // --- Controls (DND button pushed to the right) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!running) {
                Button(
                    onClick = {
                        if (remaining == 0) remaining = totalSeconds
                        running = true
                    }
                ) { Text(if (paused) "Resume" else "Start") }
            } else {
                OutlinedButton(onClick = { running = false }) { Text("Pause") }
            }

            OutlinedButton(
                onClick = {
                    running = false
                    remaining = totalSeconds
                }
            ) { Text("Reset") }

            // Push the DND chip to the far right
            Spacer(modifier = Modifier.weight(1f))

            AssistChip(
                onClick = {
                    val hasAccess = notificationManager.isNotificationPolicyAccessGranted
                    if (!hasAccess) {
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
    }
}
