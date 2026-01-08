package dev.tuandoan.tasktracker.ui.state

import androidx.lifecycle.viewModelScope
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.model.TaskSort
import dev.tuandoan.tasktracker.domain.service.TaskSortService
import dev.tuandoan.tasktracker.domain.usecase.TaskCrudUseCase
import dev.tuandoan.tasktracker.domain.usecase.TaskFilterUseCase
import dev.tuandoan.tasktracker.domain.usecase.TaskSearchUseCase
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * State manager for task list operations including search, filter, sort and visibility.
 * Coordinates between different use cases and applies business logic.
 */
@OptIn(FlowPreview::class)
class TaskListStateManager @Inject constructor(
    private val crudUseCase: TaskCrudUseCase,
    private val searchUseCase: TaskSearchUseCase,
    private val filterUseCase: TaskFilterUseCase,
    private val sortService: TaskSortService
) {
    // Sort state
    private val _taskSort = MutableStateFlow(sortService.getDefaultSort())
    val taskSort: StateFlow<TaskSort> = _taskSort.asStateFlow()

    /**
     * Initialize state flows for a given coroutine scope (typically ViewModel scope)
     */
    fun initializeStateFlows(scope: CoroutineScope): TaskListState {
        val allTasks = crudUseCase.getAllTasks()
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        val hasActiveSearch = searchUseCase.hasActiveSearch()
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )

        val hasActiveFilter = filterUseCase.hasActiveFilter()
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )

        // Combined filtered, searched, and sorted tasks
        val visibleTasks: StateFlow<List<Task>> = combine(
            allTasks,
            searchUseCase.debouncedSearchQuery,
            filterUseCase.filter,
            _taskSort
        ) { tasks, query, currentFilter, sort ->
            val statusFiltered = filterUseCase.filterTasksByStatus(tasks, currentFilter)
            val searchFiltered = searchUseCase.filterTasksBySearch(statusFiltered, query)
            sortService.sortTasks(searchFiltered, sort)
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        return TaskListState(
            allTasks = allTasks,
            visibleTasks = visibleTasks,
            searchQuery = searchUseCase.searchQuery,
            filter = filterUseCase.filter,
            taskSort = taskSort,
            hasActiveSearch = hasActiveSearch,
            hasActiveFilter = hasActiveFilter,
            isLoading = crudUseCase.isLoading
        )
    }

    // === Sort Operations ===
    fun setSort(sort: TaskSort) {
        if (sortService.isValidSort(sort)) {
            _taskSort.value = sort
        }
    }

    fun setSortKey(key: dev.tuandoan.tasktracker.domain.model.SortKey, direction: dev.tuandoan.tasktracker.domain.model.SortDirection) {
        _taskSort.value = _taskSort.value.copy(
            key = key,
            direction = direction
        )
    }

    fun setCompletedGrouping(grouping: dev.tuandoan.tasktracker.domain.model.CompletedGrouping) {
        _taskSort.value = _taskSort.value.copy(
            completedGrouping = grouping
        )
    }

    fun toggleCompletedLast(enabled: Boolean) {
        _taskSort.value = _taskSort.value.copy(
            completedGrouping = if (enabled) {
                dev.tuandoan.tasktracker.domain.model.CompletedGrouping.COMPLETED_LAST
            } else {
                dev.tuandoan.tasktracker.domain.model.CompletedGrouping.NONE
            }
        )
    }

    // === Search Operations ===
    fun updateSearchQuery(query: String) = searchUseCase.updateSearchQuery(query)
    fun clearSearch() = searchUseCase.clearSearch()

    // === Filter Operations ===
    fun setFilter(filter: TaskFilter) = filterUseCase.setFilter(filter)

    /**
     * Get available sort options for UI
     */
    fun getAvailableSortOptions(): List<TaskSort> = sortService.getAvailableSortOptions()
}

/**
 * Data class containing all task list related state
 */
data class TaskListState(
    val allTasks: StateFlow<List<Task>>,
    val visibleTasks: StateFlow<List<Task>>,
    val searchQuery: StateFlow<String>,
    val filter: StateFlow<TaskFilter>,
    val taskSort: StateFlow<TaskSort>,
    val hasActiveSearch: StateFlow<Boolean>,
    val hasActiveFilter: StateFlow<Boolean>,
    val isLoading: StateFlow<Boolean>
)