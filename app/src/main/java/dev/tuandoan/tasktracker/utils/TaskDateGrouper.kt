package dev.tuandoan.tasktracker.utils

import android.content.Context
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.model.Priority
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Data class representing a group of tasks for a specific day
 */
data class TaskSection(
    val header: String, // e.g., "Today", "Yesterday", "Jan 15, 2026"
    val dateKey: String, // e.g., "2026-01-15" for sorting/grouping
    val tasks: List<Task>,
    val isToday: Boolean = false,
)

/**
 * Utility object for grouping tasks by day with human-friendly labels
 */
object TaskDateGrouper {

    /**
     * Group a list of tasks by day with human-friendly headers.
     *
     * Grouping priority:
     * 1. If task has dueAt != null, group by due date
     * 2. Otherwise, group by createdAt
     *
     * @param tasks List of tasks (should already be filtered/searched/sorted)
     * @param context Android context for resolving localized date labels
     * @return List of TaskSection with appropriate headers and date keys
     */
    fun groupTasksByDay(tasks: List<Task>, context: Context): List<TaskSection> {
        if (tasks.isEmpty()) return emptyList()

        val calendar = Calendar.getInstance()
        val today = getDayKey(calendar.timeInMillis)

        // Get yesterday's date
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = getDayKey(calendar.timeInMillis)

        // Get tomorrow's date
        calendar.add(Calendar.DAY_OF_YEAR, 2) // +2 because we subtracted 1 for yesterday
        val tomorrow = getDayKey(calendar.timeInMillis)

        // Group tasks by their effective date (dueAt if available, otherwise createdAt)
        val groupedTasks = tasks.groupBy { task ->
            val effectiveTimestamp = task.dueAt ?: task.createdAt
            getDayKey(effectiveTimestamp)
        }

        // Sort groups by date (most recent first) and create sections
        // Apply pinned-first ordering within each day section
        return groupedTasks.toSortedMap(compareByDescending { it })
            .map { (dateKey, tasksInGroup) ->
                TaskSection(
                    header = getDateHeaderLabel(dateKey, today, yesterday, tomorrow, context),
                    dateKey = dateKey,
                    tasks = applyPinnedFirstOrdering(tasksInGroup),
                    isToday = dateKey == today,
                )
            }
    }

    /**
     * Group tasks by priority level with human-friendly headers.
     * Returns sections in High -> Medium -> Low order.
     * Empty priority sections are omitted.
     * Pinned tasks appear first within each section.
     *
     * @param tasks List of tasks (should already be filtered/searched/sorted)
     * @return List of TaskSection grouped by priority
     */
    fun groupByPriority(tasks: List<Task>): List<TaskSection> {
        if (tasks.isEmpty()) return emptyList()

        val grouped = tasks.groupBy { task ->
            when (Priority.fromValue(task.priority)) {
                Priority.HIGH -> "\uD83D\uDD34 High"
                Priority.MEDIUM -> "\uD83D\uDFE1 Medium"
                Priority.LOW -> "\uD83D\uDFE2 Low"
            }
        }

        val orderedKeys = listOf("\uD83D\uDD34 High", "\uD83D\uDFE1 Medium", "\uD83D\uDFE2 Low")
        return orderedKeys.mapNotNull { key ->
            grouped[key]?.let { tasksInGroup ->
                TaskSection(
                    header = key,
                    dateKey = key,
                    tasks = applyPinnedFirstOrdering(tasksInGroup),
                    isToday = false,
                )
            }
        }
    }

    /**
     * Apply pinned-first ordering within a group of tasks.
     * Pinned tasks come first, then unpinned tasks, preserving the existing order within each group.
     */
    private fun applyPinnedFirstOrdering(tasks: List<Task>): List<Task> {
        val (pinned, unpinned) = tasks.partition { it.isPinned }
        return pinned + unpinned
    }

    /**
     * Convert timestamp to day key in format "YYYY-MM-DD"
     */
    private fun getDayKey(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    /**
     * Get human-friendly header label for a date key
     */
    private fun getDateHeaderLabel(
        dateKey: String,
        todayKey: String,
        yesterdayKey: String,
        tomorrowKey: String,
        context: Context,
    ): String {
        return when (dateKey) {
            todayKey -> context.getString(R.string.date_today)
            yesterdayKey -> context.getString(R.string.date_yesterday)
            tomorrowKey -> context.getString(R.string.date_tomorrow)
            else -> {
                // Parse date key back to timestamp and format nicely
                val parts = dateKey.split("-")
                if (parts.size == 3) {
                    try {
                        val year = parts[0].toInt()
                        val month = parts[1].toInt() - 1 // Calendar.MONTH is 0-based
                        val day = parts[2].toInt()

                        val calendar = Calendar.getInstance()
                        calendar.set(year, month, day, 0, 0, 0)
                        calendar.set(Calendar.MILLISECOND, 0)

                        val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        return formatter.format(calendar.time)
                    } catch (e: NumberFormatException) {
                        // Fall back to the date key itself if parsing fails
                        return dateKey
                    }
                }
                dateKey
            }
        }
    }
}
