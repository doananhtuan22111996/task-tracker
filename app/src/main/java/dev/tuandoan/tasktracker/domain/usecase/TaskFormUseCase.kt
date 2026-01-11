package dev.tuandoan.tasktracker.domain.usecase

import dev.tuandoan.tasktracker.data.database.Task
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskFormUseCase @Inject constructor() {

    companion object {
        const val MAX_TITLE_LENGTH = 100
        const val MAX_DESCRIPTION_LENGTH = 500
    }

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

    // Validation Error States
    private val _titleError = MutableStateFlow<String?>(null)
    val titleError: StateFlow<String?> = _titleError.asStateFlow()

    // Original values for change detection in edit mode
    private var originalTitle: String = ""
    private var originalDescription: String = ""

    // Error State
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Computed Validation States
    val isTitleValid: Flow<Boolean> = _taskTitle.map { title ->
        val trimmed = title.trim()
        trimmed.isNotEmpty() && trimmed.length <= MAX_TITLE_LENGTH
    }

    val hasChanges: Flow<Boolean> = combine(_taskTitle, _taskDescription, _selectedTask) { title, description, selectedTask ->
        if (selectedTask == null) {
            // Add mode - has changes if title is not blank
            title.trim().isNotBlank()
        } else {
            // Edit mode - has changes if values differ from original (after trimming)
            title.trim() != originalTitle.trim() || description.trim() != originalDescription.trim()
        }
    }

    val isSaveEnabled: Flow<Boolean> = combine(isTitleValid, hasChanges) { titleValid, hasChanges ->
        titleValid && hasChanges
    }

    // Form Validation
    fun isFormValid(): Flow<Boolean> = isTitleValid

    // Computed Properties
    fun isEditMode(): Flow<Boolean> = _selectedTask.map { it != null }

    // Dialog Actions
    fun showAddTaskDialog() {
        _selectedTask.value = null
        clearTaskForm()
        clearValidationErrors()
        _showAddTaskDialog.value = true
    }

    fun showEditTaskDialog(task: Task) {
        _selectedTask.value = task
        _taskTitle.value = task.title
        _taskDescription.value = task.description

        // Store original values for change detection
        originalTitle = task.title
        originalDescription = task.description

        clearValidationErrors()
        _showAddTaskDialog.value = true
    }

    fun hideAddTaskDialog() {
        _showAddTaskDialog.value = false
        clearTaskForm()
        clearValidationErrors()
        _selectedTask.value = null
        _errorMessage.value = null
    }

    // Form Management with Length Limits
    fun updateTaskTitle(title: String) {
        // Allow typing but enforce limit at validation time for better UX
        if (title.length <= MAX_TITLE_LENGTH) {
            _taskTitle.value = title
        }
        validateTitle(title)
    }

    fun updateTaskDescription(description: String) {
        // Allow typing but enforce limit at validation time
        if (description.length <= MAX_DESCRIPTION_LENGTH) {
            _taskDescription.value = description
        }
    }

    fun clearTaskForm() {
        _taskTitle.value = ""
        _taskDescription.value = ""
        originalTitle = ""
        originalDescription = ""
    }

    // Enhanced Validation
    private fun validateTitle(title: String) {
        val trimmed = title.trim()
        _titleError.value = when {
            trimmed.isEmpty() && title.isNotEmpty() -> "Title cannot be empty"
            trimmed.length > MAX_TITLE_LENGTH -> "Title must be ≤ $MAX_TITLE_LENGTH characters"
            else -> null
        }
    }

    fun validateForm(): Pair<Boolean, String?> {
        val title = _taskTitle.value.trim()
        val description = _taskDescription.value.trim()

        return when {
            title.isEmpty() -> false to "Title cannot be empty"
            title.length > MAX_TITLE_LENGTH -> false to "Title must be ≤ $MAX_TITLE_LENGTH characters"
            description.length > MAX_DESCRIPTION_LENGTH -> false to "Description must be ≤ $MAX_DESCRIPTION_LENGTH characters"
            else -> true to null
        }
    }

    // Get trimmed form data for saving
    fun getTrimmedFormData(): FormData {
        return FormData(
            title = _taskTitle.value.trim(),
            description = _taskDescription.value.trim(),
            selectedTaskId = _selectedTask.value?.id
        )
    }

    // Error Management
    fun setError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun clearValidationErrors() {
        _titleError.value = null
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