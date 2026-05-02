package dev.tuandoan.tasktracker.ui.components

import dev.tuandoan.tasktracker.domain.model.DayDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * JVM unit tests for the pure-Kotlin helpers in [DayCell] (CAL-12). Composable rendering
 * itself is not covered — Compose UI tests are tracked as CAL-32 / ST-12 and not yet set up
 * for this project.
 */
class DayCellTest {

    private val day = LocalDate.of(2026, 5, 10)

    private fun decoration(
        priorityBuckets: Set<Int> = emptySet(),
        taskCount: Int = priorityBuckets.size,
        completedCount: Int = 0,
        hasProjection: Boolean = false,
    ): DayDecoration = DayDecoration(
        date = day,
        taskCount = taskCount,
        priorityBuckets = priorityBuckets,
        completedCount = completedCount,
        hasRecurringProjection = hasProjection,
    )

    @Test
    fun `dotsFor returns priorities sorted descending`() {
        val dots = dotsFor(decoration(priorityBuckets = setOf(0, 2, 1)))
        assertEquals(listOf(2, 1, 0), dots)
    }

    @Test
    fun `dotsFor caps at three even when more buckets present`() {
        // Not reachable today (only 0/1/2 priorities exist), but guards future changes.
        val dots = dotsFor(decoration(priorityBuckets = setOf(0, 1, 2, 3, 5)))
        assertEquals(3, dots.size)
        // Highest three, descending.
        assertEquals(listOf(5, 3, 2), dots)
    }

    @Test
    fun `dotsFor returns empty list for empty priority buckets`() {
        val dots = dotsFor(decoration(priorityBuckets = emptySet()))
        assertTrue(dots.isEmpty())
    }

    @Test
    fun `dotsFor returns single-element list for a single bucket`() {
        val dots = dotsFor(decoration(priorityBuckets = setOf(1)))
        assertEquals(listOf(1), dots)
    }

    @Test
    fun `hasDotOverflow false for zero buckets`() {
        assertFalse(hasDotOverflow(decoration(priorityBuckets = emptySet())))
    }

    @Test
    fun `hasDotOverflow false for three buckets`() {
        assertFalse(hasDotOverflow(decoration(priorityBuckets = setOf(0, 1, 2))))
    }

    @Test
    fun `hasDotOverflow true when more than three buckets`() {
        assertTrue(hasDotOverflow(decoration(priorityBuckets = setOf(0, 1, 2, 3))))
    }
}
