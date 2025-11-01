package com.example.studyplanner.data.plan

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Transaction
    @Query("SELECT * FROM assessments ORDER BY dueAt")
    fun observeAssessments(): Flow<List<AssessmentWithSubtasks>>

    @Query("SELECT COUNT(*) FROM assessments")
    suspend fun countAssessments(): Int

    @Insert suspend fun insertAssessment(a: AssessmentEntity): Long
    @Update suspend fun updateAssessment(a: AssessmentEntity)
    @Query("DELETE FROM assessments WHERE id = :id") suspend fun deleteAssessment(id: Long)

    @Insert suspend fun insertSubtask(s: SubtaskEntity): Long
    @Query("DELETE FROM subtasks WHERE assessmentId = :assessmentId")
    suspend fun deleteSubtasksFor(assessmentId: Long)
}
