package dev.tuandoan.tasktracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PriorityTest {

    @Test
    fun `fromValue 0 returns LOW`() {
        assertEquals(Priority.LOW, Priority.fromValue(0))
    }

    @Test
    fun `fromValue 1 returns MEDIUM`() {
        assertEquals(Priority.MEDIUM, Priority.fromValue(1))
    }

    @Test
    fun `fromValue 2 returns HIGH`() {
        assertEquals(Priority.HIGH, Priority.fromValue(2))
    }

    @Test
    fun `fromValue with negative returns MEDIUM`() {
        assertEquals(Priority.MEDIUM, Priority.fromValue(-1))
    }

    @Test
    fun `fromValue with out-of-range returns MEDIUM`() {
        assertEquals(Priority.MEDIUM, Priority.fromValue(99))
    }

    @Test
    fun `each priority has correct int value`() {
        assertEquals(0, Priority.LOW.value)
        assertEquals(1, Priority.MEDIUM.value)
        assertEquals(2, Priority.HIGH.value)
    }

    @Test
    fun `each priority has non-empty displayName`() {
        assertEquals("Low", Priority.LOW.displayName)
        assertEquals("Medium", Priority.MEDIUM.displayName)
        assertEquals("High", Priority.HIGH.displayName)
    }
}
