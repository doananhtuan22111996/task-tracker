package dev.tuandoan.tasktracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskSortTest {

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
        assertEquals("Title: A\u2013Z", sort.getDisplayName())
    }

    @Test
    fun `getDisplayName for TITLE DESC`() {
        val sort = TaskSort(SortKey.TITLE, SortDirection.DESC)
        assertEquals("Title: Z\u2013A", sort.getDisplayName())
    }

    @Test
    fun `getDisplayName for PRIORITY DESC`() {
        val sort = TaskSort(SortKey.PRIORITY, SortDirection.DESC)
        assertEquals("Priority: High to Low", sort.getDisplayName())
    }

    @Test
    fun `getDisplayName for PRIORITY ASC`() {
        val sort = TaskSort(SortKey.PRIORITY, SortDirection.ASC)
        assertEquals("Priority: Low to High", sort.getDisplayName())
    }

    @Test
    fun `getFullDisplayName with NONE grouping returns base name`() {
        val sort = TaskSort(SortKey.CREATED_AT, SortDirection.DESC, CompletedGrouping.NONE)
        assertEquals("Created: Newest first", sort.getFullDisplayName())
    }

    @Test
    fun `getFullDisplayName with COMPLETED_FIRST appends suffix`() {
        val sort = TaskSort(SortKey.CREATED_AT, SortDirection.DESC, CompletedGrouping.COMPLETED_FIRST)
        assertEquals("Created: Newest first (Completed first)", sort.getFullDisplayName())
    }

    @Test
    fun `getFullDisplayName with COMPLETED_LAST appends suffix`() {
        val sort = TaskSort(SortKey.TITLE, SortDirection.ASC, CompletedGrouping.COMPLETED_LAST)
        assertEquals("Title: A\u2013Z (Completed last)", sort.getFullDisplayName())
    }

    @Test
    fun `default TaskSort has expected defaults`() {
        val sort = TaskSort()
        assertEquals(SortKey.CREATED_AT, sort.key)
        assertEquals(SortDirection.DESC, sort.direction)
        assertEquals(CompletedGrouping.NONE, sort.completedGrouping)
    }
}
