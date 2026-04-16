package dev.tuandoan.tasktracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.ITaskManager
import dev.tuandoan.tasktracker.domain.model.DueDatePreset
import dev.tuandoan.tasktracker.domain.model.RecurrenceRule
import dev.tuandoan.tasktracker.domain.model.RecurrenceType
import dev.tuandoan.tasktracker.domain.model.ReminderOption
import dev.tuandoan.tasktracker.domain.usecase.TagManagementUseCase
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
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * ViewModel for the TaskEditorScreen.
 * Handles both create and edit modes for tasks.
 */
@HiltViewModel
class TaskEditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskManager: ITaskManager,
    private val taskFormUseCase: TaskFormUseCase,
    private val tagManagementUseCase: TagManagementUseCase,
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

    private val _dueAtHasTime = MutableStateFlow(false)
    val dueAtHasTime: StateFlow<Boolean> = _dueAtHasTime.asStateFlow()

    private val _reminderOption = MutableStateFlow(ReminderOption.NONE)
    val reminderOption: StateFlow<ReminderOption> = _reminderOption.asStateFlow()

    private val _tag = MutableStateFlow("")
    val tag: StateFlow<String> = _tag.asStateFlow()

    private val _priority = MutableStateFlow(1) // 0=LOW, 1=MEDIUM, 2=HIGH
    val priority: StateFlow<Int> = _priority.asStateFlow()

    private val _isPinned = MutableStateFlow(false)
    val isPinned: StateFlow<Boolean> = _isPinned.asStateFlow()

    // Recurrence fields
    private val _recurrenceType = MutableStateFlow(RecurrenceType.NONE)
    val recurrenceType: StateFlow<RecurrenceType> = _recurrenceType.asStateFlow()

    private val _recurrenceInterval = MutableStateFlow(1)
    val recurrenceInterval: StateFlow<Int> = _recurrenceInterval.asStateFlow()

    private val _recurrenceDaysOfWeek = MutableStateFlow<Set<DayOfWeek>>(emptySet())
    val recurrenceDaysOfWeek: StateFlow<Set<DayOfWeek>> = _recurrenceDaysOfWeek.asStateFlow()

    private val _recurrenceEndDate = MutableStateFlow<Long?>(null)
    val recurrenceEndDate: StateFlow<Long?> = _recurrenceEndDate.asStateFlow()

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
                _isPinned.value ||
                _recurrenceType.value != RecurrenceType.NONE
        } else {
            // Edit mode - compare with original values
            _hasChanges.value = originalTask?.let { original ->
                val originalRule = RecurrenceRule.fromTaskFields(
                    original.recurrenceType,
                    original.recurrenceInterval,
                    original.recurrenceDaysOfWeek,
                    original.recurrenceEndDate,
                )
                _taskTitle.value.trim() != original.title ||
                    _taskDescription.value.trim() != original.description ||
                    _dueAt.value != original.dueAt ||
                    _dueAtHasTime.value != original.dueAtHasTime ||
                    _reminderOption.value != ReminderOption.fromOffsetMinutes(original.reminderOffsetMinutes) ||
                    _tag.value.trim() != (original.tag ?: "") ||
                    _priority.value != original.priority ||
                    _isPinned.value != original.isPinned ||
                    _recurrenceType.value != originalRule.type ||
                    _recurrenceInterval.value != originalRule.interval ||
                    _recurrenceDaysOfWeek.value != originalRule.daysOfWeek ||
                    _recurrenceEndDate.value != originalRule.endDate
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
            trimmed.isEmpty() && title.isNotEmpty() -> context.getString(R.string.error_title_empty)
            trimmed.length > TaskFormUseCase.MAX_TITLE_LENGTH ->
                context.getString(R.string.error_title_too_long, TaskFormUseCase.MAX_TITLE_LENGTH)
            else -> null
        }
    }

    val dueDateError: Flow<String?> = _dueAt.map { dueAt ->
        when {
            dueAt != null && dueAt <= System.currentTimeMillis() -> context.getString(R.string.error_due_date_future)
            else -> null
        }
    }

    val reminderError: Flow<String?> = combine(_dueAt, _reminderOption) { dueAt, reminderOption ->
        when {
            reminderOption == ReminderOption.NONE -> null
            dueAt == null -> context.getString(R.string.error_due_date_required)
            !isReminderTimeValid(
                dueAt,
                reminderOption.offsetMinutes,
            ) -> context.getString(R.string.error_reminder_future)
            else -> null
        }
    }

    val tagError: Flow<String?> = _tag.map { tag ->
        val trimmed = tag.trim()
        when {
            trimmed.length > TaskFormUseCase.MAX_TAG_LENGTH ->
                context.getString(R.string.error_tag_too_long, TaskFormUseCase.MAX_TAG_LENGTH)
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
                    _dueAtHasTime.value = task.dueAtHasTime
                    _reminderOption.value = ReminderOption.fromOffsetMinutes(task.reminderOffsetMinutes)
                    _tag.value = task.tag ?: ""
                    _priority.value = task.priority
                    _isPinned.value = task.isPinned
                    val rule = RecurrenceRule.fromTaskFields(
                        task.recurrenceType,
                        task.recurrenceInterval,
                        task.recurrenceDaysOfWeek,
                        task.recurrenceEndDate,
                    )
                    _recurrenceType.value = rule.type
                    _recurrenceInterval.value = rule.interval
                    _recurrenceDaysOfWeek.value = rule.daysOfWeek
                    _recurrenceEndDate.value = rule.endDate
                    updateHasChanges()
                } else {
                    _events.emit(TaskEditorEvent.TaskNotFound)
                }
            } catch (e: Exception) {
                _errorMessage.value = context.getString(R.string.error_failed_load_task, e.message ?: "")
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
        if (dueAt == null) {
            _dueAtHasTime.value = false
        }
        updateHasChanges()
    }

    fun updateDueTime(hour: Int, minute: Int) {
        val current = _dueAt.value ?: return
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = current
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        _dueAt.value = calendar.timeInMillis
        _dueAtHasTime.value = true
        // Clear reminder if it would now be in the past
        if (_reminderOption.value != ReminderOption.NONE) {
            val reminderTime = _dueAt.value!! - (_reminderOption.value.offsetMinutes * 60 * 1000L)
            if (reminderTime <= System.currentTimeMillis()) {
                _reminderOption.value = ReminderOption.NONE
            }
        }
        updateHasChanges()
    }

    fun clearDueTime() {
        val current = _dueAt.value ?: return
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = current
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        _dueAt.value = calendar.timeInMillis
        _dueAtHasTime.value = false
        updateHasChanges()
    }

    fun setDueDatePreset(preset: DueDatePreset) {
        _dueAt.value = preset.toEpochMillis()
        _dueAtHasTime.value = false
        // Clear reminder if it would now be in the past
        if (_reminderOption.value != ReminderOption.NONE) {
            val reminderTime = _dueAt.value!! - (_reminderOption.value.offsetMinutes * 60 * 1000L)
            if (reminderTime <= System.currentTimeMillis()) {
                _reminderOption.value = ReminderOption.NONE
            }
        }
        updateHasChanges()
    }

    fun clearDueDate() {
        _dueAt.value = null
        _dueAtHasTime.value = false
        _reminderOption.value = ReminderOption.NONE
        _recurrenceType.value = RecurrenceType.NONE
        _recurrenceInterval.value = 1
        _recurrenceDaysOfWeek.value = emptySet()
        _recurrenceEndDate.value = null
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

    fun updateRecurrenceType(type: RecurrenceType) {
        _recurrenceType.value = type
        if (type == RecurrenceType.NONE) {
            _recurrenceInterval.value = 1
            _recurrenceDaysOfWeek.value = emptySet()
            _recurrenceEndDate.value = null
        }
        updateHasChanges()
    }

    fun updateRecurrenceInterval(interval: Int) {
        if (interval in 1..99) {
            _recurrenceInterval.value = interval
            updateHasChanges()
        }
    }

    fun toggleRecurrenceDayOfWeek(day: DayOfWeek) {
        val current = _recurrenceDaysOfWeek.value.toMutableSet()
        if (day in current) current.remove(day) else current.add(day)
        _recurrenceDaysOfWeek.value = current
        updateHasChanges()
    }

    fun updateRecurrenceEndDate(endDate: Long?) {
        _recurrenceEndDate.value = endDate
        updateHasChanges()
    }

    fun clearRecurrence() {
        updateRecurrenceType(RecurrenceType.NONE)
    }

    fun saveTask() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val trimmedTitle = _taskTitle.value.trim()
                val trimmedDescription = _taskDescription.value.trim()
                val trimmedTag = _tag.value.trim().takeIf { it.isNotEmpty() }
                val resolvedTagColor = trimmedTag?.let { tagManagementUseCase.getTagColor(it) }
                val reminderOffsetMinutes = if (_reminderOption.value ==
                    ReminderOption.NONE
                ) {
                    null
                } else {
                    _reminderOption.value.offsetMinutes
                }

                val recurrenceRule = RecurrenceRule(
                    type = _recurrenceType.value,
                    interval = _recurrenceInterval.value,
                    daysOfWeek = _recurrenceDaysOfWeek.value,
                    endDate = _recurrenceEndDate.value,
                )

                if (isEditMode && originalTask != null) {
                    // Update existing task
                    val updatedTask = originalTask!!.copy(
                        title = trimmedTitle,
                        description = trimmedDescription,
                        dueAt = _dueAt.value,
                        dueAtHasTime = _dueAtHasTime.value,
                        reminderOffsetMinutes = reminderOffsetMinutes,
                        tag = trimmedTag,
                        tagColor = resolvedTagColor,
                        priority = _priority.value,
                        isPinned = _isPinned.value,
                        recurrenceType = recurrenceRule.type.value,
                        recurrenceInterval = recurrenceRule.interval,
                        recurrenceDaysOfWeek = recurrenceRule.daysOfWeekBitmask(),
                        recurrenceEndDate = recurrenceRule.endDate,
                    )
                    taskManager.updateTask(updatedTask)
                } else {
                    // Create new task
                    val taskId = taskManager.createTask(
                        title = trimmedTitle,
                        description = trimmedDescription,
                        dueAt = _dueAt.value,
                        dueAtHasTime = _dueAtHasTime.value,
                        reminderOffsetMinutes = reminderOffsetMinutes,
                        tag = trimmedTag,
                        recurrenceType = recurrenceRule.type.value,
                        recurrenceInterval = recurrenceRule.interval,
                        recurrenceDaysOfWeek = recurrenceRule.daysOfWeekBitmask(),
                        recurrenceEndDate = recurrenceRule.endDate,
                    )

                    if (_priority.value != 1) {
                        taskManager.setPriority(taskId, _priority.value)
                    }
                    if (_isPinned.value) {
                        taskManager.setPinned(taskId, true)
                    }
                    if (resolvedTagColor != null) {
                        val createdTask = taskManager.getTaskById(taskId)
                        if (createdTask != null) {
                            taskManager.updateTask(createdTask.copy(tagColor = resolvedTagColor))
                        }
                    }
                }

                _events.emit(TaskEditorEvent.TaskSaved)
            } catch (e: Exception) {
                _errorMessage.value = context.getString(R.string.error_failed_save_task, e.message ?: "")
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
        0 -> context.getString(R.string.priority_low)
        1 -> context.getString(R.string.priority_medium)
        2 -> context.getString(R.string.priority_high)
        else -> context.getString(R.string.priority_medium)
    }

    /**
     * Get all available priorities
     */
    fun getAllPriorities(): List<Pair<Int, String>> = listOf(
        0 to context.getString(R.string.priority_low),
        1 to context.getString(R.string.priority_medium),
        2 to context.getString(R.string.priority_high),
    )
}

/**
 * Events emitted by TaskEditorViewModel
 */
sealed class TaskEditorEvent {
    object TaskSaved : TaskEditorEvent()
    object TaskNotFound : TaskEditorEvent()
}
