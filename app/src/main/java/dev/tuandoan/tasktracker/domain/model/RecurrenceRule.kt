package dev.tuandoan.tasktracker.domain.model

import java.time.DayOfWeek

/**
 * UI-friendly representation of a recurrence rule.
 * Maps between the Task entity's flat int fields and structured UI state.
 */
data class RecurrenceRule(
    val type: RecurrenceType = RecurrenceType.NONE,
    val interval: Int = 1,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val endDate: Long? = null,
) {
    val isRecurring: Boolean get() = type != RecurrenceType.NONE

    /**
     * Convert days-of-week set to bitmask for storage.
     * Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
     */
    fun daysOfWeekBitmask(): Int = daysOfWeek.fold(0) { mask, day ->
        mask or (1 shl (day.value - 1))
    }

    companion object {
        val NONE = RecurrenceRule()

        /**
         * Create from Task entity fields.
         */
        fun fromTaskFields(
            recurrenceType: Int,
            recurrenceInterval: Int,
            recurrenceDaysOfWeek: Int,
            recurrenceEndDate: Long?,
        ): RecurrenceRule {
            val type = RecurrenceType.fromValue(recurrenceType)
            if (type == RecurrenceType.NONE) return NONE

            return RecurrenceRule(
                type = type,
                interval = recurrenceInterval.coerceAtLeast(1),
                daysOfWeek = bitmaskToDaysOfWeek(recurrenceDaysOfWeek),
                endDate = recurrenceEndDate,
            )
        }

        private fun bitmaskToDaysOfWeek(bitmask: Int): Set<DayOfWeek> {
            if (bitmask == 0) return emptySet()
            return DayOfWeek.entries.filterTo(mutableSetOf()) { day ->
                bitmask and (1 shl (day.value - 1)) != 0
            }
        }
    }
}
