package com.example.studyplanner.data.plan

import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

class PlanRepository(private val dao: PlanDao) {

    val items: Flow<List<AssessmentWithSubtasks>> = dao.observeAssessments()

    suspend fun addAssessment(title: String, dueAt: LocalDateTime): Long =
        dao.insertAssessment(AssessmentEntity(title = title, dueAt = dueAt))

    // Do NOT pass a partial entity to @Update; use the targeted query instead
    suspend fun updateAssessment(id: Long, title: String, dueAt: LocalDateTime) =
        dao.updateAssessmentTitleAndDueAt(id, title, dueAt)

    // If you don't have FK ON DELETE CASCADE, also remove subtasks
    suspend fun deleteAssessment(id: Long) {
        dao.deleteSubtasksFor(id) // delete children first to avoid FK issues
        dao.deleteAssessment(id)
    }

    suspend fun addSubtask(assessmentId: Long, name: String, dueAt: LocalDateTime) =
        dao.insertSubtask(SubtaskEntity(assessmentId = assessmentId, name = name, dueAt = dueAt))

    suspend fun countAssessments(): Int = dao.countAssessments()

    suspend fun updateSubtask(
        assessmentId: Long,
        subtaskId: Long,
        name: String,
        dueAt: LocalDateTime
    ) = dao.updateSubtask(subtaskId, name, dueAt)

    suspend fun deleteSubtask(assessmentId: Long, subtaskId: Long) =
        dao.deleteSubtask(subtaskId)

    suspend fun updateRemark(assessmentId: Long, remark: String) =
        dao.updateAssessmentRemark(assessmentId, remark)
}
