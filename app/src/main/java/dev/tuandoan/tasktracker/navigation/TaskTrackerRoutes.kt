package dev.tuandoan.tasktracker.navigation

import java.net.URLEncoder

/**
 * Navigation routes for the Task Tracker app.
 */
object TaskTrackerRoutes {
    const val TASK_LIST = "task_list"
    const val ARCHIVED = "archived"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val TASK_EDITOR_CREATE = "task_editor?initialTitle={initialTitle}"
    const val TASK_EDITOR_EDIT = "task_editor/{taskId}"

    /**
     * Creates the create route, optionally with an initial title pre-filled.
     */
    fun taskEditorCreate(initialTitle: String = ""): String = if (initialTitle.isEmpty()) {
        "task_editor"
    } else {
        "task_editor?initialTitle=${URLEncoder.encode(initialTitle, "UTF-8")}"
    }

    /**
     * Creates the edit route with the given task ID.
     */
    fun taskEditorEdit(taskId: Long): String = "task_editor/$taskId"
}
