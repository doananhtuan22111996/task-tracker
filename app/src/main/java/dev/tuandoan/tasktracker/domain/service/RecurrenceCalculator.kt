package dev.tuandoan.tasktracker.domain.service

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.model.RecurrenceType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Pure Kotlin utility for calculating the next due date of a recurring task.
 * No Android dependencies — fully unit-testable.
 */
object RecurrenceCalculator {

    /**
     * Calculates the next due date for a recurring task.
     *
     * @param task The task to calculate the next occurrence for.
     * @return The next due date as epoch millis, or null if:
     *   - The task has no recurrence (NONE)
     *   - The task has no due date
     *   - The next date would exceed the recurrence end date
     *   - Weekly recurrence has no days selected
     */
    fun nextDueDate(task: Task): Long? {
        val recurrenceType = RecurrenceType.fromValue(task.recurrenceType)
        if (recurrenceType == RecurrenceType.NONE) return null

        val dueAt = task.dueAt ?: return null
        val interval = task.recurrenceInterval.coerceAtLeast(1)
        val zone = ZoneId.systemDefault()

        val nextDueAt = if (task.dueAtHasTime) {
            val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(dueAt), zone)
            val nextDateTime = advanceDateTime(dateTime, recurrenceType, interval, task.recurrenceDaysOfWeek)
                ?: return null
            nextDateTime.atZone(zone).toInstant().toEpochMilli()
        } else {
            val date = Instant.ofEpochMilli(dueAt).atZone(zone).toLocalDate()
            val nextDate = advanceDate(date, recurrenceType, interval, task.recurrenceDaysOfWeek)
                ?: return null
            nextDate.atStartOfDay(zone).toInstant().toEpochMilli()
        }

        // Check end date
        val endDate = task.recurrenceEndDate
        if (endDate != null && nextDueAt > endDate) return null

        return nextDueAt
    }

    private fun advanceDate(date: LocalDate, type: RecurrenceType, interval: Int, daysOfWeekBitmask: Int): LocalDate? =
        when (type) {
            RecurrenceType.NONE -> null
            RecurrenceType.DAILY -> date.plusDays(interval.toLong())
            RecurrenceType.WEEKLY -> nextWeeklyDate(date, interval, daysOfWeekBitmask)
            RecurrenceType.MONTHLY -> advanceMonthly(date, interval)
            RecurrenceType.YEARLY -> advanceYearly(date, interval)
        }

    private fun advanceDateTime(
        dateTime: LocalDateTime,
        type: RecurrenceType,
        interval: Int,
        daysOfWeekBitmask: Int,
    ): LocalDateTime? {
        val date = dateTime.toLocalDate()
        val nextDate = advanceDate(date, type, interval, daysOfWeekBitmask) ?: return null
        return nextDate.atTime(dateTime.toLocalTime())
    }

    /**
     * Weekly recurrence with day-of-week bitmask.
     *
     * If bitmask is 0 (no specific days), advance by [interval] weeks from current date.
     * If bitmask is set, find the next selected day after the current day-of-week.
     * If no more selected days remain in the current week, jump [interval] weeks forward
     * and pick the first selected day.
     */
    private fun nextWeeklyDate(date: LocalDate, interval: Int, bitmask: Int): LocalDate {
        if (bitmask == 0) return date.plusWeeks(interval.toLong())

        val selectedDays = selectedDaysOfWeek(bitmask)
        if (selectedDays.isEmpty()) return date.plusWeeks(interval.toLong())

        val currentDow = date.dayOfWeek

        // Find next selected day in the current week (after current day)
        val nextInWeek = selectedDays.firstOrNull { it > currentDow }

        return if (nextInWeek != null && interval <= 1) {
            // Same week, just advance to the next selected day
            date.plusDays((nextInWeek.value - currentDow.value).toLong())
        } else {
            // Jump to the start of the next interval week and pick the first selected day
            val daysUntilNextMonday = (DayOfWeek.MONDAY.value - currentDow.value + 7) % 7
            val weeksToSkip = if (nextInWeek != null) interval.toLong() else (interval.toLong() - 1).coerceAtLeast(0)
            val nextWeekMonday = date.plusDays(daysUntilNextMonday.toLong()).plusWeeks(weeksToSkip)
            val firstDay = selectedDays.first()
            nextWeekMonday.plusDays((firstDay.value - DayOfWeek.MONDAY.value).toLong())
        }
    }

    /**
     * Monthly: add N months, clamp day to month length.
     * e.g., Jan 31 + 1 month → Feb 28 (or 29 in leap year)
     */
    private fun advanceMonthly(date: LocalDate, interval: Int): LocalDate {
        val nextMonth = date.plusMonths(interval.toLong())
        val maxDay = nextMonth.lengthOfMonth()
        return if (date.dayOfMonth > maxDay) {
            nextMonth.withDayOfMonth(maxDay)
        } else {
            nextMonth
        }
    }

    /**
     * Yearly: add N years, handle Feb 29 → Feb 28 in non-leap year.
     */
    private fun advanceYearly(date: LocalDate, interval: Int): LocalDate {
        val nextYear = date.plusYears(interval.toLong())
        val maxDay = nextYear.lengthOfMonth()
        return if (date.dayOfMonth > maxDay) {
            nextYear.withDayOfMonth(maxDay)
        } else {
            nextYear
        }
    }

    /**
     * Converts bitmask to sorted list of [DayOfWeek].
     * Bitmask convention: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
     */
    internal fun selectedDaysOfWeek(bitmask: Int): List<DayOfWeek> = DayOfWeek.entries.filter { day ->
        val bit = 1 shl (day.value - 1) // Mon(1)→bit 1, Tue(2)→bit 2, etc.
        bitmask and bit != 0
    }

    /**
     * Converts a [DayOfWeek] to its bitmask value.
     */
    fun dayOfWeekToBitmask(day: DayOfWeek): Int = 1 shl (day.value - 1)
}
