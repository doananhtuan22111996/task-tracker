package dev.tuandoan.tasktracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DayDecorationTest {

    private val may12 = LocalDate.of(2026, 5, 12)
    private val may13 = LocalDate.of(2026, 5, 13)

    @Test
    fun `data class equality holds on identical content`() {
        val a = DayDecoration(
            date = may12,
            taskCount = 3,
            priorityBuckets = setOf(0, 1, 2),
            completedCount = 1,
            hasRecurringProjection = false,
        )
        val b = DayDecoration(
            date = may12,
            taskCount = 3,
            priorityBuckets = setOf(0, 1, 2),
            completedCount = 1,
            hasRecurringProjection = false,
        )
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `decorations on different dates are not equal`() {
        val a =
            DayDecoration(
                may12,
                taskCount = 1,
                priorityBuckets = setOf(1),
                completedCount = 0,
                hasRecurringProjection = false,
            )
        val b =
            DayDecoration(
                may13,
                taskCount = 1,
                priorityBuckets = setOf(1),
                completedCount = 0,
                hasRecurringProjection = false,
            )
        assertNotEquals(a, b)
    }

    @Test
    fun `priority buckets preserves distinct priority ints`() {
        val decoration = DayDecoration(
            date = may12,
            taskCount = 5,
            priorityBuckets = setOf(0, 1, 2),
            completedCount = 2,
            hasRecurringProjection = true,
        )
        assertTrue(0 in decoration.priorityBuckets)
        assertTrue(1 in decoration.priorityBuckets)
        assertTrue(2 in decoration.priorityBuckets)
        assertEquals(3, decoration.priorityBuckets.size)
    }

    @Test
    fun `empty priority buckets is valid for projection-only day`() {
        val decoration = DayDecoration(
            date = may12,
            taskCount = 0,
            priorityBuckets = emptySet(),
            completedCount = 0,
            hasRecurringProjection = true,
        )
        assertTrue(decoration.priorityBuckets.isEmpty())
        assertTrue(decoration.hasRecurringProjection)
    }
}
