package com.example.studyplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@Composable
fun CalendarScreen() {
    val cs = MaterialTheme.colorScheme
    val today = LocalDate.now()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // pull events from shared store
    val all = SharedCalendar.items

    // separate assignment-due vs subtask-due
    val assignmentsByDate = remember(all.size) {
        all.filter { it.subtaskName == "Assessment due" }
            .groupBy { it.start.toLocalDate() }
    }
    val subtasksByDate = remember(all.size) {
        all.filter { it.subtaskName != "Assessment due" }
            .groupBy { it.start.toLocalDate() }
    }

    val monthTitleFmt = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Header: month nav + today button
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

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(onClick = { currentMonth = YearMonth.now() }, label = { Text("Today") })
            val monthEventsCount = remember(all.size, currentMonth) {
                all.count { it.start.toLocalDate().withDayOfMonth(1) == currentMonth.atDay(1) }
            }
            Text("$monthEventsCount item(s)", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(12.dp))

        // Weekday header (Mon..Sun)
        val weekdays = listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        )
        Row(Modifier.fillMaxWidth()) {
            // use a plain 'for' loop so RowScope.weight() remains available
            for (dow in weekdays) {
                Box(
                    Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        dow.name.take(3), // Mon, Tue, ...
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Build month grid
        val firstOfMonth = currentMonth.atDay(1)
        val daysInMonth = currentMonth.lengthOfMonth()
        val leadingBlanks = ((firstOfMonth.dayOfWeek.value + 6) % 7) // Mon=1 -> 0 blanks; Sun=7 -> 6 blanks
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

                            // weight at call site (Option A)
                            Box(Modifier.weight(1f).aspectRatio(1f)) {
                                DayCell(
                                    date = date,
                                    isToday = isToday,
                                    hasAssignment = hasAssignment,
                                    hasSubtasks = hasSubtasks
                                )
                            }
                        } else {
                            // empty cell keeps the grid aligned
                            Box(Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        // Optional: list entries for the month below, ordered
        val timeFmt = remember { DateTimeFormatter.ofPattern("EEE, dd MMM • HH:mm") }
        val monthStart = currentMonth.atDay(1).atStartOfDay()
        val monthEnd = currentMonth.atEndOfMonth().atTime(23, 59)
        val monthList = remember(all.size, currentMonth) {
            all.filter { it.start.isAfter(monthStart.minusSeconds(1)) && it.start.isBefore(monthEnd.plusSeconds(1)) }
                .sortedBy { it.start }
        }

        if (monthList.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("This month", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                monthList.forEach { e ->
                    val isAssessment = e.subtaskName == "Assessment due"
                    val bg = if (isAssessment) cs.errorContainer else cs.secondaryContainer
                    val fg = if (isAssessment) cs.onErrorContainer else cs.onSecondaryContainer
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
                                Text(e.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(e.start.format(timeFmt), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        } else {
            Text("No calendar items this month.", style = MaterialTheme.typography.bodyMedium)
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
            .fillMaxSize() // <-- moved weight/aspectRatio to the call site
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

        // Markers at bottom: assignment is more salient than subtasks
        Column(
            Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasAssignment) {
                // Wide pill (salient)
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
                // Small dot (subtle)
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
