package dev.tuandoan.tasktracker.domain.service

import dev.tuandoan.tasktracker.domain.model.RecurrenceType
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * Covers [RecurrenceCalculator.projectOccurrences] for CAL-07. Kept separate from
 * [RecurrenceCalculatorTest] so the pre-existing single-next-date suite stays stable.
 */
class RecurrenceCalculatorProjectionTest {

    private val zone = ZoneId.systemDefault()

    private fun epoch(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun bitmask(vararg days: DayOfWeek): Int =
        days.fold(0) { acc, day -> acc or RecurrenceCalculator.dayOfWeekToBitmask(day) }

    private fun recurringTask(
        baseDate: LocalDate,
        type: RecurrenceType,
        interval: Int = 1,
        daysOfWeek: Int = 0,
        endDate: LocalDate? = null,
    ) = TestTaskFactory.createTask(
        dueAt = epoch(baseDate),
        recurrenceType = type.value,
        recurrenceInterval = interval,
        recurrenceDaysOfWeek = daysOfWeek,
        recurrenceEndDate = endDate?.let(::epoch),
    )

    // ── Degenerate inputs ──

    @Test
    fun `NONE type yields empty list`() {
        val task = TestTaskFactory.createTask(dueAt = epoch(LocalDate.of(2026, 5, 1)))
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = LocalDate.of(2026, 5, 1),
            windowEnd = LocalDate.of(2026, 5, 31),
            zone = zone,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `null dueAt yields empty list`() {
        val task = TestTaskFactory.createTask(dueAt = null, recurrenceType = RecurrenceType.DAILY.value)
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = LocalDate.of(2026, 5, 1),
            windowEnd = LocalDate.of(2026, 5, 31),
            zone = zone,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `inverted window yields empty list`() {
        val task = recurringTask(LocalDate.of(2026, 5, 1), RecurrenceType.DAILY)
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = LocalDate.of(2026, 5, 31),
            windowEnd = LocalDate.of(2026, 5, 1),
            zone = zone,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `base date after window end yields empty list`() {
        val task = recurringTask(LocalDate.of(2026, 6, 15), RecurrenceType.DAILY)
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = LocalDate.of(2026, 5, 1),
            windowEnd = LocalDate.of(2026, 5, 31),
            zone = zone,
        )
        assertTrue(result.isEmpty())
    }

    // ── DAILY ──

    @Test
    fun `DAILY with base inside window enumerates each day`() {
        val task = recurringTask(LocalDate.of(2026, 5, 10), RecurrenceType.DAILY)
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = LocalDate.of(2026, 5, 10),
            windowEnd = LocalDate.of(2026, 5, 14),
            zone = zone,
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 11),
                LocalDate.of(2026, 5, 12),
                LocalDate.of(2026, 5, 13),
                LocalDate.of(2026, 5, 14),
            ),
            result,
        )
    }

