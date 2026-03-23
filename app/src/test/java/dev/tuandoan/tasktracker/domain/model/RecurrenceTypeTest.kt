package dev.tuandoan.tasktracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RecurrenceTypeTest {

    @Test
    fun `fromValue 0 returns NONE`() {
        assertEquals(RecurrenceType.NONE, RecurrenceType.fromValue(0))
    }

    @Test
    fun `fromValue 1 returns DAILY`() {
        assertEquals(RecurrenceType.DAILY, RecurrenceType.fromValue(1))
    }

    @Test
    fun `fromValue 2 returns WEEKLY`() {
        assertEquals(RecurrenceType.WEEKLY, RecurrenceType.fromValue(2))
    }

    @Test
    fun `fromValue 3 returns MONTHLY`() {
        assertEquals(RecurrenceType.MONTHLY, RecurrenceType.fromValue(3))
    }

    @Test
    fun `fromValue 4 returns YEARLY`() {
        assertEquals(RecurrenceType.YEARLY, RecurrenceType.fromValue(4))
    }

    @Test
    fun `fromValue with negative returns NONE`() {
        assertEquals(RecurrenceType.NONE, RecurrenceType.fromValue(-1))
    }

    @Test
    fun `fromValue with out-of-range returns NONE`() {
        assertEquals(RecurrenceType.NONE, RecurrenceType.fromValue(99))
    }

    @Test
    fun `each recurrence type has correct int value`() {
        assertEquals(0, RecurrenceType.NONE.value)
        assertEquals(1, RecurrenceType.DAILY.value)
        assertEquals(2, RecurrenceType.WEEKLY.value)
        assertEquals(3, RecurrenceType.MONTHLY.value)
        assertEquals(4, RecurrenceType.YEARLY.value)
    }

    @Test
    fun `entries count is 5`() {
        assertEquals(5, RecurrenceType.entries.size)
    }
}
