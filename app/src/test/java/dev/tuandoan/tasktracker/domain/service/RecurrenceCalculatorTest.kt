package dev.tuandoan.tasktracker.domain.service

import dev.tuandoan.tasktracker.domain.model.RecurrenceType
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RecurrenceCalculatorTest {

    private val zone = ZoneId.systemDefault()

    // ── Helper functions ──

    private fun dateToEpoch(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun dateTimeToEpoch(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun epochToDate(epoch: Long): LocalDate = Instant.ofEpochMilli(epoch).atZone(zone).toLocalDate()

    private fun epochToDateTime(epoch: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), zone)

    private fun bitmask(vararg days: DayOfWeek): Int =
        days.fold(0) { acc, day -> acc or RecurrenceCalculator.dayOfWeekToBitmask(day) }

    private fun recurringTask(
        dueAt: Long,
        type: RecurrenceType,
        interval: Int = 1,
        daysOfWeek: Int = 0,
        endDate: Long? = null,
        dueAtHasTime: Boolean = false,
    ) = TestTaskFactory.createTask(
        dueAt = dueAt,
        dueAtHasTime = dueAtHasTime,
        recurrenceType = type.value,
        recurrenceInterval = interval,
        recurrenceDaysOfWeek = daysOfWeek,
        recurrenceEndDate = endDate,
    )

    // ── NONE type ──

    @Test
    fun `NONE recurrence returns null`() {
        val task = recurringTask(dueAt = dateToEpoch(2026, 3, 27), type = RecurrenceType.NONE)
        assertNull(RecurrenceCalculator.nextDueDate(task))
    }

    @Test
    fun `no due date returns null`() {
        val task = TestTaskFactory.createTask(
            dueAt = null,
            recurrenceType = RecurrenceType.DAILY.value,
        )
        assertNull(RecurrenceCalculator.nextDueDate(task))
    }

    // ── DAILY ──

    @Test
    fun `daily interval 1`() {
        val task = recurringTask(dueAt = dateToEpoch(2026, 3, 27), type = RecurrenceType.DAILY)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2026, 3, 28), epochToDate(next))
    }

    @Test
    fun `daily interval 3`() {
        val task = recurringTask(dueAt = dateToEpoch(2026, 3, 27), type = RecurrenceType.DAILY, interval = 3)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2026, 3, 30), epochToDate(next))
    }

    @Test
    fun `daily interval 7`() {
        val task = recurringTask(dueAt = dateToEpoch(2026, 3, 27), type = RecurrenceType.DAILY, interval = 7)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2026, 4, 3), epochToDate(next))
    }

    @Test
    fun `daily crosses month boundary`() {
        val task = recurringTask(dueAt = dateToEpoch(2026, 3, 31), type = RecurrenceType.DAILY)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2026, 4, 1), epochToDate(next))
    }

    @Test
    fun `daily crosses year boundary`() {
        val task = recurringTask(dueAt = dateToEpoch(2026, 12, 31), type = RecurrenceType.DAILY)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2027, 1, 1), epochToDate(next))
    }

    @Test
    fun `daily preserves time`() {
        val task = recurringTask(
            dueAt = dateTimeToEpoch(2026, 3, 27, 14, 30),
            type = RecurrenceType.DAILY,
            dueAtHasTime = true,
        )
        val next = RecurrenceCalculator.nextDueDate(task)!!
        val nextDt = epochToDateTime(next)
        assertEquals(LocalDate.of(2026, 3, 28), nextDt.toLocalDate())
        assertEquals(14, nextDt.hour)
        assertEquals(30, nextDt.minute)
    }

    // ── WEEKLY (no bitmask) ──

    @Test
    fun `weekly no bitmask interval 1`() {
        // Thursday Mar 27
        val task = recurringTask(dueAt = dateToEpoch(2026, 3, 27), type = RecurrenceType.WEEKLY)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2026, 4, 3), epochToDate(next))
    }

    @Test
    fun `weekly no bitmask interval 2`() {
        val task = recurringTask(dueAt = dateToEpoch(2026, 3, 27), type = RecurrenceType.WEEKLY, interval = 2)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2026, 4, 10), epochToDate(next))
    }

    // ── WEEKLY (with bitmask) ──

    @Test
    fun `weekly Mon-Wed-Fri from Monday picks Wednesday`() {
        // Monday Mar 23
        val mask = bitmask(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val task = recurringTask(dueAt = dateToEpoch(2026, 3, 23), type = RecurrenceType.WEEKLY, daysOfWeek = mask)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2026, 3, 25), epochToDate(next)) // Wednesday
    }

    @Test
    fun `weekly Mon-Wed-Fri from Wednesday picks Friday`() {
        val mask = bitmask(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val task = recurringTask(dueAt = dateToEpoch(2026, 3, 25), type = RecurrenceType.WEEKLY, daysOfWeek = mask)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2026, 3, 27), epochToDate(next)) // Friday
    }

    @Test
    fun `weekly Mon-Wed-Fri from Friday wraps to next Monday`() {
        val mask = bitmask(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val task = recurringTask(dueAt = dateToEpoch(2026, 3, 27), type = RecurrenceType.WEEKLY, daysOfWeek = mask)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2026, 3, 30), epochToDate(next)) // Next Monday
    }

    @Test
    fun `weekly single day Sunday interval 1`() {
        val mask = bitmask(DayOfWeek.SUNDAY)
        // Sunday Mar 29
        val task = recurringTask(dueAt = dateToEpoch(2026, 3, 29), type = RecurrenceType.WEEKLY, daysOfWeek = mask)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2026, 4, 5), epochToDate(next)) // Next Sunday
    }

    @Test
    fun `weekly Mon-Wed-Fri interval 2 from Friday jumps 2 weeks to Monday`() {
        val mask = bitmask(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val task = recurringTask(
            dueAt = dateToEpoch(2026, 3, 27),
            type = RecurrenceType.WEEKLY,
            interval = 2,
            daysOfWeek = mask,
        )
        val next = RecurrenceCalculator.nextDueDate(task)!!
        // Friday → skip to Monday 2 weeks later (Apr 6)
        assertEquals(LocalDate.of(2026, 4, 6), epochToDate(next))
    }

    @Test
    fun `weekly with bitmask preserves time`() {
        val mask = bitmask(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
        val task = recurringTask(
            dueAt = dateTimeToEpoch(2026, 3, 23, 9, 0), // Monday 9:00
            type = RecurrenceType.WEEKLY,
            daysOfWeek = mask,
            dueAtHasTime = true,
        )
        val next = RecurrenceCalculator.nextDueDate(task)!!
        val nextDt = epochToDateTime(next)
        assertEquals(LocalDate.of(2026, 3, 27), nextDt.toLocalDate()) // Friday
        assertEquals(9, nextDt.hour)
        assertEquals(0, nextDt.minute)
    }

    // ── MONTHLY ──

    @Test
    fun `monthly interval 1`() {
        val task = recurringTask(dueAt = dateToEpoch(2026, 3, 15), type = RecurrenceType.MONTHLY)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2026, 4, 15), epochToDate(next))
    }

    @Test
    fun `monthly interval 3`() {
        val task = recurringTask(dueAt = dateToEpoch(2026, 1, 15), type = RecurrenceType.MONTHLY, interval = 3)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2026, 4, 15), epochToDate(next))
    }

    @Test
    fun `monthly Jan 31 to Feb 28 non-leap year`() {
        val task = recurringTask(dueAt = dateToEpoch(2026, 1, 31), type = RecurrenceType.MONTHLY)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2026, 2, 28), epochToDate(next))
    }

    @Test
    fun `monthly Jan 31 to Feb 29 leap year`() {
        val task = recurringTask(dueAt = dateToEpoch(2028, 1, 31), type = RecurrenceType.MONTHLY)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2028, 2, 29), epochToDate(next))
    }

    @Test
    fun `monthly Mar 31 to Apr 30`() {
        val task = recurringTask(dueAt = dateToEpoch(2026, 3, 31), type = RecurrenceType.MONTHLY)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2026, 4, 30), epochToDate(next))
    }

    @Test
    fun `monthly crosses year boundary`() {
        val task = recurringTask(dueAt = dateToEpoch(2026, 12, 15), type = RecurrenceType.MONTHLY)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2027, 1, 15), epochToDate(next))
    }

    @Test
    fun `monthly preserves time`() {
        val task = recurringTask(
            dueAt = dateTimeToEpoch(2026, 3, 15, 18, 45),
            type = RecurrenceType.MONTHLY,
            dueAtHasTime = true,
        )
        val next = RecurrenceCalculator.nextDueDate(task)!!
        val nextDt = epochToDateTime(next)
        assertEquals(LocalDate.of(2026, 4, 15), nextDt.toLocalDate())
        assertEquals(18, nextDt.hour)
        assertEquals(45, nextDt.minute)
    }

    // ── YEARLY ──

    @Test
    fun `yearly interval 1`() {
        val task = recurringTask(dueAt = dateToEpoch(2026, 3, 27), type = RecurrenceType.YEARLY)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2027, 3, 27), epochToDate(next))
    }

    @Test
    fun `yearly interval 2`() {
        val task = recurringTask(dueAt = dateToEpoch(2026, 6, 15), type = RecurrenceType.YEARLY, interval = 2)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2028, 6, 15), epochToDate(next))
    }

    @Test
    fun `yearly Feb 29 leap to non-leap gives Feb 28`() {
        val task = recurringTask(dueAt = dateToEpoch(2028, 2, 29), type = RecurrenceType.YEARLY)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2029, 2, 28), epochToDate(next))
    }

    @Test
    fun `yearly Feb 29 to next leap year with interval 4`() {
        val task = recurringTask(dueAt = dateToEpoch(2028, 2, 29), type = RecurrenceType.YEARLY, interval = 4)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2032, 2, 29), epochToDate(next))
    }

    @Test
    fun `yearly preserves time`() {
        val task = recurringTask(
            dueAt = dateTimeToEpoch(2026, 7, 4, 10, 0),
            type = RecurrenceType.YEARLY,
            dueAtHasTime = true,
        )
        val next = RecurrenceCalculator.nextDueDate(task)!!
        val nextDt = epochToDateTime(next)
        assertEquals(LocalDate.of(2027, 7, 4), nextDt.toLocalDate())
        assertEquals(10, nextDt.hour)
        assertEquals(0, nextDt.minute)
    }

    // ── End date boundary ──

    @Test
    fun `end date exactly on next due date allows it`() {
        val nextExpected = dateToEpoch(2026, 3, 28)
        val task = recurringTask(
            dueAt = dateToEpoch(2026, 3, 27),
            type = RecurrenceType.DAILY,
            endDate = nextExpected,
        )
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2026, 3, 28), epochToDate(next))
    }

    @Test
    fun `end date one day before next due date returns null`() {
        val task = recurringTask(
            dueAt = dateToEpoch(2026, 3, 27),
            type = RecurrenceType.DAILY,
            endDate = dateToEpoch(2026, 3, 27), // end date = current due, next would be Mar 28
        )
        assertNull(RecurrenceCalculator.nextDueDate(task))
    }

    @Test
    fun `end date well past next due date allows it`() {
        val task = recurringTask(
            dueAt = dateToEpoch(2026, 3, 27),
            type = RecurrenceType.DAILY,
            endDate = dateToEpoch(2026, 12, 31),
        )
        assertNotNull(RecurrenceCalculator.nextDueDate(task))
    }

    // ── Edge cases ──

    @Test
    fun `interval 0 treated as 1`() {
        val task = recurringTask(dueAt = dateToEpoch(2026, 3, 27), type = RecurrenceType.DAILY, interval = 0)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2026, 3, 28), epochToDate(next))
    }

    @Test
    fun `negative interval treated as 1`() {
        val task = recurringTask(dueAt = dateToEpoch(2026, 3, 27), type = RecurrenceType.DAILY, interval = -5)
        val next = RecurrenceCalculator.nextDueDate(task)!!
        assertEquals(LocalDate.of(2026, 3, 28), epochToDate(next))
    }

    // ── Bitmask helper ──

    @Test
    fun `selectedDaysOfWeek empty bitmask`() {
        assertEquals(emptyList<DayOfWeek>(), RecurrenceCalculator.selectedDaysOfWeek(0))
    }

    @Test
    fun `selectedDaysOfWeek Monday only`() {
        assertEquals(listOf(DayOfWeek.MONDAY), RecurrenceCalculator.selectedDaysOfWeek(1))
    }

    @Test
    fun `selectedDaysOfWeek all days`() {
        val allDays = 0b1111111 // 127
        assertEquals(DayOfWeek.entries.toList(), RecurrenceCalculator.selectedDaysOfWeek(allDays))
    }

    @Test
    fun `selectedDaysOfWeek Mon-Wed-Fri`() {
        val mask = bitmask(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        assertEquals(
            listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            RecurrenceCalculator.selectedDaysOfWeek(mask),
        )
    }

    @Test
    fun `dayOfWeekToBitmask values are correct`() {
        assertEquals(1, RecurrenceCalculator.dayOfWeekToBitmask(DayOfWeek.MONDAY))
        assertEquals(2, RecurrenceCalculator.dayOfWeekToBitmask(DayOfWeek.TUESDAY))
        assertEquals(4, RecurrenceCalculator.dayOfWeekToBitmask(DayOfWeek.WEDNESDAY))
        assertEquals(8, RecurrenceCalculator.dayOfWeekToBitmask(DayOfWeek.THURSDAY))
        assertEquals(16, RecurrenceCalculator.dayOfWeekToBitmask(DayOfWeek.FRIDAY))
        assertEquals(32, RecurrenceCalculator.dayOfWeekToBitmask(DayOfWeek.SATURDAY))
        assertEquals(64, RecurrenceCalculator.dayOfWeekToBitmask(DayOfWeek.SUNDAY))
    }
}
