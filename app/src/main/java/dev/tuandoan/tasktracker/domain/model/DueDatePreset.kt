package dev.tuandoan.tasktracker.domain.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class DueDatePreset(val label: String) {
    TODAY("Today"),
    TOMORROW("Tomorrow"),
    NEXT_WEEK("Next Week"),
    ;

    fun toEpochMillis(): Long {
        val date = when (this) {
            TODAY -> LocalDate.now()
            TOMORROW -> LocalDate.now().plusDays(1)
            NEXT_WEEK -> LocalDate.now().plusDays(7)
        }
        return date.atTime(LocalTime.of(23, 59))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
