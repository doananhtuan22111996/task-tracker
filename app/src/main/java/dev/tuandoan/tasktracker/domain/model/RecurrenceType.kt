package dev.tuandoan.tasktracker.domain.model

/**
 * Defines recurrence types for recurring tasks.
 * Stored as Int ordinal in the Task Room entity.
 */
enum class RecurrenceType(val value: Int) {
    NONE(0),
    DAILY(1),
    WEEKLY(2),
    MONTHLY(3),
    YEARLY(4),
    ;

    companion object {
        fun fromValue(value: Int): RecurrenceType = entries.firstOrNull { it.value == value } ?: NONE
    }
}
