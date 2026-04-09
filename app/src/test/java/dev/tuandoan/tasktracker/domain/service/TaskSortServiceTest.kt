package dev.tuandoan.tasktracker.domain.service

import dev.tuandoan.tasktracker.domain.model.CompletedGrouping
import dev.tuandoan.tasktracker.domain.model.SortDirection
import dev.tuandoan.tasktracker.domain.model.SortKey
import dev.tuandoan.tasktracker.domain.model.TaskSort
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskSortServiceTest {

    private lateinit var service: TaskSortService

    @Before
    fun setup() {
        service = TaskSortService()
    }

    // === Empty list ===

    @Test
    fun `sortTasks with empty list returns empty list`() {
        val result = service.sortTasks(emptyList(), service.getDefaultSort())
        assertTrue(result.isEmpty())
    }

    // === Sort by CREATED_AT ===

    @Test
    fun `sortTasks by createdAt DESC returns newest first`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Old", createdAt = 1000L),
            TestTaskFactory.createTask(id = 2, title = "New", createdAt = 3000L),
            TestTaskFactory.createTask(id = 3, title = "Mid", createdAt = 2000L),
        )
        val sort = TaskSort(SortKey.CREATED_AT, SortDirection.DESC)

        val result = service.sortTasks(tasks, sort)

        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }

    @Test
    fun `sortTasks by createdAt ASC returns oldest first`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Old", createdAt = 1000L),
            TestTaskFactory.createTask(id = 2, title = "New", createdAt = 3000L),
            TestTaskFactory.createTask(id = 3, title = "Mid", createdAt = 2000L),
        )
        val sort = TaskSort(SortKey.CREATED_AT, SortDirection.ASC)

        val result = service.sortTasks(tasks, sort)

        assertEquals(listOf(1L, 3L, 2L), result.map { it.id })
    }

    // === Sort by TITLE ===

    @Test
    fun `sortTasks by title ASC is case insensitive`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Banana", createdAt = 1000L),
            TestTaskFactory.createTask(id = 2, title = "apple", createdAt = 2000L),
            TestTaskFactory.createTask(id = 3, title = "Cherry", createdAt = 3000L),
        )
        val sort = TaskSort(SortKey.TITLE, SortDirection.ASC)

        val result = service.sortTasks(tasks, sort)

        assertEquals(listOf("apple", "Banana", "Cherry"), result.map { it.title })
    }

    @Test
    fun `sortTasks by title DESC returns Z to A`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Apple", createdAt = 1000L),
            TestTaskFactory.createTask(id = 2, title = "Cherry", createdAt = 2000L),
            TestTaskFactory.createTask(id = 3, title = "Banana", createdAt = 3000L),
        )
        val sort = TaskSort(SortKey.TITLE, SortDirection.DESC)

        val result = service.sortTasks(tasks, sort)

        assertEquals(listOf("Cherry", "Banana", "Apple"), result.map { it.title })
    }

    @Test
    fun `sortTasks by title uses createdAt as secondary key`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Same", createdAt = 1000L),
            TestTaskFactory.createTask(id = 2, title = "Same", createdAt = 3000L),
            TestTaskFactory.createTask(id = 3, title = "Same", createdAt = 2000L),
        )
        val sort = TaskSort(SortKey.TITLE, SortDirection.ASC)

        val result = service.sortTasks(tasks, sort)

        // Secondary sort is thenByDescending { it.createdAt }, so newest first among same title
        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }

    // === Sort by PRIORITY ===

    @Test
    fun `sortTasks by priority DESC puts high priority first`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Low", priority = 0, createdAt = 1000L),
            TestTaskFactory.createTask(id = 2, title = "High", priority = 2, createdAt = 2000L),
            TestTaskFactory.createTask(id = 3, title = "Med", priority = 1, createdAt = 3000L),
        )
        val sort = TaskSort(SortKey.PRIORITY, SortDirection.DESC)

        val result = service.sortTasks(tasks, sort)

        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }

    @Test
    fun `sortTasks by priority ASC puts low priority first`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Low", priority = 0, createdAt = 1000L),
            TestTaskFactory.createTask(id = 2, title = "High", priority = 2, createdAt = 2000L),
            TestTaskFactory.createTask(id = 3, title = "Med", priority = 1, createdAt = 3000L),
        )
        val sort = TaskSort(SortKey.PRIORITY, SortDirection.ASC)

        val result = service.sortTasks(tasks, sort)

        assertEquals(listOf(1L, 3L, 2L), result.map { it.id })
    }

    @Test
    fun `sortTasks by priority uses createdAt as secondary key`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "First", priority = 2, createdAt = 1000L),
            TestTaskFactory.createTask(id = 2, title = "Second", priority = 2, createdAt = 3000L),
            TestTaskFactory.createTask(id = 3, title = "Third", priority = 2, createdAt = 2000L),
        )
        val sort = TaskSort(SortKey.PRIORITY, SortDirection.DESC)

        val result = service.sortTasks(tasks, sort)

        // Same priority, secondary sort is thenByDescending { createdAt }
        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }

    // === Completed Grouping ===

    @Test
    fun `COMPLETED_LAST groups active before completed`() {
        val tasks = listOf(
            TestTaskFactory.completedTask(id = 1, title = "Done", createdAt = 3000L),
            TestTaskFactory.createTask(id = 2, title = "Active1", createdAt = 2000L),
            TestTaskFactory.createTask(id = 3, title = "Active2", createdAt = 1000L),
        )
        val sort = TaskSort(SortKey.CREATED_AT, SortDirection.DESC, CompletedGrouping.COMPLETED_LAST)

        val result = service.sortTasks(tasks, sort)

        // Active first (sorted DESC by createdAt), then completed
        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
        assertTrue(!result[0].isCompleted)
        assertTrue(!result[1].isCompleted)
        assertTrue(result[2].isCompleted)
    }

    @Test
    fun `COMPLETED_FIRST groups completed before active`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Active", createdAt = 3000L),
            TestTaskFactory.completedTask(id = 2, title = "Done1", createdAt = 2000L),
            TestTaskFactory.completedTask(id = 3, title = "Done2", createdAt = 1000L),
        )
        val sort = TaskSort(SortKey.CREATED_AT, SortDirection.DESC, CompletedGrouping.COMPLETED_FIRST)

        val result = service.sortTasks(tasks, sort)

        // Completed first (sorted DESC by createdAt), then active
        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
        assertTrue(result[0].isCompleted)
        assertTrue(result[1].isCompleted)
        assertTrue(!result[2].isCompleted)
    }

    @Test
    fun `NONE grouping sorts all tasks together`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Active", createdAt = 1000L),
            TestTaskFactory.completedTask(id = 2, title = "Done", createdAt = 3000L),
            TestTaskFactory.createTask(id = 3, title = "Active2", createdAt = 2000L),
        )
        val sort = TaskSort(SortKey.CREATED_AT, SortDirection.DESC, CompletedGrouping.NONE)

        val result = service.sortTasks(tasks, sort)

        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }

    // === Default and utility ===

    @Test
    fun `getDefaultSort returns createdAt DESC with no grouping`() {
        val defaultSort = service.getDefaultSort()

        assertEquals(SortKey.CREATED_AT, defaultSort.key)
        assertEquals(SortDirection.DESC, defaultSort.direction)
        assertEquals(CompletedGrouping.NONE, defaultSort.completedGrouping)
    }

    @Test
    fun `isValidSort returns true for any sort`() {
        val sort = TaskSort(SortKey.TITLE, SortDirection.ASC, CompletedGrouping.COMPLETED_LAST)
        assertTrue(service.isValidSort(sort))
    }

    @Test
    fun `getAvailableSortOptions returns five options`() {
        val options = service.getAvailableSortOptions()
        assertEquals(5, options.size)
    }

    // === Sort by DUE_DATE ===

    @Test
    fun `sortTasks by dueDate ASC returns earliest due date first with nulls last`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "No due", dueAt = null, createdAt = 3000L),
            TestTaskFactory.createTask(id = 2, title = "Later", dueAt = 2000L, createdAt = 2000L),
            TestTaskFactory.createTask(id = 3, title = "Sooner", dueAt = 1000L, createdAt = 1000L),
        )
        val sort = TaskSort(SortKey.DUE_DATE, SortDirection.ASC)

        val result = service.sortTasks(tasks, sort)

        assertEquals(listOf(3L, 2L, 1L), result.map { it.id })
    }

    @Test
    fun `sortTasks by dueDate DESC returns latest due date first with nulls last`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "No due", dueAt = null, createdAt = 3000L),
            TestTaskFactory.createTask(id = 2, title = "Later", dueAt = 2000L, createdAt = 2000L),
            TestTaskFactory.createTask(id = 3, title = "Sooner", dueAt = 1000L, createdAt = 1000L),
        )
        val sort = TaskSort(SortKey.DUE_DATE, SortDirection.DESC)

        val result = service.sortTasks(tasks, sort)

        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }

    @Test
    fun `sortTasks by dueDate ASC uses createdAt as secondary key`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "First", dueAt = 5000L, createdAt = 1000L),
            TestTaskFactory.createTask(id = 2, title = "Second", dueAt = 5000L, createdAt = 3000L),
            TestTaskFactory.createTask(id = 3, title = "Third", dueAt = 5000L, createdAt = 2000L),
        )
        val sort = TaskSort(SortKey.DUE_DATE, SortDirection.ASC)

        val result = service.sortTasks(tasks, sort)

        // Same dueAt, secondary sort is thenByDescending { createdAt }
        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }

    @Test
    fun `sortTasks by dueDate with all nulls sorts by createdAt descending`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Old", dueAt = null, createdAt = 1000L),
            TestTaskFactory.createTask(id = 2, title = "New", dueAt = null, createdAt = 3000L),
            TestTaskFactory.createTask(id = 3, title = "Mid", dueAt = null, createdAt = 2000L),
        )
        val sort = TaskSort(SortKey.DUE_DATE, SortDirection.ASC)

        val result = service.sortTasks(tasks, sort)

        // All nulls, so secondary sort by createdAt descending
        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }

    // === Single element list ===

    @Test
    fun `sortTasks with single element returns same element`() {
        val tasks = listOf(TestTaskFactory.createTask(id = 1))
        val result = service.sortTasks(tasks, service.getDefaultSort())

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }
}
