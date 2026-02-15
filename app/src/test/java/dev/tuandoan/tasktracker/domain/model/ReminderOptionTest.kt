package dev.tuandoan.tasktracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderOptionTest {

    @Test
    fun `fromOffsetMinutes returns NONE for null`() {
        assertEquals(ReminderOption.NONE, ReminderOption.fromOffsetMinutes(null))
    }

    @Test
    fun `fromOffsetMinutes returns NONE for zero`() {
        assertEquals(ReminderOption.NONE, ReminderOption.fromOffsetMinutes(0))
    }

    @Test
    fun `fromOffsetMinutes returns MINUTES_1 for 1`() {
        assertEquals(ReminderOption.MINUTES_1, ReminderOption.fromOffsetMinutes(1))
    }

    @Test
    fun `fromOffsetMinutes returns MINUTES_5 for 5`() {
        assertEquals(ReminderOption.MINUTES_5, ReminderOption.fromOffsetMinutes(5))
    }

    @Test
    fun `fromOffsetMinutes returns HOURS_1 for 60`() {
        assertEquals(ReminderOption.HOURS_1, ReminderOption.fromOffsetMinutes(60))
    }

    @Test
    fun `fromOffsetMinutes returns DAYS_1 for 1440`() {
        assertEquals(ReminderOption.DAYS_1, ReminderOption.fromOffsetMinutes(24 * 60))
    }

    @Test
    fun `fromOffsetMinutes returns NONE for unrecognized value`() {
        assertEquals(ReminderOption.NONE, ReminderOption.fromOffsetMinutes(42))
    }

    @Test
    fun `getSelectableOptions excludes NONE`() {
        val options = ReminderOption.getSelectableOptions()
        assertEquals(4, options.size)
        assertEquals(false, options.contains(ReminderOption.NONE))
    }

    @Test
    fun `each option has correct offsetMinutes`() {
        assertEquals(0, ReminderOption.NONE.offsetMinutes)
        assertEquals(1, ReminderOption.MINUTES_1.offsetMinutes)
        assertEquals(5, ReminderOption.MINUTES_5.offsetMinutes)
        assertEquals(60, ReminderOption.HOURS_1.offsetMinutes)
        assertEquals(1440, ReminderOption.DAYS_1.offsetMinutes)
    }

    @Test
    fun `each option has non-empty displayName`() {
        for (option in ReminderOption.entries) {
            assertEquals(true, option.displayName.isNotBlank())
        }
    }
}
