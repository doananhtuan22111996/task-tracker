package dev.tuandoan.tasktracker.domain.service

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.model.CompletedGrouping
import dev.tuandoan.tasktracker.domain.model.SortDirection
import dev.tuandoan.tasktracker.domain.model.SortKey
import dev.tuandoan.tasktracker.domain.model.TaskSort
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure business logic service for sorting tasks.
 * Contains no state, only sorting algorithms and business rules.
 */
@Singleton
class TaskSortService @Inject constructor() {

    /**
     * Sorts a list of tasks according to the specified TaskSort configuration.
     * Applies completion grouping first (if specified), then sorts within groups.
     *
     * @param tasks The list of tasks to sort
     * @param sort The sort configuration to apply
     * @return Sorted list of tasks
     */
    fun sortTasks(tasks: List<Task>, sort: TaskSort): List<Task> {
        if (tasks.isEmpty()) return tasks

        return when (sort.completedGrouping) {
            CompletedGrouping.NONE -> {
                // No grouping, just sort all tasks together
                applySorting(tasks, sort.key, sort.direction)
            }
            CompletedGrouping.COMPLETED_FIRST -> {
                val (completed, active) = tasks.partition { it.isCompleted }
                applySorting(completed, sort.key, sort.direction) +
                applySorting(active, sort.key, sort.direction)
            }
            CompletedGrouping.COMPLETED_LAST -> {
                val (completed, active) = tasks.partition { it.isCompleted }
                applySorting(active, sort.key, sort.direction) +
                applySorting(completed, sort.key, sort.direction)
            }
        }
    }

    /**
     * Applies sorting by key and direction to a list of tasks.
     * Uses stable sorting with createdAt as secondary key for deterministic ordering.
     *
     * @param tasks List of tasks to sort
     * @param key The field to sort by
     * @param direction The sort direction (ASC/DESC)
     * @return Sorted list of tasks
     */
    private fun applySorting(tasks: List<Task>, key: SortKey, direction: SortDirection): List<Task> {
        val comparator = when (key) {
            SortKey.CREATED_AT -> {
                when (direction) {
                    SortDirection.DESC -> compareByDescending<Task> { it.createdAt }
                    SortDirection.ASC -> compareBy<Task> { it.createdAt }
                }
            }
            SortKey.TITLE -> {
                when (direction) {
                    SortDirection.ASC -> compareBy<Task> { it.title.lowercase(Locale.ROOT) }
                        .thenByDescending { it.createdAt } // Secondary sort for stability
                    SortDirection.DESC -> compareByDescending<Task> { it.title.lowercase(Locale.ROOT) }
                        .thenByDescending { it.createdAt } // Secondary sort for stability
                }
            }
        }

        return tasks.sortedWith(comparator)
    }

    /**
     * Creates a default TaskSort configuration.
     * Default: Created newest first, no grouping.
     */
    fun getDefaultSort(): TaskSort = TaskSort(
        key = SortKey.CREATED_AT,
        direction = SortDirection.DESC,
        completedGrouping = CompletedGrouping.NONE
    )

    /**
     * Validates if a TaskSort configuration is valid.
     * Currently all combinations are valid, but this allows for future constraints.
     */
    fun isValidSort(sort: TaskSort): Boolean = true

    /**
     * Gets all available sort options for UI display.
     */
    fun getAvailableSortOptions(): List<TaskSort> = listOf(
        TaskSort(SortKey.CREATED_AT, SortDirection.DESC, CompletedGrouping.NONE),
        TaskSort(SortKey.CREATED_AT, SortDirection.ASC, CompletedGrouping.NONE),
        TaskSort(SortKey.TITLE, SortDirection.ASC, CompletedGrouping.NONE)
    )
}