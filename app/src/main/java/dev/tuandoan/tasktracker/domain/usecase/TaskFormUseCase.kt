package dev.tuandoan.tasktracker.domain.usecase

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.model.ReminderOption
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

    private val _dueAt = MutableStateFlow<Long?>(null)
    val dueAt: StateFlow<Long?> = _dueAt.asStateFlow()

    private val _reminderOption = MutableStateFlow(ReminderOption.NONE)
    val reminderOption: StateFlow<ReminderOption> = _reminderOption.asStateFlow()

    // Validation Error States
    private val _titleError = MutableStateFlow<String?>(null)
    val titleError: StateFlow<String?> = _titleError.asStateFlow()

    private val _reminderError = MutableStateFlow<String?>(null)
    val reminderError: StateFlow<String?> = _reminderError.asStateFlow()

    private val _dueDateError = MutableStateFlow<String?>(null)
    val dueDateError: StateFlow<String?> = _dueDateError.asStateFlow()

    // Original values for change detection in edit mode
    private var originalTitle: String = ""
    private var originalDescription: String = ""
    private var originalDueAt: Long? = null
    private var originalReminderOption: ReminderOption = ReminderOption.NONE

    // Error State
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Computed Validation States
    val isTitleValid: Flow<Boolean> = _taskTitle.map { title ->
        val trimmed = title.trim()
        trimmed.isNotEmpty() && trimmed.length <= MAX_TITLE_LENGTH
    }

    val isDueDateValid: Flow<Boolean> = _dueAt.map { dueAt ->
        dueAt == null || dueAt > System.currentTimeMillis()
    }

    val isReminderValid: Flow<Boolean> = combine(_dueAt, _reminderOption) { dueAt, reminderOption ->
        if (reminderOption == ReminderOption.NONE) {
            true // No reminder is always valid
        } else {
            // Reminder requires a due date
            dueAt != null && isReminderTimeValid(dueAt, reminderOption.offsetMinutes)
        }
    }

    val hasChanges: Flow<Boolean> = combine(_taskTitle, _taskDescription, _dueAt, _reminderOption, _selectedTask) { title, description, dueAt, reminderOption, selectedTask ->
        if (selectedTask == null) {
            // Add mode - has changes if title is not blank or due date/reminder is set
            title.trim().isNotBlank() || dueAt != null || reminderOption != ReminderOption.NONE
        } else {
            // Edit mode - has changes if values differ from original (after trimming)
            val titleChanged = title.trim() != originalTitle.trim()
            val descChanged = description.trim() != originalDescription.trim()
            val dueDateChanged = dueAt != originalDueAt
            val reminderChanged = reminderOption != originalReminderOption

            titleChanged || descChanged || dueDateChanged || reminderChanged
        }
    }

    val isSaveEnabled: Flow<Boolean> = combine(isTitleValid, isDueDateValid, isReminderValid, hasChanges) { titleValid, dueDateValid, reminderValid, hasChanges ->
        titleValid && dueDateValid && reminderValid && hasChanges
    }

    // Form Validation
    fun isFormValid(): Flow<Boolean> = combine(isTitleValid, isDueDateValid, isReminderValid) { titleValid, dueDateValid, reminderValid ->
        titleValid && dueDateValid && reminderValid
    }

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
        _dueAt.value = task.dueAt
        _reminderOption.value = ReminderOption.fromOffsetMinutes(task.reminderOffsetMinutes)

        // Store original values for change detection
        originalTitle = task.title
        originalDescription = task.description
        originalDueAt = task.dueAt
        originalReminderOption = ReminderOption.fromOffsetMinutes(task.reminderOffsetMinutes)

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

    fun updateDueAt(dueAt: Long?) {
        _dueAt.value = dueAt
        validateDueDate()
        validateReminder()
    }

    fun updateReminderOption(reminderOption: ReminderOption) {
        _reminderOption.value = reminderOption
        validateReminder()
    }

    fun clearTaskForm() {
        _taskTitle.value = ""
        _taskDescription.value = ""
        _dueAt.value = null
        _reminderOption.value = ReminderOption.NONE
        originalTitle = ""
        originalDescription = ""
        originalDueAt = null
        originalReminderOption = ReminderOption.NONE
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

    private fun validateDueDate() {
        val dueAt = _dueAt.value
        _dueDateError.value = when {
            dueAt == null -> null // No due date is valid
            dueAt <= System.currentTimeMillis() -> "Due date must be in the future"
            else -> null
        }
    }

    private fun validateReminder() {
        val dueAt = _dueAt.value
        val reminderOption = _reminderOption.value

        _reminderError.value = when {
            reminderOption == ReminderOption.NONE -> null // No reminder is valid
            dueAt == null -> "Due date is required for reminders"
            !isReminderTimeValid(dueAt, reminderOption.offsetMinutes) -> "Reminder time must be in the future"
            else -> null
        }
    }

    private fun isReminderTimeValid(dueAt: Long, offsetMinutes: Int): Boolean {
        val reminderTime = dueAt - (offsetMinutes * 60 * 1000L)
        return reminderTime > System.currentTimeMillis()
    }

    fun validateForm(): Pair<Boolean, String?> {
        val title = _taskTitle.value.trim()
        val description = _taskDescription.value.trim()
        val dueAt = _dueAt.value
        val reminderOption = _reminderOption.value

        return when {
            title.isEmpty() -> false to "Title cannot be empty"
            title.length > MAX_TITLE_LENGTH -> false to "Title must be ≤ $MAX_TITLE_LENGTH characters"
            description.length > MAX_DESCRIPTION_LENGTH -> false to "Description must be ≤ $MAX_DESCRIPTION_LENGTH characters"
            dueAt != null && dueAt <= System.currentTimeMillis() -> false to "Due date must be in the future"
            reminderOption != ReminderOption.NONE && dueAt == null -> false to "Due date is required for reminders"
            reminderOption != ReminderOption.NONE && dueAt != null && !isReminderTimeValid(dueAt, reminderOption.offsetMinutes) ->
                false to "Reminder time must be in the future. Choose a later due date or smaller reminder."
            else -> true to null
        }
    }

    // Get trimmed form data for saving
    fun getTrimmedFormData(): FormData {
        return FormData(
            title = _taskTitle.value.trim(),
            description = _taskDescription.value.trim(),
            selectedTaskId = _selectedTask.value?.id,
            dueAt = _dueAt.value,
            reminderOffsetMinutes = if (_reminderOption.value == ReminderOption.NONE) null else _reminderOption.value.offsetMinutes
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
        _dueDateError.value = null
        _reminderError.value = null
        _errorMessage.value = null
    }

    // Form Data Access
    data class FormData(
        val title: String,
        val description: String,
        val selectedTaskId: Long?,
        val dueAt: Long?,
        val reminderOffsetMinutes: Int?
    )

    fun getFormData(): Flow<FormData> = combine(
        _taskTitle,
        _taskDescription,
        _selectedTask,
        _dueAt,
        _reminderOption
    ) { title, description, selectedTask, dueAt, reminderOption ->
        FormData(
            title = title,
            description = description,
            selectedTaskId = selectedTask?.id,
            dueAt = dueAt,
            reminderOffsetMinutes = if (reminderOption == ReminderOption.NONE) null else reminderOption.offsetMinutes
        )
    }
}