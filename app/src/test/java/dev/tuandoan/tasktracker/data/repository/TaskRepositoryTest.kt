package dev.tuandoan.tasktracker.data.repository

import app.cash.turbine.test
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRepositoryTest {

    private lateinit var repository: FakeTaskRepository

    @Before
    fun setup() {
        repository = FakeTaskRepository()
    }

    // === CRUD ===

    @Test
    fun `insertTask assigns auto-increment id when id is zero`() = runTest {
        val task = TestTaskFactory.createTask(id = 0, title = "New")
        val id = repository.insertTask(task)

        assertTrue(id > 0)
        val stored = repository.getTaskById(id)
        assertNotNull(stored)
        assertEquals("New", stored!!.title)
    }

    @Test
    fun `getTaskById returns null for non-existent id`() = runTest {
        assertNull(repository.getTaskById(999))
    }

    @Test
    fun `updateTask modifies existing task`() = runTest {
        repository.seed(TestTaskFactory.createTask(id = 1, title = "Old"))
        repository.updateTask(TestTaskFactory.createTask(id = 1, title = "Updated"))

        val result = repository.getTaskById(1)
        assertEquals("Updated", result!!.title)
    }

    @Test
    fun `deleteTask removes task`() = runTest {
        val task = TestTaskFactory.createTask(id = 1, title = "ToDelete")
        repository.seed(task)

        repository.deleteTask(task)

        assertNull(repository.getTaskById(1))
    }

    // === Flow Observation ===

    @Test
    fun `getAllTasks excludes archived tasks`() = runTest {
        repository.seed(
            TestTaskFactory.createTask(id = 1, title = "Active"),
            TestTaskFactory.archivedTask(id = 2, title = "Archived"),
        )

        repository.getAllTasks().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Active", result[0].title)
        }
    }

    @Test
    fun `getActiveTasks excludes completed and archived`() = runTest {
        repository.seed(
            TestTaskFactory.createTask(id = 1, title = "Active"),
            TestTaskFactory.completedTask(id = 2, title = "Done"),
            TestTaskFactory.archivedTask(id = 3, title = "Archived"),
        )

        repository.getActiveTasks().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Active", result[0].title)
        }
    }

    @Test
    fun `getCompletedTasks returns only completed non-archived`() = runTest {
        repository.seed(
            TestTaskFactory.createTask(id = 1, title = "Active"),
            TestTaskFactory.completedTask(id = 2, title = "Done"),
            TestTaskFactory.archivedTask(id = 3, title = "Archived"),
        )

        repository.getCompletedTasks().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Done", result[0].title)
        }
    }

    @Test
    fun `getArchivedTasks returns only archived`() = runTest {
        repository.seed(
            TestTaskFactory.createTask(id = 1, title = "Active"),
            TestTaskFactory.archivedTask(id = 2, title = "Archived"),
        )

        repository.getArchivedTasks().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Archived", result[0].title)
        }
    }

    // === Archive Operations ===

    @Test
    fun `archiveTask sets isArchived and archivedAt`() = runTest {
        repository.seed(TestTaskFactory.createTask(id = 1, title = "Active"))

        repository.archiveTask(1)

        val result = repository.getTaskById(1)
        assertTrue(result!!.isArchived)
        assertNotNull(result.archivedAt)
    }

    @Test
    fun `unarchiveTask clears isArchived and archivedAt`() = runTest {
        repository.seed(TestTaskFactory.archivedTask(id = 1))

        repository.unarchiveTask(1)

        val result = repository.getTaskById(1)
        assertFalse(result!!.isArchived)
        assertNull(result.archivedAt)
    }

    // === Bulk Operations ===

    @Test
    fun `markCompleted sets isCompleted for multiple ids`() = runTest {
        repository.seed(
            TestTaskFactory.createTask(id = 1, title = "A"),
            TestTaskFactory.createTask(id = 2, title = "B"),
            TestTaskFactory.createTask(id = 3, title = "C"),
        )

        repository.markCompleted(listOf(1, 3))

        assertTrue(repository.getTaskById(1)!!.isCompleted)
        assertFalse(repository.getTaskById(2)!!.isCompleted)
        assertTrue(repository.getTaskById(3)!!.isCompleted)
    }

    @Test
    fun `markActive clears isCompleted for multiple ids`() = runTest {
        repository.seed(
            TestTaskFactory.completedTask(id = 1),
            TestTaskFactory.completedTask(id = 2),
        )

        repository.markActive(listOf(1, 2))

        assertFalse(repository.getTaskById(1)!!.isCompleted)
        assertFalse(repository.getTaskById(2)!!.isCompleted)
    }

    @Test
    fun `deleteByIds removes multiple tasks`() = runTest {
        repository.seed(
            TestTaskFactory.createTask(id = 1),
            TestTaskFactory.createTask(id = 2),
            TestTaskFactory.createTask(id = 3),
        )

        repository.deleteByIds(listOf(1, 3))

        assertNull(repository.getTaskById(1))
        assertNotNull(repository.getTaskById(2))
        assertNull(repository.getTaskById(3))
    }

    // === Observation Count Flows ===

    @Test
    fun `observeActiveCount reflects non-completed non-archived`() = runTest {
        repository.seed(
            TestTaskFactory.createTask(id = 1),
            TestTaskFactory.completedTask(id = 2),
            TestTaskFactory.archivedTask(id = 3),
            TestTaskFactory.createTask(id = 4),
        )

        repository.observeActiveCount().test {
            assertEquals(2, awaitItem())
        }
    }

    @Test
    fun `observeCompletedCount reflects completed non-archived`() = runTest {
        repository.seed(
            TestTaskFactory.createTask(id = 1),
            TestTaskFactory.completedTask(id = 2),
            TestTaskFactory.completedTask(id = 3),
        )

        repository.observeCompletedCount().test {
            assertEquals(2, awaitItem())
        }
    }

    // === Pin and Priority ===

    @Test
    fun `setPinned updates pin status`() = runTest {
        repository.seed(TestTaskFactory.createTask(id = 1))

        repository.setPinned(1, true)
        assertTrue(repository.getTaskById(1)!!.isPinned)

        repository.setPinned(1, false)
        assertFalse(repository.getTaskById(1)!!.isPinned)
    }

    @Test
    fun `setPriority updates priority value`() = runTest {
        repository.seed(TestTaskFactory.createTask(id = 1, priority = 1))

        repository.setPriority(1, 2)
        assertEquals(2, repository.getTaskById(1)!!.priority)
    }
}
