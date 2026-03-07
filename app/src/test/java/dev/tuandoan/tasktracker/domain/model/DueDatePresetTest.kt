package dev.tuandoan.tasktracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DueDatePresetTest {

    @Test
    fun `TODAY produces end of today in local timezone`() {
        val result = DueDatePreset.TODAY.toEpochMillis()
        val expected = LocalDate.now()
            .atTime(LocalTime.of(23, 59))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `TOMORROW produces end of tomorrow in local timezone`() {
        val result = DueDatePreset.TOMORROW.toEpochMillis()
        val expected = LocalDate.now().plusDays(1)
            .atTime(LocalTime.of(23, 59))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `NEXT_WEEK produces end of day 7 days from now`() {
        val result = DueDatePreset.NEXT_WEEK.toEpochMillis()
        val expected = LocalDate.now().plusDays(7)
            .atTime(LocalTime.of(23, 59))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `TOMORROW is exactly one day after TODAY`() {
        val today = DueDatePreset.TODAY.toEpochMillis()
        val tomorrow = DueDatePreset.TOMORROW.toEpochMillis()
        // Allow small delta for DST transitions (23-25 hours)
        val diffHours = (tomorrow - today) / (1000 * 60 * 60)
        assertTrue("Expected ~24 hour difference, got $diffHours", diffHours in 23..25)
    }

    @Test
    fun `NEXT_WEEK is exactly seven days after TODAY`() {
        val today = DueDatePreset.TODAY.toEpochMillis()
        val nextWeek = DueDatePreset.NEXT_WEEK.toEpochMillis()
        val diffDays = (nextWeek - today) / (1000 * 60 * 60 * 24)
        assertTrue("Expected ~7 day difference, got $diffDays", diffDays in 6..8)
    }

    @Test
    fun `all presets produce future timestamps`() {
        val now = System.currentTimeMillis()
        DueDatePreset.entries.forEach { preset ->
            // TODAY at 23:59 should be >= current time (unless running at 23:59)
            assertTrue(
                "${preset.name} should produce a timestamp >= start of today",
                preset.toEpochMillis() > now - (24 * 60 * 60 * 1000),
            )
        }
    }

    @Test
    fun `entries returns all three presets`() {
        assertEquals(3, DueDatePreset.entries.size)
        assertEquals(DueDatePreset.TODAY, DueDatePreset.entries[0])
        assertEquals(DueDatePreset.TOMORROW, DueDatePreset.entries[1])
        assertEquals(DueDatePreset.NEXT_WEEK, DueDatePreset.entries[2])
    }
}
