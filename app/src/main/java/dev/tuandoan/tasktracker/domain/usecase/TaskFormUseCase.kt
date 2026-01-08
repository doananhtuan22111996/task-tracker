package dev.tuandoan.tasktracker.domain.usecase

import dev.tuandoan.tasktracker.data.database.Task
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskFormUseCase @Inject constructor() {

    // Dialog State
    private val _showAddTaskDialog = MutableStateFlow(false)
    val showAddTaskDialog: StateFlow<Boolean> = _showAddTaskDialog.asStateFlow()

    // Selected Task for Editing
    private val _selectedTask = MutableStateFlow<Task?>(null)
    val selectedTask: StateFlow<Task?> = _selectedTask.asStateFlow()

    // Form Fields
    private val _taskTitle = MutableStateFlow("")
    val taskTitle: StateFlow<String> = _taskTitle.asStateFlow()

    private val _taskDescription = MutableStateFlow("")
    val taskDescription: StateFlow<String> = _taskDescription.asStateFlow()

    // Error State
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Form Validation
    fun isFormValid(): Flow<Boolean> = _taskTitle.map { it.isNotBlank() }

    // Computed Properties
    fun isEditMode(): Flow<Boolean> = _selectedTask.map { it != null }

    // Dialog Actions
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

    // Error Management
    fun setError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Form Data Access
    data class FormData(
        val title: String,
        val description: String,
        val selectedTaskId: Long?
    )

    fun getFormData(): Flow<FormData> = combine(
        _taskTitle,
        _taskDescription,
        _selectedTask
    ) { title, description, selectedTask ->
        FormData(
            title = title,
            description = description,
            selectedTaskId = selectedTask?.id
        )
    }
}