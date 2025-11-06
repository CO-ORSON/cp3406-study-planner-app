package com.example.studyplanner.ui.plan

import app.cash.turbine.test
import com.example.studyplanner.MainDispatcherRule
import com.example.studyplanner.ui.plan.PlanViewModel.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

class PlanViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    // ---- Minimal Fake Repo implementing the VM interface ----
    private class FakeRepo : PlanRepositoryLike {
        private val _items = MutableStateFlow<List<RepoAggregate>>(emptyList())
        override val items = _items

        private var nextAId = 1L
        private var nextSId = 1L

        override suspend fun addAssessment(title: String, dueAt: LocalDateTime): Long {
            val id = nextAId++
            val agg = RepoAggregate(
                assessment = RepoAssessment(id, title, dueAt, remark = ""),
                subtasks = emptyList()
            )
            _items.update { it + agg }
            return id
        }

        override suspend fun updateAssessment(id: Long, title: String, dueAt: LocalDateTime) {
            _items.update { list ->
                list.map { agg ->
                    if (agg.assessment.id == id)
                        agg.copy(assessment = agg.assessment.copy(title = title, dueAt = dueAt))
                    else agg
                }
            }
        }

        override suspend fun deleteAssessment(id: Long) {
            _items.update { list -> list.filterNot { it.assessment.id == id } }
        }

        override suspend fun addSubtask(assessmentId: Long, name: String, dueAt: LocalDateTime): Long {
            val sid = nextSId++
            _items.update { list ->
                list.map { agg ->
                    if (agg.assessment.id == assessmentId)
                        agg.copy(subtasks = agg.subtasks + RepoSubtask(sid, name, dueAt))
                    else agg
                }
            }
            return sid
        }

        override suspend fun updateSubtask(
            assessmentId: Long,
            subtaskId: Long,
            name: String,
            dueAt: LocalDateTime
        ) {
            _items.update { list ->
                list.map { agg ->
                    if (agg.assessment.id == assessmentId)
                        agg.copy(subtasks = agg.subtasks.map { st ->
                            if (st.id == subtaskId) st.copy(name = name, dueAt = dueAt) else st
                        })
                    else agg
                }
            }
        }

        override suspend fun deleteSubtask(assessmentId: Long, subtaskId: Long) {
            _items.update { list ->
                list.map { agg ->
                    if (agg.assessment.id == assessmentId)
                        agg.copy(subtasks = agg.subtasks.filterNot { it.id == subtaskId })
                    else agg
                }
            }
        }

        override suspend fun updateRemark(assessmentId: Long, remark: String) {
            _items.update { list ->
                list.map { agg ->
                    if (agg.assessment.id == assessmentId)
                        agg.copy(assessment = agg.assessment.copy(remark = remark))
                    else agg
                }
            }
        }
    }


    // simple Application stub (AndroidViewModel needs one)
    private fun appStub() = android.app.Application()

    @Test
    fun `initial is empty then add emits new item`() = runTest {
        val vm = PlanViewModel(app = appStub(), repo = FakeRepo())

        vm.items.test {
            assertEquals(0, awaitItem().size) // initial empty

            val due = LocalDateTime.of(2030, 1, 1, 9, 0)
            vm.addAssessment("A1", due)
            advanceUntilIdle()

            val after = awaitItem()
            assertEquals(1, after.size)
            assertEquals("A1", after.first().title)
            assertEquals(due, after.first().dueAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateAssessment and updateRemark reflect in state`() = runTest {
        val vm = PlanViewModel(app = appStub(), repo = FakeRepo())

        vm.items.test {
            awaitItem() // [] initial
            vm.addAssessment("Draft", LocalDateTime.of(2030, 1, 1, 9, 0))
            advanceUntilIdle()
            awaitItem() // [Draft]

            vm.updateAssessment(1, "Final", LocalDateTime.of(2030, 2, 2, 10, 0))
            vm.updateRemark(1, "Bring printed copy")
            advanceUntilIdle()

            val updated = awaitItem()
            val a1 = updated.first()
            assertEquals("Final", a1.title)
            assertEquals(LocalDateTime.of(2030, 2, 2, 10, 0), a1.dueAt)
            assertEquals("Bring printed copy", a1.remark)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `add update delete subtask`() = runTest {
        val vm = PlanViewModel(app = appStub(), repo = FakeRepo())

        vm.items.test {
            awaitItem() // []
            vm.addAssessment("A1", LocalDateTime.of(2030,1,1,9,0))
            advanceUntilIdle()
            awaitItem() // [A1]

            vm.addSubtask(1, "Read paper", LocalDateTime.of(2030,1,1,18,0))
            advanceUntilIdle()
            var after = awaitItem()
            assertEquals(1, after.first().subtasks.size)
            val stId = after.first().subtasks.first().id

            vm.updateSubtask(1, stId, "Read + annotate", LocalDateTime.of(2030,1,2,12,0))
            advanceUntilIdle()
            after = awaitItem()
            assertEquals("Read + annotate", after.first().subtasks.first().name)

            vm.deleteSubtask(1, stId)
            advanceUntilIdle()
            after = awaitItem()
            assertEquals(0, after.first().subtasks.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
