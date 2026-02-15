package dev.tuandoan.tasktracker.ui.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskSelectionStateManagerTest {

    private lateinit var manager: TaskSelectionStateManager

    @Before
    fun setup() {
        manager = TaskSelectionStateManager()
    }

    // === initial state ===

    @Test
    fun `initial state has no selection`() {
        assertFalse(manager.hasSelection())
        assertEquals(0, manager.getSelectionCount())
        assertTrue(manager.getSelectedIds().isEmpty())
    }

    // === enterSelection ===

    @Test
    fun `enterSelection sets single task as selected`() {
        manager.enterSelection(5L)

        assertTrue(manager.hasSelection())
        assertEquals(1, manager.getSelectionCount())
        assertTrue(manager.isSelected(5L))
    }

    @Test
    fun `enterSelection replaces existing selection`() {
        manager.enterSelection(1L)
        manager.enterSelection(2L)

        assertEquals(1, manager.getSelectionCount())
        assertFalse(manager.isSelected(1L))
        assertTrue(manager.isSelected(2L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `enterSelection with zero id throws`() {
        manager.enterSelection(0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `enterSelection with negative id throws`() {
        manager.enterSelection(-1L)
    }

    // === toggleSelection ===

    @Test
    fun `toggleSelection adds task to selection`() {
        manager.enterSelection(1L)
        manager.toggleSelection(2L)

        assertEquals(2, manager.getSelectionCount())
        assertTrue(manager.isSelected(1L))
        assertTrue(manager.isSelected(2L))
    }

    @Test
    fun `toggleSelection removes task from selection`() {
        manager.enterSelection(1L)
        manager.toggleSelection(2L)
        manager.toggleSelection(1L) // Remove 1

        assertEquals(1, manager.getSelectionCount())
        assertFalse(manager.isSelected(1L))
        assertTrue(manager.isSelected(2L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toggleSelection with zero id throws`() {
        manager.toggleSelection(0L)
    }

    // === clearSelection ===

    @Test
    fun `clearSelection empties selection`() {
        manager.enterSelection(1L)
        manager.toggleSelection(2L)

        manager.clearSelection()

        assertFalse(manager.hasSelection())
        assertEquals(0, manager.getSelectionCount())
    }

    @Test
    fun `clearSelection on empty selection is safe`() {
        manager.clearSelection()
        assertFalse(manager.hasSelection())
    }

    // === selectAll ===

    @Test
    fun `selectAll sets all provided ids`() {
        manager.selectAll(listOf(1L, 2L, 3L))

        assertEquals(3, manager.getSelectionCount())
        assertTrue(manager.isSelected(1L))
        assertTrue(manager.isSelected(2L))
        assertTrue(manager.isSelected(3L))
    }

    @Test
    fun `selectAll replaces existing selection`() {
        manager.enterSelection(99L)
        manager.selectAll(listOf(1L, 2L))

        assertEquals(2, manager.getSelectionCount())
        assertFalse(manager.isSelected(99L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `selectAll with empty list throws`() {
        manager.selectAll(emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `selectAll with duplicates throws`() {
        manager.selectAll(listOf(1L, 2L, 1L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `selectAll with invalid ids throws`() {
        manager.selectAll(listOf(1L, -1L))
    }

    // === validateSelection ===

    @Test
    fun `validateSelection returns Empty when nothing selected`() {
        val result = manager.validateSelection()
        assertTrue(result is SelectionValidationResult.Empty)
    }

    @Test
    fun `validateSelection returns SingleItem for one selection`() {
        manager.enterSelection(7L)

        val result = manager.validateSelection()

        assertTrue(result is SelectionValidationResult.SingleItem)
        assertEquals(7L, (result as SelectionValidationResult.SingleItem).taskId)
    }

    @Test
    fun `validateSelection returns MultipleItems for multiple selections`() {
        manager.selectAll(listOf(1L, 2L, 3L))

        val result = manager.validateSelection()

        assertTrue(result is SelectionValidationResult.MultipleItems)
        assertEquals(3, (result as SelectionValidationResult.MultipleItems).taskIds.size)
    }

    // === isSelected ===

    @Test
    fun `isSelected returns false for unselected task`() {
        assertFalse(manager.isSelected(99L))
    }

    // === StateFlow reactivity ===

    @Test
    fun `selectedIds StateFlow reflects changes`() {
        assertEquals(emptySet<Long>(), manager.selectedIds.value)

        manager.enterSelection(1L)
        assertEquals(setOf(1L), manager.selectedIds.value)

        manager.toggleSelection(2L)
        assertEquals(setOf(1L, 2L), manager.selectedIds.value)

        manager.clearSelection()
        assertEquals(emptySet<Long>(), manager.selectedIds.value)
    }
}
