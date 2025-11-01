package com.example.studyplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studyplanner.ui.plan.PlanViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@Composable
fun CalendarScreen(vm: PlanViewModel = viewModel()) {
    val cs = MaterialTheme.colorScheme
    val today = LocalDate.now()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // DB items (kept; may be used elsewhere)
    val items by vm.items.collectAsStateWithLifecycle()

    // In-memory calendar marks created by PlanScreen ("Add to calendar")
    val all = SharedCalendar.items

    // Precompute markers for the month grid (from calendar marks only)
    val assignmentsByDate = remember(all.size) {
        all.filter { it.subtaskName == "Assessment due" }
            .groupBy { it.start.toLocalDate() }
    }
    val subtasksByDate = remember(all.size) {
        all.filter { it.subtaskName != "Assessment due" }
            .groupBy { it.start.toLocalDate() }
    }

    val monthTitleFmt = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }

    // Month list (marks) for the visible month
    val monthStart = currentMonth.atDay(1).atStartOfDay()
    val monthEnd = currentMonth.atEndOfMonth().atTime(23, 59)
    val monthList = remember(all.size, currentMonth) {
        all.filter { it.start in monthStart..monthEnd }.sortedBy { it.start }
    }

    // NEW: Counters split into assignments vs subtasks (from calendar marks)
    val assignmentMarksThisMonth = remember(monthList) {
        monthList.count { it.subtaskName == "Assessment due" }
    }
    val subtaskMarksThisMonth = remember(monthList) {
        monthList.size - assignmentMarksThisMonth
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header: month nav + title
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                }
                Text(
                    text = currentMonth.atDay(1).format(monthTitleFmt),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
                }
            }
        }

        // Today + counters
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(onClick = { currentMonth = YearMonth.now() }, label = { Text("Today") })
                Text(
                    // CHANGED: from "x mark(s) • y assignment(s)" to "x assignment(s) • y subtask(s)"
                    "$assignmentMarksThisMonth assignment(s) • $subtaskMarksThisMonth subtask(s)",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Weekday header (Mon..Sun)
        item {
            val weekdays = listOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
            )
            Row(Modifier.fillMaxWidth()) {
                for (dow in weekdays) {
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            dow.name.take(3),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Month grid (kept as before, now it's just one scroll item)
        item {
            val firstOfMonth = currentMonth.atDay(1)
            val daysInMonth = currentMonth.lengthOfMonth()
            val leadingBlanks = ((firstOfMonth.dayOfWeek.value + 6) % 7)
            val totalCells = leadingBlanks + daysInMonth
            val rows = ceil(totalCells / 7.0).toInt()

            Column(Modifier.fillMaxWidth()) {
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (c in 0 until 7) {
                            val idx = r * 7 + c
                            val dayNum = idx - leadingBlanks + 1
                            if (dayNum in 1..daysInMonth) {
                                val date = currentMonth.atDay(dayNum)
                                val isToday = date == today
                                val hasAssignment = assignmentsByDate.containsKey(date)
                                val hasSubtasks = subtasksByDate.containsKey(date)

                                Box(Modifier.weight(1f).aspectRatio(1f)) {
                                    DayCell(
                                        date = date,
                                        isToday = isToday,
                                        hasAssignment = hasAssignment,
                                        hasSubtasks = hasSubtasks
                                    )
                                }
                            } else {
                                Box(Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // "This month" section title or empty text
        if (monthList.isNotEmpty()) {
            item {
                Text("This month", style = MaterialTheme.typography.titleMedium)
            }

            // Monthly list with Remove action (scrolls as part of LazyColumn)
            items(monthList) { entry ->
                val isAssessment = entry.subtaskName == "Assessment due"
                val bg = if (isAssessment) cs.errorContainer else cs.secondaryContainer
                val fg = if (isAssessment) cs.onErrorContainer else cs.onSecondaryContainer
                val timeFmt = remember { DateTimeFormatter.ofPattern("EEE, dd MMM HH:mm") }

                // Extract base title (remove trailing "— DUE" or " - DUE")
                val baseTitle = remember(entry.title) {
                    entry.title.substringBefore(" —").substringBefore(" - ").trim()
                }

                Surface(
                    color = bg,
                    contentColor = fg,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = if (isAssessment) 4.dp else 0.dp
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(baseTitle, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("DUE: ${entry.start.format(timeFmt)}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.width(12.dp))
                        TextButton(onClick = { SharedCalendar.items.remove(entry) }) {
                            Text("Remove")
                        }
                    }
                }
            }
        } else {
            item {
                Text("No calendar items this month.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    hasAssignment: Boolean,
    hasSubtasks: Boolean
) {
    val cs = MaterialTheme.colorScheme
    val outline = if (isToday) cs.primary else cs.outlineVariant

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isToday) 2.dp else 1.dp,
                color = outline,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(6.dp)
    ) {
        // Day number
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )

        // Markers at bottom
        Column(
            Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasAssignment) {
                Box(
                    Modifier
                        .fillMaxWidth(0.75f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.error)
                )
                if (hasSubtasks) Spacer(Modifier.height(2.dp))
            }
            if (hasSubtasks) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
        }
    }
}
