package dev.tuandoan.tasktracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.ITaskManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for task-related UI operations.
 * Uses @HiltViewModel for Hilt dependency injection in ViewModels.
 * Automatically scoped to ViewModelComponent lifecycle.
 */
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskManager: ITaskManager
) : ViewModel() {

    // Data State
    val allTasks = taskManager.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI State
    private val _selectedTask = MutableStateFlow<Task?>(null)
    val selectedTask: StateFlow<Task?> = _selectedTask.asStateFlow()

    private val _showAddTaskDialog = MutableStateFlow(false)
    val showAddTaskDialog: StateFlow<Boolean> = _showAddTaskDialog.asStateFlow()

    private val _taskTitle = MutableStateFlow("")
    val taskTitle: StateFlow<String> = _taskTitle.asStateFlow()

    private val _taskDescription = MutableStateFlow("")
    val taskDescription: StateFlow<String> = _taskDescription.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Form Validation
    val isFormValid: StateFlow<Boolean> = _taskTitle
        .map { it.isNotBlank() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Task Actions (delegate to TaskManager)
    fun createTask() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                taskManager.createTask(
                    title = _taskTitle.value,
                    description = _taskDescription.value
                )

                hideAddTaskDialog()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to create task"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTask() {
        val currentSelectedTask = _selectedTask.value ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                taskManager.updateTaskContent(
                    taskId = currentSelectedTask.id,
                    title = _taskTitle.value,
                    description = _taskDescription.value
                )

                hideAddTaskDialog()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update task"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                taskManager.deleteTask(task)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to delete task"
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            try {
                taskManager.toggleTaskCompletion(task)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update task"
            }
        }
    }

    // UI Actions
    fun showAddTaskDialog() {
        _selectedTask.value = null
        clearTaskForm()
        _showAddTaskDialog.value = true
    }

    fun showEditTaskDialog(task: Task) {
        _selectedTask.value = task
        _taskTitle.value = task.title
        _taskDescription.value = task.description
        _showAddTaskDialog.value = true
    }

    fun hideAddTaskDialog() {
        _showAddTaskDialog.value = false
        clearTaskForm()
        _selectedTask.value = null
        _errorMessage.value = null
    }

    fun saveTask() {
        if (_selectedTask.value != null) {
            updateTask()
        } else {
            createTask()
        }
    }

    // Form Management
    fun updateTaskTitle(title: String) {
        _taskTitle.value = title
        clearError()
    }

    fun updateTaskDescription(description: String) {
        _taskDescription.value = description
    }

    fun clearTaskForm() {
        _taskTitle.value = ""
        _taskDescription.value = ""
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Computed Properties
    val isEditMode: Boolean
        get() = _selectedTask.value != null
}