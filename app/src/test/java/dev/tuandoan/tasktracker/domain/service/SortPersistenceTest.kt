package dev.tuandoan.tasktracker.domain.service

import dev.tuandoan.tasktracker.domain.model.CompletedGrouping
import dev.tuandoan.tasktracker.domain.model.SortDirection
import dev.tuandoan.tasktracker.domain.model.SortKey
import dev.tuandoan.tasktracker.domain.model.TaskSort
import dev.tuandoan.tasktracker.ui.state.TaskListStateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for sort configuration, default values, and sort display names.
 */
class SortPersistenceTest {

    // === DEFAULT_SORT ===

    @Test
    fun `DEFAULT_SORT is DUE_DATE ASC COMPLETED_LAST`() {
        val defaultSort = TaskListStateManager.DEFAULT_SORT
        assertEquals(SortKey.DUE_DATE, defaultSort.key)
        assertEquals(SortDirection.ASC, defaultSort.direction)
        assertEquals(CompletedGrouping.COMPLETED_LAST, defaultSort.completedGrouping)
    }

    // === TaskSort display names ===

    @Test
    fun `getDisplayName for CREATED_AT DESC`() {
        val sort = TaskSort(SortKey.CREATED_AT, SortDirection.DESC)
        assertEquals("Created: Newest first", sort.getDisplayName())
    }

    @Test
    fun `getDisplayName for CREATED_AT ASC`() {
        val sort = TaskSort(SortKey.CREATED_AT, SortDirection.ASC)
        assertEquals("Created: Oldest first", sort.getDisplayName())
    }

    @Test
    fun `getDisplayName for TITLE ASC`() {
        val sort = TaskSort(SortKey.TITLE, SortDirection.ASC)
        assertEquals("Title: A–Z", sort.getDisplayName())
    }

    @Test
    fun `getDisplayName for PRIORITY DESC`() {
        val sort = TaskSort(SortKey.PRIORITY, SortDirection.DESC)
        assertEquals("Priority: High to Low", sort.getDisplayName())
    }

    @Test
    fun `getDisplayName for DUE_DATE ASC`() {
        val sort = TaskSort(SortKey.DUE_DATE, SortDirection.ASC)
        assertEquals("Due Date: Earliest first", sort.getDisplayName())
    }

    @Test
    fun `getDisplayName for DUE_DATE DESC`() {
        val sort = TaskSort(SortKey.DUE_DATE, SortDirection.DESC)
        assertEquals("Due Date: Latest first", sort.getDisplayName())
    }

    // === getFullDisplayName with grouping ===

    @Test
    fun `getFullDisplayName with NONE grouping returns base name`() {
        val sort = TaskSort(SortKey.CREATED_AT, SortDirection.DESC, CompletedGrouping.NONE)
        assertEquals("Created: Newest first", sort.getFullDisplayName())
    }

    @Test
    fun `getFullDisplayName with COMPLETED_LAST appends suffix`() {
        val sort = TaskSort(SortKey.TITLE, SortDirection.ASC, CompletedGrouping.COMPLETED_LAST)
        assertEquals("Title: A–Z (Completed last)", sort.getFullDisplayName())
    }

    @Test
    fun `getFullDisplayName with COMPLETED_FIRST appends suffix`() {
        val sort = TaskSort(SortKey.PRIORITY, SortDirection.DESC, CompletedGrouping.COMPLETED_FIRST)
        assertEquals("Priority: High to Low (Completed first)", sort.getFullDisplayName())
    }

    // === TaskSort equality ===

    @Test
    fun `TaskSort with same fields is equal`() {
        val a = TaskSort(SortKey.TITLE, SortDirection.ASC, CompletedGrouping.COMPLETED_LAST)
        val b = TaskSort(SortKey.TITLE, SortDirection.ASC, CompletedGrouping.COMPLETED_LAST)
        assertEquals(a, b)
    }

    @Test
    fun `TaskSort with different grouping is not equal`() {
        val a = TaskSort(SortKey.TITLE, SortDirection.ASC, CompletedGrouping.NONE)
        val b = TaskSort(SortKey.TITLE, SortDirection.ASC, CompletedGrouping.COMPLETED_LAST)
        assertNotEquals(a, b)
    }

    // === Sort options list ===

    @Test
    fun `sort options include DUE_DATE as first entry`() {
        val service = TaskSortService()
        val options = service.getAvailableSortOptions()
        assertEquals(SortKey.DUE_DATE, options[0].key)
        assertEquals(SortDirection.ASC, options[0].direction)
    }

    @Test
    fun `all sort options have NONE completed grouping`() {
        val service = TaskSortService()
        val options = service.getAvailableSortOptions()
        assertTrue(options.all { it.completedGrouping == CompletedGrouping.NONE })
    }

    // === Due date sort edge cases ===

    @Test
    fun `sortTasks by dueDate mixed nulls and values in ASC puts nulls last`() {
        val service = TaskSortService()
        val tasks = listOf(
            dev.tuandoan.tasktracker.testutil.TestTaskFactory.createTask(
                id = 1,
                title = "No due",
                dueAt = null,
                createdAt = 5000L,
            ),
            dev.tuandoan.tasktracker.testutil.TestTaskFactory.createTask(
                id = 2,
                title = "Due soon",
                dueAt = 1000L,
                createdAt = 3000L,
            ),
            dev.tuandoan.tasktracker.testutil.TestTaskFactory.createTask(
                id = 3,
                title = "No due 2",
                dueAt = null,
                createdAt = 4000L,
            ),
            dev.tuandoan.tasktracker.testutil.TestTaskFactory.createTask(
                id = 4,
                title = "Due later",
                dueAt = 2000L,
                createdAt = 2000L,
            ),
        )
        val sort = TaskSort(SortKey.DUE_DATE, SortDirection.ASC)
        val result = service.sortTasks(tasks, sort)

        // Tasks with due dates first (ASC), then nulls (by createdAt DESC)
        assertEquals(listOf(2L, 4L, 1L, 3L), result.map { it.id })
    }

    @Test
    fun `sortTasks by dueDate DESC with nulls puts nulls last`() {
        val service = TaskSortService()
        val tasks = listOf(
            dev.tuandoan.tasktracker.testutil.TestTaskFactory.createTask(
                id = 1,
                title = "No due",
                dueAt = null,
                createdAt = 3000L,
            ),
            dev.tuandoan.tasktracker.testutil.TestTaskFactory.createTask(
                id = 2,
                title = "Due later",
                dueAt = 2000L,
                createdAt = 2000L,
            ),
            dev.tuandoan.tasktracker.testutil.TestTaskFactory.createTask(
                id = 3,
                title = "Due soon",
                dueAt = 1000L,
                createdAt = 1000L,
            ),
        )
        val sort = TaskSort(SortKey.DUE_DATE, SortDirection.DESC)
        val result = service.sortTasks(tasks, sort)

        // Tasks with due dates first (DESC), then nulls
        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }
}
