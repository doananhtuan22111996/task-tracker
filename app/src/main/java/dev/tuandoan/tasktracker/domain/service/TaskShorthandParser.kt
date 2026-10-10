package dev.tuandoan.tasktracker.domain.service

import dev.tuandoan.tasktracker.domain.model.ParsedTaskTokens
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Pure Kotlin natural language shorthand tokenizer for fast task creation (CAP-10).
 *
 * Extracts dates, times, priority levels, and tags from task title input without Android framework dependencies.
 */
object TaskShorthandParser {

    private val PRIORITY_REGEX = Regex(
        """(?i)(?:^|\s)!(high|med|medium|low|h|m|l|[123])(?=\s|$|[.,!?;])""",
    )

    private val TAG_REGEX = Regex(
        """(?i)(?:^|\s)#([A-Za-z0-9_-]{1,20})(?=\s|$|[.,!?;])""",
    )

    private val TIME_AT_REGEX = Regex(
        """(?i)(?:^|\s)at\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?(?=\s|$|[.,!?;])""",
    )

    private val TIME_COLON_REGEX = Regex(
        """(?i)(?:^|\s)(\d{1,2}):(\d{2})\s*(am|pm)?(?=\s|$|[.,!?;])""",
    )

    private val TIME_AM_PM_REGEX = Regex(
        """(?i)(?:^|\s)(\d{1,2})\s*(am|pm)(?=\s|$|[.,!?;])""",
    )

    private val DATE_RELATIVE_REGEX = Regex(
        """(?i)(?:^|\s)(today|tomorrow|tmrw|next\s+week|hôm\s+nay|ngày\s+mai)(?=\s|$|[.,!?;])""",
    )

    private val DATE_WEEKDAY_REGEX = Regex(
        """(?i)(?:^|\s)(?:on\s+|next\s+)?(monday|mon|tuesday|tue|wednesday|wed|thursday|thu|friday|fri|saturday|sat|sunday|sun)(?=\s|$|[.,!?;])""",
    )

    private val PUNCTUATION_CLEANUP_REGEX = Regex(
        """^[\s,.:;!?-]+|[\s,.:;!?-]+$""",
    )

    /**
     * Parses the given [rawText] into structured [ParsedTaskTokens].
     *
     * @param rawText User input string.
     * @param clock Time provider (defaults to system default zone).
     */
    fun parse(rawText: String, clock: Clock = Clock.systemDefaultZone()): ParsedTaskTokens {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            return ParsedTaskTokens(rawText = rawText, cleanTitle = "")
        }

        val rangesToRemove = mutableListOf<IntRange>()

        // 1. Priority
        val priorityMatch = PRIORITY_REGEX.find(rawText)
        val priority = priorityMatch?.let { match ->
            rangesToRemove.add(tokenRange(match))
            when (match.groupValues[1].lowercase(Locale.ROOT)) {
                "3", "high", "h" -> 2
                "2", "medium", "med", "m" -> 1
                "1", "low", "l" -> 0
                else -> null
            }
        }

        // 2. Tag
        val tagMatch = TAG_REGEX.find(rawText)
        val tag = tagMatch?.let { match ->
            rangesToRemove.add(tokenRange(match))
            TagNormalizer.normalize(match.groupValues[1])
        }

        // 3. Time
        var parsedTime: LocalTime? = null
        val timeAtMatch = TIME_AT_REGEX.find(rawText)
        if (timeAtMatch != null) {
            val hourRaw = timeAtMatch.groupValues[1].toIntOrNull()
            val minuteRaw = timeAtMatch.groupValues[2].ifEmpty { "0" }.toIntOrNull() ?: 0
            val amPm = timeAtMatch.groupValues[3].lowercase(Locale.ROOT)
            val resolvedTime = resolveTime(hourRaw, minuteRaw, amPm)
            if (resolvedTime != null) {
                parsedTime = resolvedTime
                rangesToRemove.add(tokenRange(timeAtMatch))
            }
        }

        if (parsedTime == null) {
            val timeColonMatch = TIME_COLON_REGEX.find(rawText)
            if (timeColonMatch != null) {
                val hourRaw = timeColonMatch.groupValues[1].toIntOrNull()
                val minuteRaw = timeColonMatch.groupValues[2].toIntOrNull() ?: 0
                val amPm = timeColonMatch.groupValues[3].lowercase(Locale.ROOT)
                val resolvedTime = resolveTime(hourRaw, minuteRaw, amPm)
                if (resolvedTime != null) {
                    parsedTime = resolvedTime
                    rangesToRemove.add(tokenRange(timeColonMatch))
                }
            }
        }

