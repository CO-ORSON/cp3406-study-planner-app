package com.example.studyplanner.data.plan

import java.time.LocalDateTime

class PlanRepository(private val dao: PlanDao) {
    val items = dao.observeAssessments()

    suspend fun addAssessment(title: String, dueAt: LocalDateTime): Long =
        dao.insertAssessment(AssessmentEntity(title = title, dueAt = dueAt))

    suspend fun updateAssessment(id: Long, title: String, dueAt: LocalDateTime) =
        dao.updateAssessment(AssessmentEntity(id = id, title = title, dueAt = dueAt))

    suspend fun deleteAssessment(id: Long) = dao.deleteAssessment(id)

    suspend fun addSubtask(assessmentId: Long, name: String, dueAt: LocalDateTime) =
        dao.insertSubtask(SubtaskEntity(assessmentId = assessmentId, name = name, dueAt = dueAt))

    suspend fun countAssessments(): Int = dao.countAssessments()

    // Pass-throughs to DAO (add inside PlanRepository)
    suspend fun updateSubtask(
        assessmentId: Long,            // kept for symmetry; not required by DAO
        subtaskId: Long,
        name: String,
        dueAt: LocalDateTime
    ) {
        dao.updateSubtask(subtaskId, name, dueAt)
    }

    suspend fun deleteSubtask(
        assessmentId: Long,            // kept for symmetry; not required by DAO
        subtaskId: Long
    ) {
        dao.deleteSubtask(subtaskId)
    }

}
