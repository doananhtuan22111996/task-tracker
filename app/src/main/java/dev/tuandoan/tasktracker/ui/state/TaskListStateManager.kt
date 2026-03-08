package dev.tuandoan.tasktracker.ui.state

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.model.SortDirection
import dev.tuandoan.tasktracker.domain.model.SortKey
import dev.tuandoan.tasktracker.domain.model.TaskSort
import dev.tuandoan.tasktracker.domain.service.TaskSortService
import dev.tuandoan.tasktracker.domain.usecase.TaskCrudUseCase
import dev.tuandoan.tasktracker.domain.usecase.TaskFilterUseCase
import dev.tuandoan.tasktracker.domain.usecase.TaskSearchUseCase
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val sortService: TaskSortService,
) {
    // Tag filter state
    private val _tagFilter = MutableStateFlow<String?>(null)
    val tagFilter: StateFlow<String?> = _tagFilter.asStateFlow()

    /**
     * Initialize state flows for a given coroutine scope (typically ViewModel scope)
     */
    fun initializeStateFlows(scope: CoroutineScope): TaskListState {
        val allTasks = crudUseCase.getAllTasks()
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

        val hasActiveSearch = searchUseCase.hasActiveSearch()
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false,
            )

        val hasActiveFilter = filterUseCase.hasActiveFilter()
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false,
            )

        val hasActiveTagFilter = _tagFilter.map { it != null }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false,
            )

        // Fixed sort: always due date ascending (null due dates last)
        val dueDateSort = TaskSort(key = SortKey.DUE_DATE, direction = SortDirection.ASC)

        // Combined filtered, searched, and sorted tasks
        val visibleTasks: StateFlow<List<Task>> = combine(
            allTasks,
            searchUseCase.debouncedSearchQuery,
            filterUseCase.filter,
            _tagFilter,
        ) { tasks, query, currentFilter, tagFilter ->
            val statusFiltered = filterUseCase.filterTasksByStatus(tasks, currentFilter)
            val searchFiltered = searchUseCase.filterTasksBySearch(statusFiltered, query)
            val tagFiltered = filterTasksByTag(searchFiltered, tagFilter)
            sortService.sortTasks(tagFiltered, dueDateSort)
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

        return TaskListState(
            allTasks = allTasks,
            visibleTasks = visibleTasks,
            searchQuery = searchUseCase.searchQuery,
            filter = filterUseCase.filter,
            tagFilter = tagFilter,
            hasActiveSearch = hasActiveSearch,
            hasActiveFilter = hasActiveFilter,
            hasActiveTagFilter = hasActiveTagFilter,
            isLoading = crudUseCase.isLoading,
        )
    }

    // === Search Operations ===
    fun updateSearchQuery(query: String) = searchUseCase.updateSearchQuery(query)
    fun clearSearch() = searchUseCase.clearSearch()

    // === Filter Operations ===
    fun setFilter(filter: TaskFilter) = filterUseCase.setFilter(filter)

    // === Tag Filter Operations ===
    fun setTagFilter(tag: String?) {
        _tagFilter.value = tag
    }

    fun clearTagFilter() {
        _tagFilter.value = null
    }

    /**
     * Filter tasks by tag. If tagFilter is null, returns all tasks.
     * If tagFilter is set, returns only tasks that have that exact tag.
     */
    private fun filterTasksByTag(tasks: List<Task>, tagFilter: String?): List<Task> = if (tagFilter == null) {
        tasks
    } else {
        tasks.filter { task -> task.tag == tagFilter }
    }
}

/**
 * Data class containing all task list related state
 */
data class TaskListState(
    val allTasks: StateFlow<List<Task>>,
    val visibleTasks: StateFlow<List<Task>>,
    val searchQuery: StateFlow<String>,
    val filter: StateFlow<TaskFilter>,
    val tagFilter: StateFlow<String?>,
    val hasActiveSearch: StateFlow<Boolean>,
    val hasActiveFilter: StateFlow<Boolean>,
    val hasActiveTagFilter: StateFlow<Boolean>,
    val isLoading: StateFlow<Boolean>,
)
