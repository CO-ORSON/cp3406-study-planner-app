package com.example.studyplanner.ui.plan

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplanner.data.plan.PlanDb
import com.example.studyplanner.data.plan.PlanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class PlanViewModel(
    app: Application,
    // In tests, pass a FakeRepo; in prod, we fall back to real repo via defaultRepo(app)
    private val repo: PlanRepositoryLike = defaultRepo(app)
) : AndroidViewModel(app) {

    @Suppress("unused") // called via reflection
    constructor(app: Application) : this(app, defaultRepo(app))

    // ---------- UI models ----------
    data class SubtaskUi(
        val id: Long,
        val name: String,
        val dueAt: LocalDateTime
    )

    data class AssessmentUi(
        val id: Long,
        val title: String,
        val dueAt: LocalDateTime,
        val remark: String,
        val subtasks: List<SubtaskUi>
    )

    // ---------- Lightweight repo-facing projection (stable for tests) ----------
    data class RepoAssessment(
        val id: Long,
        val title: String,
        val dueAt: LocalDateTime,
        val remark: String
    )
    data class RepoSubtask(
        val id: Long,
        val name: String,
        val dueAt: LocalDateTime
    )
    data class RepoAggregate(
        val assessment: RepoAssessment,
        val subtasks: List<RepoSubtask>
    )

    interface PlanRepositoryLike {
        val items: Flow<List<RepoAggregate>>
        suspend fun addAssessment(title: String, dueAt: LocalDateTime): Long
        suspend fun updateAssessment(id: Long, title: String, dueAt: LocalDateTime)
        suspend fun deleteAssessment(id: Long)
        suspend fun addSubtask(assessmentId: Long, name: String, dueAt: LocalDateTime): Long
        suspend fun updateSubtask(assessmentId: Long, subtaskId: Long, name: String, dueAt: LocalDateTime)
        suspend fun deleteSubtask(assessmentId: Long, subtaskId: Long)
        suspend fun updateRemark(assessmentId: Long, remark: String)
    }

    // ---------- VM state ----------
    val items: StateFlow<List<AssessmentUi>> =
        repo.items
            .map { list ->
                list.map { aw ->
                    AssessmentUi(
                        id = aw.assessment.id,
                        title = aw.assessment.title,
                        dueAt = aw.assessment.dueAt,
                        remark = aw.assessment.remark,
                        subtasks = aw.subtasks.map { st ->
                            SubtaskUi(st.id, st.name, st.dueAt)
                        }
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------- Intents ----------
    fun addAssessment(title: String, dueAt: LocalDateTime) =
        viewModelScope.launch { repo.addAssessment(title, dueAt) }

    fun updateAssessment(id: Long, title: String, dueAt: LocalDateTime) =
        viewModelScope.launch { repo.updateAssessment(id, title, dueAt) }

    fun deleteAssessment(id: Long) =
        viewModelScope.launch { repo.deleteAssessment(id) }

    fun addSubtask(assessmentId: Long, name: String, dueAt: LocalDateTime) =
        viewModelScope.launch { repo.addSubtask(assessmentId, name, dueAt) }

    fun updateSubtask(assessmentId: Long, subtaskId: Long, name: String, dueAt: LocalDateTime) =
        viewModelScope.launch { repo.updateSubtask(assessmentId, subtaskId, name, dueAt) }

    fun deleteSubtask(assessmentId: Long, subtaskId: Long) =
        viewModelScope.launch { repo.deleteSubtask(assessmentId, subtaskId) }

    fun updateRemark(assessmentId: Long, remark: String) =
        viewModelScope.launch { repo.updateRemark(assessmentId, remark) }

    // ---------- Production wiring ----------
    companion object {
        @VisibleForTesting
        internal fun defaultRepo(app: Application): PlanRepositoryLike {
            val real = PlanRepository(PlanDb.get(app).dao())
            return object : PlanRepositoryLike {
                override val items: Flow<List<RepoAggregate>> =
                    real.items.map { list ->
                        list.map { aw ->
                            RepoAggregate(
                                assessment = RepoAssessment(
                                    id = aw.assessment.id,
                                    title = aw.assessment.title,
                                    dueAt = aw.assessment.dueAt,
                                    remark = aw.assessment.remark
                                ),
                                subtasks = aw.subtasks.map { st ->
                                    RepoSubtask(
                                        id = st.id,
                                        name = st.name,
                                        dueAt = st.dueAt
                                    )
                                }
                            )
                        }
                    }

                override suspend fun addAssessment(title: String, dueAt: LocalDateTime) =
                    real.addAssessment(title, dueAt)

                override suspend fun updateAssessment(id: Long, title: String, dueAt: LocalDateTime) =
                    real.updateAssessment(id, title, dueAt)

                override suspend fun deleteAssessment(id: Long) =
                    real.deleteAssessment(id)

                override suspend fun addSubtask(assessmentId: Long, name: String, dueAt: LocalDateTime) =
                    real.addSubtask(assessmentId, name, dueAt)

                override suspend fun updateSubtask(
                    assessmentId: Long,
                    subtaskId: Long,
                    name: String,
                    dueAt: LocalDateTime
                ) = real.updateSubtask(assessmentId, subtaskId, name, dueAt)

                override suspend fun deleteSubtask(assessmentId: Long, subtaskId: Long) =
                    real.deleteSubtask(assessmentId, subtaskId)

                override suspend fun updateRemark(assessmentId: Long, remark: String) =
                    real.updateRemark(assessmentId, remark)
            }
        }
    }
}
