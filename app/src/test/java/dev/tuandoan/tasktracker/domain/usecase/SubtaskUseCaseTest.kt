package dev.tuandoan.tasktracker.domain.usecase

import app.cash.turbine.test
import dev.tuandoan.tasktracker.data.database.Subtask
import dev.tuandoan.tasktracker.domain.repository.ISubtaskRepository
import dev.tuandoan.tasktracker.testutil.FakeSubtaskRepository
import dev.tuandoan.tasktracker.testutil.TestSubtaskFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class SubtaskUseCaseTest {

    private lateinit var repository: FakeSubtaskRepository
    private lateinit var useCase: SubtaskUseCase

    @Before
    fun setup() {
        repository = FakeSubtaskRepository()
        useCase = SubtaskUseCase(repository)
    }

    // === addSubtask ===

    @Test
    fun `addSubtask with valid title inserts and returns id`() = runTest {
        val result = useCase.addSubtask(taskId = 1L, title = "Buy milk")

        assertTrue(result.isSuccess)
        val id = result.getOrThrow()
        assertTrue(id > 0)
        val stored = repository.getSubtaskById(id)
        assertNotNull(stored)
        assertEquals("Buy milk", stored!!.title)
        assertEquals(1L, stored.taskId)
    }

    @Test
    fun `addSubtask trims whitespace`() = runTest {
        val result = useCase.addSubtask(taskId = 1L, title = "  Trim me  ")

        val stored = repository.getSubtaskById(result.getOrThrow())
        assertEquals("Trim me", stored!!.title)
    }

    @Test
    fun `addSubtask with blank title returns failure`() = runTest {
        val result = useCase.addSubtask(taskId = 1L, title = "   ")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(0, repository.getAllSubtasksSnapshot().size)
    }

    @Test
    fun `addSubtask with title over 500 chars returns failure`() = runTest {
        val longTitle = "a".repeat(SubtaskUseCase.MAX_TITLE_LENGTH + 1)

        val result = useCase.addSubtask(taskId = 1L, title = longTitle)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(0, repository.getAllSubtasksSnapshot().size)
    }

    @Test
    fun `addSubtask at exactly 500 chars succeeds`() = runTest {
        val title = "a".repeat(SubtaskUseCase.MAX_TITLE_LENGTH)

        val result = useCase.addSubtask(taskId = 1L, title = title)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `addSubtask assigns next sortOrder based on existing count`() = runTest {
        repository.seed(
            TestSubtaskFactory.createSubtask(id = 10L, taskId = 1L, sortOrder = 0),
            TestSubtaskFactory.createSubtask(id = 11L, taskId = 1L, sortOrder = 1),
            TestSubtaskFactory.createSubtask(id = 12L, taskId = 2L, sortOrder = 0),
        )

        val result = useCase.addSubtask(taskId = 1L, title = "Appended")

        val stored = repository.getSubtaskById(result.getOrThrow())
        assertEquals(2, stored!!.sortOrder)
    }

    // === updateTitle ===

    @Test
    fun `updateTitle with valid title updates`() = runTest {
        val existing = TestSubtaskFactory.createSubtask(id = 1L, title = "original")
        repository.seed(existing)

        val result = useCase.updateTitle(subtaskId = 1L, title = "changed")

        assertTrue(result.isSuccess)
        assertEquals("changed", repository.getSubtaskById(1L)!!.title)
    }

    @Test
    fun `updateTitle with blank title returns failure and does not change data`() = runTest {
        val existing = TestSubtaskFactory.createSubtask(id = 1L, title = "original")
        repository.seed(existing)

        val result = useCase.updateTitle(subtaskId = 1L, title = " ")

        assertTrue(result.isFailure)
        assertEquals("original", repository.getSubtaskById(1L)!!.title)
    }

    @Test
    fun `updateTitle for non-existent subtask returns failure`() = runTest {
        val result = useCase.updateTitle(subtaskId = 999L, title = "x")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    // === setCompleted ===

    @Test
    fun `setCompleted toggles completion state`() = runTest {
        val existing = TestSubtaskFactory.createSubtask(id = 1L, isCompleted = false)
        repository.seed(existing)

        assertTrue(useCase.setCompleted(1L, completed = true).isSuccess)
        assertTrue(repository.getSubtaskById(1L)!!.isCompleted)

        assertTrue(useCase.setCompleted(1L, completed = false).isSuccess)
        assertFalse(repository.getSubtaskById(1L)!!.isCompleted)
    }

    @Test
    fun `setCompleted is a no-op when state already matches`() = runTest {
        val existing = TestSubtaskFactory.createSubtask(id = 1L, isCompleted = true)
        repository.seed(existing)
        val before = repository.getSubtaskById(1L)

        val result = useCase.setCompleted(1L, completed = true)

        assertTrue(result.isSuccess)
        assertEquals(before, repository.getSubtaskById(1L))
    }

    @Test
    fun `setCompleted on missing subtask returns failure`() = runTest {
        val result = useCase.setCompleted(subtaskId = 42L, completed = true)

        assertTrue(result.isFailure)
    }

    // === delete ===

    @Test
    fun `delete removes existing subtask`() = runTest {
        repository.seed(TestSubtaskFactory.createSubtask(id = 1L))

        val result = useCase.delete(subtaskId = 1L)

        assertTrue(result.isSuccess)
        assertNull(repository.getSubtaskById(1L))
    }

    @Test
    fun `delete of non-existent id succeeds silently`() = runTest {
        val result = useCase.delete(subtaskId = 99L)

        assertTrue(result.isSuccess)
    }

    // === reorder ===

    @Test
    fun `reorder rewrites sortOrder based on list position`() = runTest {
        repository.seed(
            TestSubtaskFactory.createSubtask(id = 1L, taskId = 1L, sortOrder = 0, title = "A"),
            TestSubtaskFactory.createSubtask(id = 2L, taskId = 1L, sortOrder = 1, title = "B"),
            TestSubtaskFactory.createSubtask(id = 3L, taskId = 1L, sortOrder = 2, title = "C"),
        )

        val result = useCase.reorder(taskId = 1L, orderedIds = listOf(3L, 1L, 2L))

        assertTrue(result.isSuccess)
        val ordered = repository.getSubtasks(1L)
        assertEquals(listOf("C", "A", "B"), ordered.map { it.title })
        assertEquals(listOf(0, 1, 2), ordered.map { it.sortOrder })
    }

    @Test
    fun `reorder with mismatched id set returns failure and does not mutate`() = runTest {
        repository.seed(
            TestSubtaskFactory.createSubtask(id = 1L, taskId = 1L, sortOrder = 0),
            TestSubtaskFactory.createSubtask(id = 2L, taskId = 1L, sortOrder = 1),
        )

        // 99L is not part of this task's subtasks.
        val result = useCase.reorder(taskId = 1L, orderedIds = listOf(1L, 99L))

        assertTrue(result.isFailure)
        // Existing sort order preserved.
        assertEquals(listOf(0, 1), repository.getSubtasks(1L).map { it.sortOrder })
    }

    @Test
    fun `reorder with duplicate ids returns failure`() = runTest {
        repository.seed(
            TestSubtaskFactory.createSubtask(id = 1L, taskId = 1L, sortOrder = 0),
            TestSubtaskFactory.createSubtask(id = 2L, taskId = 1L, sortOrder = 1),
        )

        val result = useCase.reorder(taskId = 1L, orderedIds = listOf(1L, 1L))

        assertTrue(result.isFailure)
    }

    @Test
    fun `reorder with subset of ids returns failure`() = runTest {
        repository.seed(
            TestSubtaskFactory.createSubtask(id = 1L, taskId = 1L, sortOrder = 0),
            TestSubtaskFactory.createSubtask(id = 2L, taskId = 1L, sortOrder = 1),
            TestSubtaskFactory.createSubtask(id = 3L, taskId = 1L, sortOrder = 2),
        )

        val result = useCase.reorder(taskId = 1L, orderedIds = listOf(1L, 2L))

        assertTrue(result.isFailure)
    }

    // === resetCompletion ===

    @Test
    fun `resetCompletion unchecks only target task subtasks`() = runTest {
        repository.seed(
            TestSubtaskFactory.createSubtask(id = 1L, taskId = 1L, isCompleted = true),
            TestSubtaskFactory.createSubtask(id = 2L, taskId = 1L, isCompleted = true),
            TestSubtaskFactory.createSubtask(id = 3L, taskId = 2L, isCompleted = true),
        )

        val result = useCase.resetCompletion(taskId = 1L)

        assertTrue(result.isSuccess)
        assertFalse(repository.getSubtaskById(1L)!!.isCompleted)
        assertFalse(repository.getSubtaskById(2L)!!.isCompleted)
        // Subtasks on another task are untouched.
        assertTrue(repository.getSubtaskById(3L)!!.isCompleted)
    }

    // === observeSubtasks ===

    @Test
    fun `observeSubtasks returns ordered list isolated by taskId`() = runTest {
        repository.seed(
            TestSubtaskFactory.createSubtask(id = 1L, taskId = 1L, sortOrder = 1, title = "Second"),
            TestSubtaskFactory.createSubtask(id = 2L, taskId = 1L, sortOrder = 0, title = "First"),
            TestSubtaskFactory.createSubtask(id = 3L, taskId = 2L, sortOrder = 0, title = "Other"),
        )

        val emitted = useCase.observeSubtasks(taskId = 1L).first()

        assertEquals(listOf("First", "Second"), emitted.map { it.title })
    }

    @Test
    fun `observeSubtasks emits new item after addSubtask`() = runTest {
        useCase.observeSubtasks(taskId = 1L).test {
            assertEquals(emptyList<Subtask>(), awaitItem())

            useCase.addSubtask(taskId = 1L, title = "Added").getOrThrow()

            val next = awaitItem()
            assertEquals(listOf("Added"), next.map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === copySubtasksResetCompletion (repository contract) ===

    @Test
    fun `copySubtasksResetCompletion copies from source to target with isCompleted reset`() = runTest {
        repository.seed(
            TestSubtaskFactory.createSubtask(id = 1L, taskId = 10L, title = "A", sortOrder = 0, isCompleted = true),
            TestSubtaskFactory.createSubtask(id = 2L, taskId = 10L, title = "B", sortOrder = 1, isCompleted = true),
            TestSubtaskFactory.createSubtask(id = 3L, taskId = 99L, title = "Other", sortOrder = 0),
        )

        repository.copySubtasksResetCompletion(fromTaskId = 10L, toTaskId = 20L)

        val targetSubtasks = repository.getSubtasks(20L)
        assertEquals(listOf("A", "B"), targetSubtasks.map { it.title })
        assertEquals(listOf(0, 1), targetSubtasks.map { it.sortOrder })
        assertTrue(targetSubtasks.all { !it.isCompleted })

        // Source untouched.
        assertEquals(2, repository.getSubtasks(10L).size)
        assertTrue(repository.getSubtasks(10L).all { it.isCompleted })

        // Unrelated task untouched.
        assertEquals(1, repository.getSubtasks(99L).size)
    }

    @Test
    fun `copySubtasksResetCompletion is a no-op when source has no subtasks`() = runTest {
        repository.copySubtasksResetCompletion(fromTaskId = 10L, toTaskId = 20L)

        assertEquals(0, repository.getAllSubtasksSnapshot().size)
    }

    // === observeProgressByTaskId ===

    @Test
    fun `observeProgressByTaskId emits per-task progress keyed by taskId`() = runTest {
        repository.seed(
            TestSubtaskFactory.createSubtask(id = 1L, taskId = 10L, isCompleted = true),
            TestSubtaskFactory.createSubtask(id = 2L, taskId = 10L, isCompleted = false),
            TestSubtaskFactory.createSubtask(id = 3L, taskId = 10L, isCompleted = false),
            TestSubtaskFactory.createSubtask(id = 4L, taskId = 20L, isCompleted = true),
        )

        val progress = useCase.observeProgressByTaskId().first()

        assertEquals(2, progress.size)
        assertEquals(3, progress[10L]!!.total)
        assertEquals(1, progress[10L]!!.completed)
        assertEquals(1, progress[20L]!!.total)
        assertEquals(1, progress[20L]!!.completed)
    }

    @Test
    fun `observeProgressByTaskId emits empty map when no subtasks exist`() = runTest {
        val progress = useCase.observeProgressByTaskId().first()
        assertTrue(progress.isEmpty())
    }

    // === CancellationException propagation ===

    @Test
    fun `mutations rethrow CancellationException instead of wrapping in Result`() = runTest {
        val throwing = object : ISubtaskRepository by repository {
            override suspend fun updateSubtask(subtask: Subtask): Unit = throw CancellationException("cancel")
        }
        val cancellingUseCase = SubtaskUseCase(throwing)
        repository.seed(TestSubtaskFactory.createSubtask(id = 1L, isCompleted = false))

        try {
            cancellingUseCase.setCompleted(subtaskId = 1L, completed = true)
            fail("CancellationException should have propagated")
        } catch (expected: CancellationException) {
            assertEquals("cancel", expected.message)
        }
    }
}
