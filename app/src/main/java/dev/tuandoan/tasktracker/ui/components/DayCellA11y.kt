package dev.tuandoan.tasktracker.ui.components

/**
 * Pure string composer for the day-cell TalkBack description (CAL-28 / CAL-30).
 * Keeping the composition logic JVM-pure means we can verify the grammar without touching
 * Android resources — the caller in [DayCell] resolves locale-aware strings and hands them
 * in as non-null/null tokens. Null tokens are skipped.
 *
 * Expected output shape (English example):
 *   "Today, Tuesday, May 12, 3 tasks, 1 high priority, selected"
 *
 * None of the tokens carry separators themselves; this function joins them with ", " and
 * trims any leading/trailing separator artifacts.
 */
internal fun buildDayCellContentDescription(
    dateText: String,
    taskCountText: String? = null,
    highPriorityText: String? = null,
    isToday: Boolean = false,
    isSelected: Boolean = false,
    todayPrefix: String? = null,
    selectedSuffix: String? = null,
): String {
    val parts = buildList {
        if (isToday && todayPrefix != null) add(todayPrefix)
        add(dateText)
        if (taskCountText != null) add(taskCountText)
        if (highPriorityText != null) add(highPriorityText)
        if (isSelected && selectedSuffix != null) add(selectedSuffix)
    }
    return parts.joinToString(separator = ", ")
}
