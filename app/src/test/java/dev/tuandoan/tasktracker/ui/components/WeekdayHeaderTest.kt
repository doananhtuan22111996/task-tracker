package dev.tuandoan.tasktracker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.util.Locale

/**
 * JVM tests for [weekdayShortNames] — the pure helper powering [WeekdayHeader] (CAL-13).
 * Locks the two guarantees callers care about: correct rotation order from an arbitrary
 * first-day, and locale-appropriate short names.
 */
class WeekdayHeaderTest {

    @Test
    fun `Monday-first US locale returns Mon through Sun in order`() {
        val names = weekdayShortNames(DayOfWeek.MONDAY, Locale.US)

        assertEquals(7, names.size)
        // java.time US short names are "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun".
        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), names)
    }

    @Test
    fun `Sunday-first US locale starts with Sun`() {
        val names = weekdayShortNames(DayOfWeek.SUNDAY, Locale.US)

        assertEquals("Sun", names.first())
        assertEquals("Sat", names.last())
        assertEquals(listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"), names)
    }

    @Test
    fun `Saturday-first Arabic-style rotation starts with Saturday`() {
        // Arabic locales commonly use Saturday as the first day of week. Locale fallback for
        // Arabic display names is available in the JVM default; assert rotation rather than
        // exact text since names depend on the test JVM's ICU data.
        val names = weekdayShortNames(DayOfWeek.SATURDAY, Locale.US)

        assertEquals(7, names.size)
        assertEquals(listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri"), names)
    }

    @Test
    fun `German locale returns localized short names, Monday-first`() {
        val names = weekdayShortNames(DayOfWeek.MONDAY, Locale.GERMAN)

        assertEquals(7, names.size)
        // German short day names start with "Mo." in Java 17 ICU data. Assert only the
        // first letter to stay portable across ICU versions.
        assertEquals("M", names[0].first().toString())
        assertEquals("D", names[1].first().toString()) // Dienstag
    }

    @Test
    fun `helper always returns exactly 7 entries regardless of firstDayOfWeek`() {
        DayOfWeek.values().forEach { start ->
            val names = weekdayShortNames(start, Locale.US)
            assertEquals("Expected 7 entries for first day $start", 7, names.size)
        }
    }
}