    @Test
    fun `DAILY with base before window fast-forwards to first in-window occurrence`() {
        val task = recurringTask(LocalDate.of(2026, 4, 20), RecurrenceType.DAILY)
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = LocalDate.of(2026, 5, 1),
            windowEnd = LocalDate.of(2026, 5, 3),
            zone = zone,
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 5, 3),
            ),
            result,
        )
    }

    @Test
    fun `DAILY with interval 3 emits every third day`() {
        val task = recurringTask(LocalDate.of(2026, 5, 1), RecurrenceType.DAILY, interval = 3)
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = LocalDate.of(2026, 5, 1),
            windowEnd = LocalDate.of(2026, 5, 14),
            zone = zone,
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 4),
                LocalDate.of(2026, 5, 7),
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 13),
            ),
            result,
        )
    }

    // ── WEEKLY ──

    @Test
    fun `WEEKLY Mon Wed Fri over a two-week window`() {
        // Base: Mon 2026-05-04
        val task = recurringTask(
            baseDate = LocalDate.of(2026, 5, 4),
            type = RecurrenceType.WEEKLY,
            daysOfWeek = bitmask(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        )
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = LocalDate.of(2026, 5, 4),
            windowEnd = LocalDate.of(2026, 5, 17),
            zone = zone,
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 5, 4), // Mon
                LocalDate.of(2026, 5, 6), // Wed
                LocalDate.of(2026, 5, 8), // Fri
                LocalDate.of(2026, 5, 11),
                LocalDate.of(2026, 5, 13),
                LocalDate.of(2026, 5, 15),
            ),
            result,
        )
    }

    @Test
    fun `WEEKLY with no bitmask and interval 2 emits every second week`() {
        val task = recurringTask(
            baseDate = LocalDate.of(2026, 5, 1), // Fri
            type = RecurrenceType.WEEKLY,
            interval = 2,
        )
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = LocalDate.of(2026, 5, 1),
            windowEnd = LocalDate.of(2026, 6, 15),
            zone = zone,
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 15),
                LocalDate.of(2026, 5, 29),
                LocalDate.of(2026, 6, 12),
            ),
            result,
        )
    }

    // ── MONTHLY & YEARLY edge cases ──

    @Test
    fun `MONTHLY Jan 31 base clamps to Feb 28 on non-leap year`() {
        val task = recurringTask(LocalDate.of(2026, 1, 31), RecurrenceType.MONTHLY)
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = LocalDate.of(2026, 1, 1),
            windowEnd = LocalDate.of(2026, 4, 30),
            zone = zone,
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28), // clamped
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30), // clamped (30 days)
            ),
            result,
        )
    }

    @Test
    fun `YEARLY Feb 29 clamps to Feb 28 on non-leap years`() {
        val task = recurringTask(LocalDate.of(2024, 2, 29), RecurrenceType.YEARLY)
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = LocalDate.of(2024, 1, 1),
            windowEnd = LocalDate.of(2028, 12, 31),
            zone = zone,
        )
        assertEquals(
            listOf(
                LocalDate.of(2024, 2, 29),
                LocalDate.of(2025, 2, 28), // clamped
                LocalDate.of(2026, 2, 28), // clamped
                LocalDate.of(2027, 2, 28), // clamped
                LocalDate.of(2028, 2, 29), // leap again
            ),
            result,
        )
    }

    // ── End-date truncation ──

    @Test
    fun `recurrenceEndDate before windowEnd truncates enumeration`() {
        val task = recurringTask(
            baseDate = LocalDate.of(2026, 5, 1),
            type = RecurrenceType.DAILY,
            endDate = LocalDate.of(2026, 5, 3),
        )
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = LocalDate.of(2026, 5, 1),
            windowEnd = LocalDate.of(2026, 5, 31),
            zone = zone,
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 5, 3),
            ),
            result,
        )
    }

    @Test
    fun `recurrenceEndDate before windowStart yields empty list`() {
        val task = recurringTask(
            baseDate = LocalDate.of(2026, 4, 1),
            type = RecurrenceType.DAILY,
            endDate = LocalDate.of(2026, 4, 15),
        )
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = LocalDate.of(2026, 5, 1),
            windowEnd = LocalDate.of(2026, 5, 31),
            zone = zone,
        )
        assertTrue(result.isEmpty())
    }

    // ── Cap ──

    @Test
    fun `maxOccurrences cap honored for pathological DAILY window`() {
        val task = recurringTask(LocalDate.of(2026, 1, 1), RecurrenceType.DAILY)
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = LocalDate.of(2026, 1, 1),
            windowEnd = LocalDate.of(2026, 12, 31),
            zone = zone,
            maxOccurrences = 10,
        )
        assertEquals(10, result.size)
        assertEquals(LocalDate.of(2026, 1, 1), result.first())
        assertEquals(LocalDate.of(2026, 1, 10), result.last())
    }

    // ── Single-day window ──

    @Test
    fun `single-day window includes base date that matches`() {
        val task = recurringTask(LocalDate.of(2026, 5, 15), RecurrenceType.DAILY)
        val target = LocalDate.of(2026, 5, 15)
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = target,
            windowEnd = target,
            zone = zone,
        )
        assertEquals(listOf(target), result)
    }

    @Test
    fun `single-day window excludes base if rule does not fall on that day`() {
        val task = recurringTask(LocalDate.of(2026, 5, 1), RecurrenceType.DAILY, interval = 7)
        val target = LocalDate.of(2026, 5, 10) // base + 9 days — not a multiple of 7 from base
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = target,
            windowEnd = target,
            zone = zone,
        )
        assertTrue(result.isEmpty())
    }

    // ── Fast-forward cap ──

    @Test
    fun `DAILY base far before window returns empty if fast-forward cap is hit`() {
        // Year 1900 base, 2026 window → ~46,000 daily steps, well over the 10,000 cap.
        val task = recurringTask(LocalDate.of(1900, 1, 1), RecurrenceType.DAILY)
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = LocalDate.of(2026, 5, 1),
            windowEnd = LocalDate.of(2026, 5, 31),
            zone = zone,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `DAILY base moderately before window still reaches the window`() {
        // 3 years before the window → ~1,095 daily steps, well under the cap.
        val task = recurringTask(LocalDate.of(2023, 5, 1), RecurrenceType.DAILY)
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = LocalDate.of(2026, 5, 1),
            windowEnd = LocalDate.of(2026, 5, 3),
            zone = zone,
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 5, 3),
            ),
            result,
        )
    }

    // ── Realistic 6-week window sanity check ──

    @Test
    fun `DAILY over 42-day window produces exactly 42 occurrences`() {
        val task = recurringTask(LocalDate.of(2026, 5, 1), RecurrenceType.DAILY)
        val result = RecurrenceCalculator.projectOccurrences(
            task = task,
            windowStart = LocalDate.of(2026, 5, 1),
            windowEnd = LocalDate.of(2026, 6, 11), // +41 days, inclusive
            zone = zone,
        )
        assertEquals(42, result.size)
    }
}
