package com.example.studyplanner.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studyplanner.ui.plan.PlanViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun InsightsScreen(vm: PlanViewModel = viewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    val now = LocalDateTime.now()
    val today = now.toLocalDate()

    // ---- Flatten marks (same semantics as CalendarScreen) ----
    data class Mark(
        val id: Long,
        val title: String,
        val isAssessment: Boolean,
        val at: LocalDateTime
    )

    val marks = remember(items) {
        items.flatMap { a ->
            buildList {
                add(Mark(a.id, a.title, true, a.dueAt))
                a.subtasks.forEach { s -> add(Mark(s.id, s.name, false, s.dueAt)) }
            }
        }
    }

    // ---- Quick stats ----
    val overdue = remember(marks, now) { marks.filter { it.at.isBefore(now) && it.isAssessment } }
    val dueToday = remember(marks, today) { marks.count { it.isAssessment && it.at.toLocalDate() == today } }
    val dueThisWeek = remember(marks, today) {
        val end = today.plusDays(6)
        marks.count { it.isAssessment && it.at.toLocalDate() in today..end }
    }

    val upcoming = remember(marks, now) {
        marks.filter { it.at.isAfter(now) && it.isAssessment }.sortedBy { it.at }.take(3)
    }
    val upcomingSubtasks = remember(marks, now) {
        marks.filter { it.at.isAfter(now) && !it.isAssessment }.sortedBy { it.at }.take(4)
    }

    // ---- 14-day workload spark bars (assignments vs subtasks) ----
    data class DayLoad(val date: LocalDate, val assign: Int, val subs: Int)
    val next14 = remember(marks, today) {
        (0..13).map { d ->
            val date = today.plusDays(d.toLong())
            val a = marks.count { it.isAssessment && it.at.toLocalDate() == date }
            val s = marks.count { !it.isAssessment && it.at.toLocalDate() == date }
            DayLoad(date, a, s)
        }
    }
    val maxLoad = remember(next14) { max(1, next14.maxOfOrNull { it.assign + it.subs } ?: 1) }

    // ---- Recommended focus length (simple heuristic based on nearest assessment) ----
    val nearest = remember(upcoming) { upcoming.firstOrNull() }
    val recommendedMinutes = remember(nearest, now) {
        if (nearest == null) 30
        else {
            val daysLeft = ChronoUnit.DAYS.between(now.toLocalDate().atStartOfDay(), nearest.at.toLocalDate().atStartOfDay()).toInt()
            when {
                daysLeft <= 2 -> 60
                daysLeft <= 6 -> 45
                else -> 30
            }
        }
    }

    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, dd MMM HH:mm") }
    val dayLabelFmt = remember { DateTimeFormatter.ofPattern("E") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Column(Modifier.fillMaxWidth()) {
                Text("Insights", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (items.isEmpty()) "Add your first assessment to unlock insights." else "Plan smarter with a snapshot of deadlines and workload.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )
            }
        }

        // Stat chips row
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(
                    label = "Overdue",
                    value = overdue.size.toString(),
                    container = cs.errorContainer,
                    content = cs.onErrorContainer
                )
                StatChip(
                    label = "Due today",
                    value = dueToday.toString(),
                    container = cs.secondaryContainer,
                    content = cs.onSecondaryContainer
                )
                StatChip(
                    label = "Next 7 days",
                    value = dueThisWeek.toString(),
                    container = cs.primaryContainer,
                    content = cs.onPrimaryContainer
                )
            }
        }

        // Workload next 14 days (stacked micro-bars)
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Upcoming workload (14 days)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    if (next14.all { it.assign + it.subs == 0 }) {
                        Text("No scheduled items yet.", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    } else {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            next14.forEach { d ->
                                StackedBar(
                                    assignments = d.assign,
                                    subtasks = d.subs,
                                    maxCount = maxLoad,
                                    height = 64.dp,
                                    assignmentColor = cs.error,
                                    subtaskColor = cs.secondary
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        // Day labels below (show every other to reduce clutter)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            next14.forEachIndexed { idx, d ->
                                val lbl = if (idx % 2 == 0) d.date.format(dayLabelFmt) else ""
                                Text(lbl, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LegendDot(cs.error); Text("Assessments", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.width(12.dp))
                            LegendDot(cs.secondary); Text("Subtasks", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Nearest deadlines
        if (upcoming.isNotEmpty()) {
            item { Text("Next deadlines", style = MaterialTheme.typography.titleMedium) }
            items(upcoming) { m ->
                Surface(
                    color = cs.errorContainer,
                    contentColor = cs.onErrorContainer,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(m.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("DUE: ${m.at.format(dateFmt)}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // Soonest subtasks
        if (upcomingSubtasks.isNotEmpty()) {
            item { Text("Upcoming subtasks", style = MaterialTheme.typography.titleMedium) }
            items(upcomingSubtasks) { m ->
                Surface(
                    color = cs.secondaryContainer,
                    contentColor = cs.onSecondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(m.title, style = MaterialTheme.typography.bodyLarge)
                            Text(m.at.format(dateFmt), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // Focus helper card
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Focus helper", style = MaterialTheme.typography.titleMedium)
                    if (nearest == null) {
                        Text("No upcoming assessments. Try a 25–30 min session to plan ahead.", color = cs.onSurfaceVariant)
                    } else {
                        val daysLeft = ChronoUnit.DAYS.between(today, nearest.at.toLocalDate()).toInt().coerceAtLeast(0)
                        Text("Nearest: \"${nearest.title}\" in ${daysLeft} day(s)")
                        Text("Suggested next session: ${recommendedMinutes} min", fontWeight = FontWeight.SemiBold)
                        LinearProgressIndicator(
                            progress = { (1f - (daysLeft / 14f)).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Tip: increase session length as deadlines approach.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                }
            }
        }

        // Overdue list (if any)
        if (overdue.isNotEmpty()) {
            item { Text("Overdue", style = MaterialTheme.typography.titleMedium, color = cs.error) }
            items(overdue.take(4)) { m ->
                Surface(
                    color = cs.errorContainer,
                    contentColor = cs.onErrorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(m.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Was due ${m.at.format(dateFmt)}", style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(onClick = { /* optional: jump to Plan/Calendar via your nav */ }) { Text("Review") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    container: Color,
    content: Color
) {
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun StackedBar(
    assignments: Int,
    subtasks: Int,
    maxCount: Int,
    height: Dp,
    assignmentColor: Color,
    subtaskColor: Color
) {
    val total = assignments + subtasks
    val h = height
    val barWidth = 14.dp

    Box(Modifier.width(barWidth).height(h)) {
        if (total == 0) {
            // faint baseline
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .width(barWidth)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            )
        } else {
            val frac = total.toFloat() / max(1, maxCount).toFloat()
            val barH = (h * frac)
            // Subtasks segment (bottom)
            val subsFrac = if (total == 0) 0f else subtasks.toFloat() / total.toFloat()
            val subsH = barH * subsFrac
            // Assignments segment (top)
            val assignH = barH - subsH

            Column(Modifier.align(Alignment.BottomCenter)) {
                Box(
                    Modifier
                        .width(barWidth)
                        .height(subsH)
                        .background(subtaskColor, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )
                Box(
                    Modifier
                        .width(barWidth)
                        .height(assignH)
                        .background(assignmentColor, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        Modifier
            .size(10.dp)
            .background(color, RoundedCornerShape(50))
    )
}
