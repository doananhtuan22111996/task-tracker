package dev.tuandoan.tasktracker.navigation

/**
 * Navigation routes for the Task Tracker app.
 */
object TaskTrackerRoutes {
    const val TASK_LIST = "task_list?statsFilter={statsFilter}"
    const val TASK_LIST_BASE = "task_list"
    const val ARCHIVED = "archived"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"
    const val HELP = "help"
    const val TAG_MANAGEMENT = "tag_management"
    const val TASK_EDITOR_CREATE = "task_editor"
    const val TASK_EDITOR_EDIT = "task_editor/{taskId}"

    /**
     * Creates the edit route with the given task ID.
     */
    fun taskEditorEdit(taskId: Long): String = "task_editor/$taskId"

    /**
     * Creates the task list route with an optional stats filter.
     */
    fun taskListWithFilter(statsFilter: String): String = "task_list?statsFilter=$statsFilter"
}

/**
 * Filter types that can be applied from the Stats screen.
 */
enum class StatsFilter {
    ACTIVE,
    COMPLETED,
    COMPLETED_TODAY,
    DUE_TODAY,
    OVERDUE,
}
