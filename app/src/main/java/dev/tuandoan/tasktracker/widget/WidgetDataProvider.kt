package dev.tuandoan.tasktracker.widget

import dev.tuandoan.tasktracker.data.database.TaskDao
import dev.tuandoan.tasktracker.widget.model.WidgetTask

class WidgetDataProvider(private val taskDao: TaskDao) {

    suspend fun getWidgetTasks(limit: Int = MAX_WIDGET_TASKS): List<WidgetTask> =
        taskDao.getWidgetTasks(limit).map { task ->
            WidgetTask(
                id = task.id,
                title = task.title,
                dueAt = task.dueAt,
                dueAtHasTime = task.dueAtHasTime,
                priority = task.priority,
                isPinned = task.isPinned,
            )
        }

    companion object {
        const val MAX_WIDGET_TASKS = 5
    }
}
