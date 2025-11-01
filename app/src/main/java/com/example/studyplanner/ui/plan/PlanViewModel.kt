package com.example.studyplanner.ui.plan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplanner.data.plan.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class PlanViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PlanRepository(PlanDb.get(app).dao())

    data class SubtaskUi(val id: Long, val name: String, val dueAt: LocalDateTime)
    data class AssessmentUi(val id: Long, val title: String, val dueAt: LocalDateTime, val subtasks: List<SubtaskUi>)

    val items: StateFlow<List<AssessmentUi>> =
        repo.items.map { list ->
            list.map { aw ->
                AssessmentUi(
                    id = aw.assessment.id,
                    title = aw.assessment.title,
                    dueAt = aw.assessment.dueAt,
                    subtasks = aw.subtasks.map { SubtaskUi(it.id, it.name, it.dueAt) }
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addAssessment(title: String, dueAt: LocalDateTime) =
        viewModelScope.launch { repo.addAssessment(title, dueAt) }

    fun updateAssessment(id: Long, title: String, dueAt: LocalDateTime) =
        viewModelScope.launch { repo.updateAssessment(id, title, dueAt) }

    fun deleteAssessment(id: Long) =
        viewModelScope.launch { repo.deleteAssessment(id) }

    fun addSubtask(assessmentId: Long, name: String, dueAt: LocalDateTime) =
        viewModelScope.launch { repo.addSubtask(assessmentId, name, dueAt) }

    fun seedIfEmpty(defaultNextHour: () -> LocalDateTime) = viewModelScope.launch {
        if (repo.countAssessments() == 0) {
            repo.addAssessment("Assessment 1", defaultNextHour().plusWeeks(1))
            repo.addAssessment("Assessment 2", defaultNextHour().plusWeeks(2))
            repo.addAssessment("Assessment 3", defaultNextHour().plusWeeks(3))
        }
    }
}
