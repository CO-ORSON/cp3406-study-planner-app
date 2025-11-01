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

// ------------- NEW: UI-only calendar model -------------
private data class CalendarMarkUi(
    val id: Long,
    val title: String,
    val isAssessment: Boolean,
    val start: LocalDateTime
)

@Composable
fun CalendarScreen(vm: PlanViewModel = viewModel()) {
    val cs = MaterialTheme.colorScheme
    val today = LocalDate.now()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // 1) this is your real VM data: List<AssessmentUi>
    val assessments by vm.items.collectAsStateWithLifecycle()

    // 2) flatten Assessment + its subtasks => calendar marks
    val marks = remember(assessments) {
        assessments.flatMap { a ->
            buildList {
                // assessment itself
                add(
                    CalendarMarkUi(
                        id = a.id,
                        title = a.title,
                        isAssessment = true,
                        start = a.dueAt
                    )
                )
                // its subtasks
                a.subtasks.forEach { s ->
                    add(
                        CalendarMarkUi(
                            id = s.id,
                            title = s.name,
                            isAssessment = false,
                            start = s.dueAt
                        )
                    )
                }
            }
        }
    }

    // 3) per-day markers
    val assignmentsByDate = remember(marks) {
        marks
            .filter { it.isAssessment }
            .groupBy { it.start.toLocalDate() }
    }
    val subtasksByDate = remember(marks) {
        marks
            .filter { !it.isAssessment }
            .groupBy { it.start.toLocalDate() }
    }

    val monthTitleFmt = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }

    // 4) month list
    val monthStart = currentMonth.atDay(1).atStartOfDay()
    val monthEnd = currentMonth.atEndOfMonth().atTime(23, 59)
    val monthList = remember(marks, currentMonth) {
        marks
            .filter { it.start in monthStart..monthEnd }
            .sortedBy { it.start }
    }

    // 5) counters
    val assignmentMarksThisMonth = remember(monthList) {
        monthList.count { it.isAssessment }
    }
    val subtaskMarksThisMonth = remember(monthList) {
        monthList.size - assignmentMarksThisMonth
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
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
                    "$assignmentMarksThisMonth assignment(s) • $subtaskMarksThisMonth subtask(s)",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Weekday header
        item {
            val weekdays = listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
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

        // Month grid
        item {
            val firstOfMonth = currentMonth.atDay(1)
            val daysInMonth = currentMonth.lengthOfMonth()
            val leadingBlanks = ((firstOfMonth.dayOfWeek.value + 6) % 7)
            val totalCells = leadingBlanks + daysInMonth
            val rows = ceil(totalCells / 7.0).toInt()

            Column(Modifier.fillMaxWidth()) {
                for (r in 0 until rows) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (c in 0 until 7) {
                            val idx = r * 7 + c
                            val dayNum = idx - leadingBlanks + 1
                            if (dayNum in 1..daysInMonth) {
                                val date = currentMonth.atDay(dayNum)
                                val isToday = date == today
                                val hasAssignment = assignmentsByDate.containsKey(date)
                                val hasSubtasks = subtasksByDate.containsKey(date)

                                Box(
                                    Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                ) {
                                    DayCell(
                                        date = date,
                                        isToday = isToday,
                                        hasAssignment = hasAssignment,
                                        hasSubtasks = hasSubtasks
                                    )
                                }
                            } else {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // This month list
        if (monthList.isNotEmpty()) {
            item {
                Text("This month", style = MaterialTheme.typography.titleMedium)
            }

            items(monthList) { entry ->
                val bg = if (entry.isAssessment) cs.errorContainer else cs.secondaryContainer
                val fg = if (entry.isAssessment) cs.onErrorContainer else cs.onSecondaryContainer
                val timeFmt = remember { DateTimeFormatter.ofPattern("EEE, dd MMM HH:mm") }

                Surface(
                    color = bg,
                    contentColor = fg,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = if (entry.isAssessment) 4.dp else 0.dp
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                entry.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "DUE: ${entry.start.format(timeFmt)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
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
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )

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
