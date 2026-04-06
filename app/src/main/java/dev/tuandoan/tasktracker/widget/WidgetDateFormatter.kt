package dev.tuandoan.tasktracker.widget

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal fun formatDueDate(dueAt: Long?, nowProvider: () -> Long = { System.currentTimeMillis() }): String? {
    if (dueAt == null) return null

    val now = nowProvider()
    val today = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    val dayAfterTomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 2) }

    return when {
        dueAt < now -> "Overdue"
        dueAt >= today.timeInMillis && dueAt < tomorrow.timeInMillis -> "Today"
        dueAt >= tomorrow.timeInMillis && dueAt < dayAfterTomorrow.timeInMillis -> "Tomorrow"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(dueAt))
    }
}
