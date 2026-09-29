package dev.tuandoan.tasktracker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InlineSubtaskListTest {

    @Test
    fun `sanitizeSubtaskTitle trims leading and trailing whitespace`() {
        val input = "   Buy groceries   "
        val expected = "Buy groceries"
        assertEquals(expected, sanitizeSubtaskTitle(input))
    }

    @Test
    fun `sanitizeSubtaskTitle returns null for empty string`() {
        assertNull(sanitizeSubtaskTitle(""))
    }

    @Test
    fun `sanitizeSubtaskTitle returns null for whitespace-only string`() {
        assertNull(sanitizeSubtaskTitle("   \t\n  "))
    }

    @Test
    fun `sanitizeSubtaskTitle preserves internal spacing`() {
        val input = "Write  unit  tests"
        assertEquals("Write  unit  tests", sanitizeSubtaskTitle(input))
    }
}