        if (parsedTime == null) {
            val timeAmPmMatch = TIME_AM_PM_REGEX.find(rawText)
            if (timeAmPmMatch != null) {
                val hourRaw = timeAmPmMatch.groupValues[1].toIntOrNull()
                val amPm = timeAmPmMatch.groupValues[2].lowercase(Locale.ROOT)
                val resolvedTime = resolveTime(hourRaw, 0, amPm)
                if (resolvedTime != null) {
                    parsedTime = resolvedTime
                    rangesToRemove.add(tokenRange(timeAmPmMatch))
                }
            }
        }

        // 4. Date
        val zonedDateTime = ZonedDateTime.now(clock)
        val today = zonedDateTime.toLocalDate()
        var parsedDate: LocalDate? = null

        val relativeDateMatch = DATE_RELATIVE_REGEX.find(rawText)
        if (relativeDateMatch != null) {
            rangesToRemove.add(tokenRange(relativeDateMatch))
            parsedDate = when (relativeDateMatch.groupValues[1].lowercase(Locale.ROOT)) {
                "today", "hôm nay" -> today
                "tomorrow", "tmrw", "ngày mai" -> today.plusDays(1)
                "next week" -> today.plusWeeks(1)
                else -> null
            }
        }

        if (parsedDate == null) {
            val weekdayMatch = DATE_WEEKDAY_REGEX.find(rawText)
            if (weekdayMatch != null) {
                rangesToRemove.add(tokenRange(weekdayMatch))
                val targetDayOfWeek = parseDayOfWeek(weekdayMatch.groupValues[1])
                if (targetDayOfWeek != null) {
                    parsedDate = today.with(TemporalAdjusters.next(targetDayOfWeek))
                }
            }
        }

        // 5. Combine Date & Time
        var dueAt: Long? = null
        var dueAtHasTime = false

        if (parsedDate != null && parsedTime != null) {
            dueAt = ZonedDateTime.of(parsedDate, parsedTime, clock.zone).toInstant().toEpochMilli()
            dueAtHasTime = true
        } else if (parsedDate != null) {
            // Default to end-of-day (23:59:00) when date is given without explicit time
            val endOfDay = LocalTime.of(23, 59, 0)
            dueAt = ZonedDateTime.of(parsedDate, endOfDay, clock.zone).toInstant().toEpochMilli()
            dueAtHasTime = false
        } else if (parsedTime != null) {
            // If only time is provided, determine whether it's later today or tomorrow
            val targetDate = if (parsedTime.isAfter(zonedDateTime.toLocalTime())) {
                today
            } else {
                today.plusDays(1)
            }
            dueAt = ZonedDateTime.of(targetDate, parsedTime, clock.zone).toInstant().toEpochMilli()
            dueAtHasTime = true
        }

        // 6. Clean Title
        val cleanTitle = buildCleanTitle(rawText, rangesToRemove)

        return ParsedTaskTokens(
            rawText = rawText,
            cleanTitle = cleanTitle.ifEmpty { trimmed },
            dueAt = dueAt,
            dueAtHasTime = dueAtHasTime,
            priority = priority,
            tag = tag,
        )
    }

    private fun tokenRange(match: MatchResult): IntRange {
        // Strip leading whitespace from match range if present
        var start = match.range.first
        while (start < match.range.last && match.value[start - match.range.first].isWhitespace()) {
            start++
        }
        return start..match.range.last
    }

    private fun buildCleanTitle(rawText: String, ranges: List<IntRange>): String {
        if (ranges.isEmpty()) return rawText.trim()

        val chars = rawText.toCharArray()
        for (range in ranges) {
            val start = range.first.coerceIn(0, chars.size - 1)
            val end = range.last.coerceIn(0, chars.size - 1)
            for (i in start..end) {
                chars[i] = ' '
            }
        }

        val cleaned = String(chars)
            .replace(Regex("""\s+"""), " ")
            .trim()

        return cleaned.replace(PUNCTUATION_CLEANUP_REGEX, "").trim()
    }

    private fun resolveTime(hourRaw: Int?, minuteRaw: Int, amPm: String): LocalTime? {
        if (hourRaw == null || minuteRaw !in 0..59) return null

        val hour = when {
            amPm == "am" -> if (hourRaw == 12) 0 else hourRaw
            amPm == "pm" -> if (hourRaw < 12) hourRaw + 12 else hourRaw
            else -> hourRaw
        }

        return if (hour in 0..23) {
            LocalTime.of(hour, minuteRaw, 0)
        } else {
            null
        }
    }

    private fun parseDayOfWeek(input: String): DayOfWeek? = when (input.lowercase(Locale.ROOT)) {
        "monday", "mon" -> DayOfWeek.MONDAY
        "tuesday", "tue" -> DayOfWeek.TUESDAY
        "wednesday", "wed" -> DayOfWeek.WEDNESDAY
        "thursday", "thu" -> DayOfWeek.THURSDAY
        "friday", "fri" -> DayOfWeek.FRIDAY
        "saturday", "sat" -> DayOfWeek.SATURDAY
        "sunday", "sun" -> DayOfWeek.SUNDAY
        else -> null
    }
}
