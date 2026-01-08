package dev.tuandoan.tasktracker.domain.model

/**
 * Defines the sorting key for tasks
 */
enum class SortKey {
    CREATED_AT,
    TITLE
}

/**
 * Defines the sorting direction
 */
enum class SortDirection {
    ASC,    // Ascending
    DESC    // Descending
}

/**
 * Defines how completed tasks should be grouped
 */
enum class CompletedGrouping {
    NONE,               // No special grouping
    COMPLETED_FIRST,    // Show completed tasks first
    COMPLETED_LAST      // Show completed tasks last
}

/**
 * Represents the current sort configuration for tasks
 */
data class TaskSort(
    val key: SortKey = SortKey.CREATED_AT,
    val direction: SortDirection = SortDirection.DESC, // Default newest first
    val completedGrouping: CompletedGrouping = CompletedGrouping.NONE
) {
    /**
     * Returns a human-readable description of the current sort
     */
    fun getDisplayName(): String = when (key) {
        SortKey.CREATED_AT -> when (direction) {
            SortDirection.DESC -> "Created: Newest first"
            SortDirection.ASC -> "Created: Oldest first"
        }
        SortKey.TITLE -> when (direction) {
            SortDirection.ASC -> "Title: A–Z"
            SortDirection.DESC -> "Title: Z–A"
        }
    }

    /**
     * Returns display name including completed grouping if applicable
     */
    fun getFullDisplayName(): String {
        val baseName = getDisplayName()
        return when (completedGrouping) {
            CompletedGrouping.NONE -> baseName
            CompletedGrouping.COMPLETED_FIRST -> "$baseName (Completed first)"
            CompletedGrouping.COMPLETED_LAST -> "$baseName (Completed last)"
        }
    }
}