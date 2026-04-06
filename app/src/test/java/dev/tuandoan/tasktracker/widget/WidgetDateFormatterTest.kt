package dev.tuandoan.tasktracker.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WidgetDateFormatterTest {

    // Fixed "now" = 2025-06-15 12:00:00 UTC
    private val fixedNow: Long = Calendar.getInstance().apply {
        set(2025, Calendar.JUNE, 15, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun format(dueAt: Long?): String? = formatDueDate(dueAt) { fixedNow }

    @Test
    fun `null input returns null`() {
        assertNull(format(null))
    }

    @Test
    fun `past timestamp returns Overdue`() {
        val yesterday = Calendar.getInstance().apply {
            set(2025, Calendar.JUNE, 14, 10, 0, 0)
        }.timeInMillis
        assertEquals("Overdue", format(yesterday))
    }

    @Test
    fun `timestamp later today returns Today`() {
        val todayEvening = Calendar.getInstance().apply {
            set(2025, Calendar.JUNE, 15, 18, 0, 0)
        }.timeInMillis
        assertEquals("Today", format(todayEvening))
    }

    @Test
    fun `timestamp tomorrow returns Tomorrow`() {
        val tomorrowNoon = Calendar.getInstance().apply {
            set(2025, Calendar.JUNE, 16, 12, 0, 0)
        }.timeInMillis
        assertEquals("Tomorrow", format(tomorrowNoon))
    }

    @Test
    fun `timestamp far future returns MMM dd format`() {
        val futureDate = Calendar.getInstance().apply {
            set(2025, Calendar.JULY, 10, 12, 0, 0)
        }.timeInMillis
        val expected = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(futureDate))
        assertEquals(expected, format(futureDate))
    }
}
