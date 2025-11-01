package com.example.studyplanner.data.plan

import androidx.room.*
import java.time.LocalDateTime

@Entity(tableName = "assessments")
data class AssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueAt: LocalDateTime,
    @ColumnInfo(defaultValue = "") val remark: String = ""
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [ForeignKey(
        entity = AssessmentEntity::class,
        parentColumns = ["id"],
        childColumns = ["assessmentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("assessmentId")]
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assessmentId: Long,
    val name: String,
    val dueAt: LocalDateTime
)

data class AssessmentWithSubtasks(
    @Embedded val assessment: AssessmentEntity,
    @Relation(parentColumn = "id", entityColumn = "assessmentId")
    val subtasks: List<SubtaskEntity>
)
