package com.example.studyplanner.data.plan

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class PlanRepositoryIntegrationTest {

    private lateinit var db: PlanDb
    private lateinit var dao: PlanDao
    private lateinit var repo: PlanRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PlanDb::class.java)
            .allowMainThreadQueries() // fine for tests
            .build()
        dao = db.dao()
        repo = PlanRepository(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_read_back_assessment() = runTest {
        val due = LocalDateTime.of(2030, 1, 1, 9, 0)
        val id = repo.addAssessment("Integration A1", due)

        val list = repo.items.first()
        val a = list.firstOrNull { it.assessment.id == id }
        assertNotNull(a)
        assertEquals("Integration A1", a!!.assessment.title)
        assertEquals(due, a.assessment.dueAt)
        // default remark may be "" depending on your repo — assert if you want:
        // assertEquals("", a.assessment.remark)
    }

    @Test
    fun add_update_delete_subtask_and_remark() = runTest {
        val aId = repo.addAssessment("A1", LocalDateTime.of(2030, 1, 1, 9, 0))

        // add subtask
        val stId = repo.addSubtask(aId, "Read paper", LocalDateTime.of(2030, 1, 1, 18, 0))
        var items = repo.items.first()
        var agg = items.first { it.assessment.id == aId }
        assertEquals(1, agg.subtasks.size)
        assertEquals(stId, agg.subtasks.first().id)

        // update subtask + remark + due date/title
        repo.updateSubtask(aId, stId, "Read + annotate", LocalDateTime.of(2030, 1, 2, 12, 0))
        repo.updateRemark(aId, "Bring printed copy")
        repo.updateAssessment(aId, "A1 (final)", LocalDateTime.of(2030, 2, 2, 10, 0))

        items = repo.items.first()
        agg = items.first { it.assessment.id == aId }
        assertEquals("A1 (final)", agg.assessment.title)
        assertEquals(LocalDateTime.of(2030, 2, 2, 10, 0), agg.assessment.dueAt)
        assertEquals("Bring printed copy", agg.assessment.remark)
        assertEquals("Read + annotate", agg.subtasks.first().name)

        // delete subtask
        repo.deleteSubtask(aId, stId)
        items = repo.items.first()
        agg = items.first { it.assessment.id == aId }
        assertTrue(agg.subtasks.isEmpty())

        // delete assessment
        repo.deleteAssessment(aId)
        items = repo.items.first()
        assertTrue(items.none { it.assessment.id == aId })
    }
}
