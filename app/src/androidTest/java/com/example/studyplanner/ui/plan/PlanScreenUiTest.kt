// app/src/androidTest/java/com/example/studyplanner/ui/plan/PlanScreenUiTest.kt
package com.example.studyplanner.ui.plan

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.studyplanner.ui.plan.PlanViewModel.*
import com.example.studyplanner.ui.screens.PlanScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class PlanScreenUiTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    // ---- Fake repo so UI tests don't hit Room ----
    private class FakeRepo : PlanRepositoryLike {
        private val _items = MutableStateFlow<List<RepoAggregate>>(emptyList())
        override val items = _items

        private var nextAId = 1L
        private var nextSId = 1L

        override suspend fun addAssessment(title: String, dueAt: LocalDateTime): Long {
            val id = nextAId++
            _items.update { list ->
                list + RepoAggregate(
                    assessment = RepoAssessment(id, title, dueAt, remark = ""),
                    subtasks = emptyList()
                )
            }
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

        override suspend fun addSubtask(
            assessmentId: Long,
            name: String,
            dueAt: LocalDateTime
        ): Long {
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

    private fun vm(): PlanViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        return PlanViewModel(app = app, repo = FakeRepo())
    }

    @Test
    fun add_flow_shows_new_item() {
        val vm = vm()
        compose.setContent { PlanScreen(vm = vm) }

        // Open dialog
        compose.onNodeWithTag("add-assessment").assertExists().performClick()

        // Type title and save
        compose.onNodeWithTag("field-title").assertExists().performTextClearance()
        compose.onNodeWithTag("field-title").performTextInput("UI A1")
        compose.onNodeWithTag("btn-save").assertExists().performClick()

        // Wait until it actually appears in the tree (handle async/recomposition)
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("UI A1", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Now assert it's displayed
        compose.onNodeWithText("UI A1", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun delete_item_removes_from_list() {
        val vm = vm()
        compose.setContent { PlanScreen(vm = vm) }

        // Add one
        compose.onNodeWithTag("add-assessment").performClick()
        compose.onNodeWithTag("field-title").performTextClearance()
        compose.onNodeWithTag("field-title").performTextInput("To be deleted")
        compose.onNodeWithTag("btn-save").performClick()

        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("To be deleted", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("To be deleted", useUnmergedTree = true).assertIsDisplayed()

        // Delete (first inserted gets id == 1 in FakeRepo)
        compose.onNodeWithTag("btn-delete-1").assertExists().performClick()
        compose.onNodeWithTag("confirm-delete").assertExists().performClick()

        // Wait until gone
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("To be deleted", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("To be deleted", useUnmergedTree = true).assertDoesNotExist()
    }
}
