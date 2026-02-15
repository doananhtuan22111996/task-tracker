package dev.tuandoan.tasktracker.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {

    @Test
    fun `formatDate produces non-empty string`() {
        val result = formatDate(1700000000000L) // Nov 14, 2023
        assertTrue(result.isNotBlank())
        assertTrue(result.contains("2023"))
    }

    @Test
    fun `formatDueDate produces non-empty string with time`() {
        val result = formatDueDate(1700000000000L)
        assertTrue(result.isNotBlank())
        // Should contain "at" separator between date and time
        assertTrue(result.contains("at"))
    }

    @Test
    fun `isOverdue returns true for past timestamp`() {
        val pastTimestamp = System.currentTimeMillis() - 86_400_000L // yesterday
        assertTrue(isOverdue(pastTimestamp))
    }

    @Test
    fun `isOverdue returns false for future timestamp`() {
        val futureTimestamp = System.currentTimeMillis() + 86_400_000L // tomorrow
        assertFalse(isOverdue(futureTimestamp))
    }
}
