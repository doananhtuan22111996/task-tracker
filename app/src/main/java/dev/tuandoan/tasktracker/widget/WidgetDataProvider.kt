package dev.tuandoan.tasktracker.widget

import dev.tuandoan.tasktracker.data.database.TaskDao
import dev.tuandoan.tasktracker.widget.model.WidgetSource
import dev.tuandoan.tasktracker.widget.model.WidgetTask

/**
 * Single dispatch point from the [WidgetSource] sealed set to the matching
 * [TaskDao] query (ADR-004 Decision 3). Keeps `provideGlance` free of
 * branching logic and makes adding a fifth source in v1.14.0 a one-line
 * `when` extension.
 *
 * `now` is injected so JVM tests can drive deterministic Upcoming-7d windows.
 * Production callers use `System.currentTimeMillis()`.
 */
class WidgetDataProvider(private val taskDao: TaskDao, private val now: () -> Long = { System.currentTimeMillis() }) {

    suspend fun getWidgetTasks(source: WidgetSource, limit: Int): List<WidgetTask> {
        val tasks = when (source) {
            is WidgetSource.Today ->
                taskDao.getWidgetTasks(limit)

            is WidgetSource.Upcoming7d -> {
                val start = now()
                taskDao.getWidgetTasksUpcoming(start, start + WEEK_MILLIS, limit)
            }

            is WidgetSource.Pinned ->
                taskDao.getWidgetTasksPinned(limit)

            is WidgetSource.Tag ->
                if (source.name.isBlank()) {
                    emptyList()
                } else {
                    taskDao.getWidgetTasksByTag(source.name, limit)
                }
        }
        return tasks.map { task ->
            WidgetTask(
                id = task.id,
                title = task.title,
                dueAt = task.dueAt,
                dueAtHasTime = task.dueAtHasTime,
                priority = task.priority,
                isPinned = task.isPinned,
            )
        }
    }

    companion object {
        private const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}
