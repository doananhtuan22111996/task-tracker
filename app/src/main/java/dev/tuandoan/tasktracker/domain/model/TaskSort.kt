package dev.tuandoan.tasktracker.domain.model

/**
 * Defines task priority levels
 */
enum class Priority(val value: Int, val displayName: String) {
    LOW(0, "Low"),
    MEDIUM(1, "Medium"),
    HIGH(2, "High"),
    ;

    companion object {
        fun fromValue(value: Int): Priority = when (value) {
            0 -> LOW
            1 -> MEDIUM
            2 -> HIGH
            else -> MEDIUM // Default to MEDIUM for invalid values
        }
    }
}

/**
 * Defines the sorting key for tasks
 */
enum class SortKey {
    MANUAL,
    CREATED_AT,
    TITLE,
    PRIORITY,
}

/**
 * Defines the sorting direction
 */
enum class SortDirection {
    ASC, // Ascending
    DESC, // Descending
}

/**
 * Defines how completed tasks should be grouped
 */
enum class CompletedGrouping {
    NONE, // No special grouping
    COMPLETED_FIRST, // Show completed tasks first
    COMPLETED_LAST, // Show completed tasks last
}

/**
 * Represents the current sort configuration for tasks
 */
data class TaskSort(
    val key: SortKey = SortKey.CREATED_AT,
    val direction: SortDirection = SortDirection.DESC, // Default newest first
    val completedGrouping: CompletedGrouping = CompletedGrouping.NONE,
) {
    /**
     * Returns a human-readable description of the current sort
     */
    fun getDisplayName(): String = when (key) {
        SortKey.MANUAL -> "Manual"
        SortKey.CREATED_AT -> when (direction) {
            SortDirection.DESC -> "Created: Newest first"
            SortDirection.ASC -> "Created: Oldest first"
        }
        SortKey.TITLE -> when (direction) {
            SortDirection.ASC -> "Title: A–Z"
            SortDirection.DESC -> "Title: Z–A"
        }
        SortKey.PRIORITY -> when (direction) {
            SortDirection.DESC -> "Priority: High to Low"
            SortDirection.ASC -> "Priority: Low to High"
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
