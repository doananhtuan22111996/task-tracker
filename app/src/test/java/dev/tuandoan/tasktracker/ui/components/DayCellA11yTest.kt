package dev.tuandoan.tasktracker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM tests for [buildDayCellContentDescription] (CAL-28 / CAL-30). Locks the grammar shape
 * and edge cases for TalkBack announcements on calendar day cells.
 */
class DayCellA11yTest {

    @Test
    fun `date-only description returns just the date text`() {
        val result = buildDayCellContentDescription(dateText = "Tuesday, May 12")

        assertEquals("Tuesday, May 12", result)
    }

    @Test
    fun `date plus task count joins with comma-space`() {
        val result = buildDayCellContentDescription(
            dateText = "Tuesday, May 12",
            taskCountText = "3 tasks",
        )

        assertEquals("Tuesday, May 12, 3 tasks", result)
    }

    @Test
    fun `full description includes all tokens in order`() {
        val result = buildDayCellContentDescription(
            dateText = "Tuesday, May 12",
            taskCountText = "3 tasks",
            highPriorityText = "with high priority",
            isToday = true,
            isSelected = true,
            todayPrefix = "Today",
            selectedSuffix = "selected",
        )

        assertEquals("Today, Tuesday, May 12, 3 tasks, with high priority, selected", result)
    }

    @Test
    fun `today-only prefixes with Today`() {
        val result = buildDayCellContentDescription(
            dateText = "Tuesday, May 12",
            isToday = true,
            todayPrefix = "Today",
        )

        assertEquals("Today, Tuesday, May 12", result)
    }

    @Test
    fun `selected-only suffixes with selected`() {
        val result = buildDayCellContentDescription(
            dateText = "Tuesday, May 12",
            isSelected = true,
            selectedSuffix = "selected",
        )

        assertEquals("Tuesday, May 12, selected", result)
    }

    @Test
    fun `isToday without todayPrefix does not emit the prefix`() {
        // Defensive: caller forgot to resolve the string. Drop the flag rather than produce
        // a broken description.
        val result = buildDayCellContentDescription(
            dateText = "Tuesday, May 12",
            isToday = true,
            todayPrefix = null,
        )

        assertEquals("Tuesday, May 12", result)
    }

    @Test
    fun `isSelected without selectedSuffix does not emit the suffix`() {
        val result = buildDayCellContentDescription(
            dateText = "Tuesday, May 12",
            isSelected = true,
            selectedSuffix = null,
        )

        assertEquals("Tuesday, May 12", result)
    }

    @Test
    fun `highPriorityText null means no high-priority token`() {
        val result = buildDayCellContentDescription(
            dateText = "Tuesday, May 12",
            taskCountText = "3 tasks",
            highPriorityText = null,
        )

        assertEquals("Tuesday, May 12, 3 tasks", result)
    }

    @Test
    fun `taskCountText null with highPriorityText null yields date-only`() {
        val result = buildDayCellContentDescription(
            dateText = "Tuesday, May 12",
            taskCountText = null,
            highPriorityText = null,
        )

        assertEquals("Tuesday, May 12", result)
    }

    @Test
    fun `today and selected with no tasks still composes correctly`() {
        val result = buildDayCellContentDescription(
            dateText = "Tuesday, May 12",
            isToday = true,
            isSelected = true,
            todayPrefix = "Today",
            selectedSuffix = "selected",
        )

        assertEquals("Today, Tuesday, May 12, selected", result)
    }
}
