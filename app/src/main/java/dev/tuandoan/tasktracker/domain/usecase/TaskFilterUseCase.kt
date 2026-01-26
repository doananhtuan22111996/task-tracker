package dev.tuandoan.tasktracker.domain.usecase

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskFilterUseCase @Inject constructor() {

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    /**
     * Applies status filter to a list of tasks
     */
    fun filterTasksByStatus(tasks: List<Task>, filter: TaskFilter): List<Task> = when (filter) {
        TaskFilter.ALL -> tasks
        TaskFilter.ACTIVE -> tasks.filter { !it.isCompleted }
        TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }
    }

    /**
     * Check if filter is not showing all tasks
     */
    fun hasActiveFilter(): Flow<Boolean> = _filter.map { it != TaskFilter.ALL }

    /**
     * Reset filter to show all tasks
     */
    fun resetFilter() {
        _filter.value = TaskFilter.ALL
    }
}
