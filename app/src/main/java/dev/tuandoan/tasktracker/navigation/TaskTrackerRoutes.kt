package dev.tuandoan.tasktracker.navigation

/**
 * Navigation routes for the Task Tracker app.
 */
object TaskTrackerRoutes {
    const val TASK_LIST = "task_list"
    const val ARCHIVED = "archived"
    const val STATS = "stats"
    const val TASK_EDITOR_CREATE = "task_editor"
    const val TASK_EDITOR_EDIT = "task_editor/{taskId}"

    /**
     * Creates the edit route with the given task ID.
     */
    fun taskEditorEdit(taskId: Long): String = "task_editor/$taskId"
}
