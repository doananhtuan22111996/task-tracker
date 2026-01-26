package dev.tuandoan.tasktracker.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.ITaskManager
import dev.tuandoan.tasktracker.domain.model.ReminderOption
import dev.tuandoan.tasktracker.domain.usecase.TaskFormUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the TaskEditorScreen.
 * Handles both create and edit modes for tasks.
 */
@HiltViewModel
class TaskEditorViewModel @Inject constructor(
    private val taskManager: ITaskManager,
    private val taskFormUseCase: TaskFormUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        private const val TASK_ID_ARG = "taskId"
    }

    private val taskId: Long? = savedStateHandle.get<Long>(TASK_ID_ARG)

    // Form fields
    private val _taskTitle = MutableStateFlow("")
    val taskTitle: StateFlow<String> = _taskTitle.asStateFlow()

    private val _taskDescription = MutableStateFlow("")
    val taskDescription: StateFlow<String> = _taskDescription.asStateFlow()

    private val _dueAt = MutableStateFlow<Long?>(null)
    val dueAt: StateFlow<Long?> = _dueAt.asStateFlow()

    private val _reminderOption = MutableStateFlow(ReminderOption.NONE)
    val reminderOption: StateFlow<ReminderOption> = _reminderOption.asStateFlow()

    private val _tag = MutableStateFlow("")
    val tag: StateFlow<String> = _tag.asStateFlow()

    private val _priority = MutableStateFlow(1) // 0=LOW, 1=MEDIUM, 2=HIGH
    val priority: StateFlow<Int> = _priority.asStateFlow()

    private val _isPinned = MutableStateFlow(false)
    val isPinned: StateFlow<Boolean> = _isPinned.asStateFlow()

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Original task for edit mode change detection
    private var originalTask: Task? = null

    // Derived states
    val isEditMode: Boolean = taskId != null

    // Form validation using existing TaskFormUseCase logic
    private val isTitleValid: Flow<Boolean> = _taskTitle.map { title ->
        val trimmed = title.trim()
        trimmed.isNotEmpty() && trimmed.length <= TaskFormUseCase.MAX_TITLE_LENGTH
    }

    private val isDueDateValid: Flow<Boolean> = _dueAt.map { dueAt ->
        dueAt == null || dueAt > System.currentTimeMillis()
    }

    private val isReminderValid: Flow<Boolean> = combine(_dueAt, _reminderOption) { dueAt, reminderOption ->
        if (reminderOption == ReminderOption.NONE) {
            true
        } else {
            dueAt != null && isReminderTimeValid(dueAt, reminderOption.offsetMinutes)
        }
    }

    private val isTagValid: Flow<Boolean> = _tag.map { tag ->
        val trimmed = tag.trim()
        trimmed.length <= TaskFormUseCase.MAX_TAG_LENGTH
    }

    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    // Update hasChanges whenever any field changes
    private fun updateHasChanges() {
        if (!isEditMode) {
            // Create mode - has changes if any field has content
            _hasChanges.value = _taskTitle.value.trim().isNotBlank() ||
                _taskDescription.value.trim().isNotBlank() ||
                _dueAt.value != null ||
                _reminderOption.value != ReminderOption.NONE ||
                _tag.value.trim().isNotBlank() ||
                _priority.value != 1 ||
                // Default is MEDIUM (1)
                _isPinned.value
        } else {
            // Edit mode - compare with original values
            _hasChanges.value = originalTask?.let { original ->
                _taskTitle.value.trim() != original.title ||
                    _taskDescription.value.trim() != original.description ||
                    _dueAt.value != original.dueAt ||
                    _reminderOption.value != ReminderOption.fromOffsetMinutes(original.reminderOffsetMinutes) ||
                    _tag.value.trim() != (original.tag ?: "") ||
                    _priority.value != original.priority ||
                    _isPinned.value != original.isPinned
            } ?: false
        }
    }

    val isSaveEnabled: Flow<Boolean> = combine(
        isTitleValid,
        isDueDateValid,
        isReminderValid,
        isTagValid,
        hasChanges,
    ) { titleValid, dueDateValid, reminderValid, tagValid, hasChanges ->
        titleValid && dueDateValid && reminderValid && tagValid && hasChanges
    }

    // Validation errors
    val titleError: Flow<String?> = _taskTitle.map { title ->
        val trimmed = title.trim()
        when {
            trimmed.isEmpty() && title.isNotEmpty() -> "Title cannot be empty"
            trimmed.length > TaskFormUseCase.MAX_TITLE_LENGTH ->
                "Title must be ≤ ${TaskFormUseCase.MAX_TITLE_LENGTH} characters"
            else -> null
        }
    }

    val dueDateError: Flow<String?> = _dueAt.map { dueAt ->
        when {
            dueAt != null && dueAt <= System.currentTimeMillis() -> "Due date must be in the future"
            else -> null
        }
    }

    val reminderError: Flow<String?> = combine(_dueAt, _reminderOption) { dueAt, reminderOption ->
        when {
            reminderOption == ReminderOption.NONE -> null
            dueAt == null -> "Due date is required for reminders"
            !isReminderTimeValid(dueAt, reminderOption.offsetMinutes) -> "Reminder time must be in the future"
            else -> null
        }
    }

    val tagError: Flow<String?> = _tag.map { tag ->
        val trimmed = tag.trim()
        when {
            trimmed.length > TaskFormUseCase.MAX_TAG_LENGTH ->
                "Tag must be ≤ ${TaskFormUseCase.MAX_TAG_LENGTH} characters"
            else -> null
        }
    }

    // Events
    private val _events = MutableSharedFlow<TaskEditorEvent>(replay = 0)
    val events: SharedFlow<TaskEditorEvent> = _events.asSharedFlow()

    init {
        if (isEditMode && taskId != null) {
            loadTask(taskId)
        }
    }

    private fun loadTask(taskId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val task = taskManager.getTaskById(taskId)
                if (task != null) {
                    originalTask = task
                    _taskTitle.value = task.title
                    _taskDescription.value = task.description
                    _dueAt.value = task.dueAt
                    _reminderOption.value = ReminderOption.fromOffsetMinutes(task.reminderOffsetMinutes)
                    _tag.value = task.tag ?: ""
                    _priority.value = task.priority
                    _isPinned.value = task.isPinned
                    updateHasChanges()
                } else {
                    _events.emit(TaskEditorEvent.TaskNotFound)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load task: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Form field updates
    fun updateTitle(title: String) {
        if (title.length <= TaskFormUseCase.MAX_TITLE_LENGTH) {
            _taskTitle.value = title
            updateHasChanges()
        }
    }

    fun updateDescription(description: String) {
        if (description.length <= TaskFormUseCase.MAX_DESCRIPTION_LENGTH) {
            _taskDescription.value = description
            updateHasChanges()
        }
    }

    fun updateDueAt(dueAt: Long?) {
        _dueAt.value = dueAt
        updateHasChanges()
    }

    fun updateReminderOption(reminderOption: ReminderOption) {
        _reminderOption.value = reminderOption
        updateHasChanges()
    }

    fun updateTag(tag: String) {
        if (tag.length <= TaskFormUseCase.MAX_TAG_LENGTH) {
            _tag.value = tag
            updateHasChanges()
        }
    }

    fun updatePriority(priority: Int) {
        if (priority in 0..2) { // 0=LOW, 1=MEDIUM, 2=HIGH
            _priority.value = priority
            updateHasChanges()
        }
    }

    fun updateIsPinned(isPinned: Boolean) {
        _isPinned.value = isPinned
        updateHasChanges()
    }

    fun saveTask() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val trimmedTitle = _taskTitle.value.trim()
                val trimmedDescription = _taskDescription.value.trim()
                val trimmedTag = _tag.value.trim().takeIf { it.isNotEmpty() }
                val reminderOffsetMinutes = if (_reminderOption.value ==
                    ReminderOption.NONE
                ) {
                    null
                } else {
                    _reminderOption.value.offsetMinutes
                }

                if (isEditMode && originalTask != null) {
                    // Update existing task
                    val updatedTask = originalTask!!.copy(
                        title = trimmedTitle,
                        description = trimmedDescription,
                        dueAt = _dueAt.value,
                        reminderOffsetMinutes = reminderOffsetMinutes,
                        tag = trimmedTag,
                        priority = _priority.value,
                        isPinned = _isPinned.value,
                    )
                    taskManager.updateTask(updatedTask)
                } else {
                    // Create new task
                    val taskId = taskManager.createTask(
                        title = trimmedTitle,
                        description = trimmedDescription,
                        dueAt = _dueAt.value,
                        reminderOffsetMinutes = reminderOffsetMinutes,
                        tag = trimmedTag,
                    )

                    // Set priority and pin status if different from defaults
                    if (_priority.value != 1) {
                        taskManager.setPriority(taskId, _priority.value)
                    }
                    if (_isPinned.value) {
                        taskManager.setPinned(taskId, true)
                    }
                }

                _events.emit(TaskEditorEvent.TaskSaved)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save task: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun isReminderTimeValid(dueAt: Long, offsetMinutes: Int): Boolean {
        val reminderTime = dueAt - (offsetMinutes * 60 * 1000L)
        return reminderTime > System.currentTimeMillis()
    }

    /**
     * Get priority display name
     */
    fun getPriorityName(priority: Int): String = when (priority) {
        0 -> "Low"
        1 -> "Medium"
        2 -> "High"
        else -> "Medium"
    }

    /**
     * Get all available priorities
     */
    fun getAllPriorities(): List<Pair<Int, String>> = listOf(
        0 to "Low",
        1 to "Medium",
        2 to "High",
    )
}

/**
 * Events emitted by TaskEditorViewModel
 */
sealed class TaskEditorEvent {
    object TaskSaved : TaskEditorEvent()
    object TaskNotFound : TaskEditorEvent()
}
