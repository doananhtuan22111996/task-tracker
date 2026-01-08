package dev.tuandoan.tasktracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.model.CompletedGrouping
import dev.tuandoan.tasktracker.domain.model.SortDirection
import dev.tuandoan.tasktracker.domain.model.SortKey
import dev.tuandoan.tasktracker.domain.model.TaskSort
import dev.tuandoan.tasktracker.ui.manager.TaskCrudManager
import dev.tuandoan.tasktracker.ui.state.TaskFormState
import dev.tuandoan.tasktracker.ui.state.TaskFormStateManager
import dev.tuandoan.tasktracker.ui.state.TaskListState
import dev.tuandoan.tasktracker.ui.state.TaskListStateManager
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Refactored ViewModel that acts as a thin coordinator between UI and business logic managers.
 * Delegates responsibilities to specialized state managers and business logic coordinators.
 *
 * Responsibilities:
 * - Expose UI state from state managers
 * - Coordinate user actions between managers
 * - Handle coroutine scope management
 */
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val listStateManager: TaskListStateManager,
    private val formStateManager: TaskFormStateManager,
    private val crudManager: TaskCrudManager
) : ViewModel() {

    // Initialize state from managers
    private val listState: TaskListState = listStateManager.initializeStateFlows(viewModelScope)
    private val formState: TaskFormState = formStateManager.initializeStateFlows(viewModelScope)

    // === Exposed State Flows ===

    // List state
    val allTasks = listState.allTasks
    val visibleTasks = listState.visibleTasks
    val searchQuery = listState.searchQuery
    val filter = listState.filter
    val taskSort = listState.taskSort
    val hasActiveSearch = listState.hasActiveSearch
    val hasActiveFilter = listState.hasActiveFilter
    val isLoading = listState.isLoading

    // Form state
    val showAddTaskDialog = formState.showAddTaskDialog
    val selectedTask = formState.selectedTask
    val taskTitle = formState.taskTitle
    val taskDescription = formState.taskDescription
    val isFormValid = formState.isFormValid
    val isEditMode = formState.isEditMode

    // Combined error state
    val errorMessage = crudManager.initializeErrorState(viewModelScope)

    // === CRUD Operations ===

    fun createTask() {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.createTask(viewModelScope) }
        )
    }

    fun updateTask() {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.updateTask(viewModelScope) }
        )
    }

    fun saveTask() {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.saveTask(viewModelScope) }
        )
    }

    fun deleteTask(task: Task) {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.deleteTask(task) }
        )
    }

    fun toggleTaskCompletion(task: Task) {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.toggleTaskCompletion(task) }
        )
    }

    // === Form Management ===

    fun showAddTaskDialog() = formStateManager.showAddTaskDialog()
    fun showEditTaskDialog(task: Task) = formStateManager.showEditTaskDialog(task)
    fun hideAddTaskDialog() = formStateManager.hideAddTaskDialog()
    fun updateTaskTitle(title: String) = formStateManager.updateTaskTitle(title)
    fun updateTaskDescription(description: String) = formStateManager.updateTaskDescription(description)

    // === Search Operations ===

    fun updateSearchQuery(query: String) = listStateManager.updateSearchQuery(query)
    fun clearSearch() = listStateManager.clearSearch()

    // === Filter Operations ===

    fun setFilter(filter: TaskFilter) = listStateManager.setFilter(filter)

    // === Sort Operations ===

    fun setSort(sort: TaskSort) = listStateManager.setSort(sort)
    fun setSortKey(key: SortKey, direction: SortDirection) = listStateManager.setSortKey(key, direction)
    fun setCompletedGrouping(grouping: CompletedGrouping) = listStateManager.setCompletedGrouping(grouping)
    fun toggleCompletedLast(enabled: Boolean) = listStateManager.toggleCompletedLast(enabled)

    /**
     * Get available sort options for UI
     */
    fun getAvailableSortOptions(): List<TaskSort> = listStateManager.getAvailableSortOptions()

    // === Error Management ===

    fun clearError() = crudManager.clearAllErrors()

    // === Lifecycle Management ===

    override fun onCleared() {
        super.onCleared()
        // Cleanup handled by managers automatically
    }
}