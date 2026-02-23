package dev.tuandoan.tasktracker.domain.model

/**
 * Enum representing different reminder lead time options for tasks.
 */
enum class ReminderOption(val offsetMinutes: Int) {
    NONE(0),
    MINUTES_1(1),
    MINUTES_5(5),
    HOURS_1(60),
    DAYS_1(24 * 60),
    ;

    companion object {
        /**
         * Get ReminderOption from offset minutes.
         * Returns NONE if no match found.
         */
        fun fromOffsetMinutes(offsetMinutes: Int?): ReminderOption {
            if (offsetMinutes == null || offsetMinutes == 0) return NONE
            return entries.firstOrNull { it.offsetMinutes == offsetMinutes } ?: NONE
        }

        /**
         * Get all options except NONE for UI display.
         */
        fun getSelectableOptions(): List<ReminderOption> = entries.filter { it != NONE }
    }
}
