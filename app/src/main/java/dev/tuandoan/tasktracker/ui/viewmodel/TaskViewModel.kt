package dev.tuandoan.tasktracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.usecase.TaskCrudUseCase
import dev.tuandoan.tasktracker.domain.usecase.TaskFilterUseCase
import dev.tuandoan.tasktracker.domain.usecase.TaskFormUseCase
import dev.tuandoan.tasktracker.domain.usecase.TaskSearchUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main coordinator ViewModel that orchestrates task-related UI operations.
 * Uses specialized use cases for different concerns (CRUD, Search, Filter, Form).
 * Uses @HiltViewModel for Hilt dependency injection in ViewModels.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val crudUseCase: TaskCrudUseCase,
    private val searchUseCase: TaskSearchUseCase,
    private val filterUseCase: TaskFilterUseCase,
    private val formUseCase: TaskFormUseCase
) : ViewModel() {

    // Delegate data state from specialized use cases
    val allTasks = crudUseCase.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val isLoading = crudUseCase.isLoading

    // Search functionality
    val searchQuery = searchUseCase.searchQuery
    val hasActiveSearch = searchUseCase.hasActiveSearch()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Filter functionality
    val filter = filterUseCase.filter
    val hasActiveFilter = filterUseCase.hasActiveFilter()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Form functionality
    val showAddTaskDialog = formUseCase.showAddTaskDialog
    val selectedTask = formUseCase.selectedTask
    val taskTitle = formUseCase.taskTitle
    val taskDescription = formUseCase.taskDescription
    val isFormValid = formUseCase.isFormValid()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    val isEditMode = formUseCase.isEditMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Error handling - combine errors from all use cases
    val errorMessage: StateFlow<String?> = combine(
        crudUseCase.errorMessage,
        formUseCase.errorMessage
    ) { crudError, formError ->
        crudError ?: formError
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Combined filtered and searched tasks
    val visibleTasks: StateFlow<List<Task>> = combine(
        allTasks,
        searchUseCase.debouncedSearchQuery,
        filter
    ) { tasks, query, currentFilter ->
        val statusFiltered = filterUseCase.filterTasksByStatus(tasks, currentFilter)
        searchUseCase.filterTasksBySearch(statusFiltered, query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // === CRUD Operations ===
    fun createTask() {
        viewModelScope.launch {
            formUseCase.getFormData().first().let { formData ->
                if (formData.title.isBlank()) {
                    formUseCase.setError("Title cannot be empty")
                    return@launch
                }

                val result = crudUseCase.createTask(formData.title, formData.description)
                if (result.isSuccess) {
                    formUseCase.hideAddTaskDialog()
                }
            }
        }
    }

    fun updateTask() {
        viewModelScope.launch {
            formUseCase.getFormData().first().let { formData ->
                val taskId = formData.selectedTaskId

                if (taskId == null) {
                    formUseCase.setError("No task selected for update")
                    return@launch
                }

                if (formData.title.isBlank()) {
                    formUseCase.setError("Title cannot be empty")
                    return@launch
                }

                val result = crudUseCase.updateTask(taskId, formData.title, formData.description)
                if (result.isSuccess) {
                    formUseCase.hideAddTaskDialog()
                }
            }
        }
    }

    fun saveTask() {
        viewModelScope.launch {
            formUseCase.getFormData().first().let { currentFormData ->
                if (currentFormData.selectedTaskId != null) {
                    updateTask()
                } else {
                    createTask()
                }
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            crudUseCase.deleteTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            crudUseCase.toggleTaskCompletion(task)
        }
    }

    // === Form Management ===
    fun showAddTaskDialog() = formUseCase.showAddTaskDialog()
    fun showEditTaskDialog(task: Task) = formUseCase.showEditTaskDialog(task)
    fun hideAddTaskDialog() = formUseCase.hideAddTaskDialog()
    fun updateTaskTitle(title: String) = formUseCase.updateTaskTitle(title)
    fun updateTaskDescription(description: String) = formUseCase.updateTaskDescription(description)

    // === Search Operations ===
    fun updateSearchQuery(query: String) = searchUseCase.updateSearchQuery(query)
    fun clearSearch() = searchUseCase.clearSearch()

    // === Filter Operations ===
    fun setFilter(filter: TaskFilter) = filterUseCase.setFilter(filter)

    // === Error Management ===
    fun clearError() {
        crudUseCase.clearError()
        formUseCase.clearError()
    }
}