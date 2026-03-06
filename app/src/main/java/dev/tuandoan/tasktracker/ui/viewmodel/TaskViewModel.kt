package dev.tuandoan.tasktracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.model.CompletedGrouping
import dev.tuandoan.tasktracker.domain.model.Priority
import dev.tuandoan.tasktracker.domain.model.SortDirection
import dev.tuandoan.tasktracker.domain.model.SortKey
import dev.tuandoan.tasktracker.domain.model.TaskSort
import dev.tuandoan.tasktracker.ui.events.UiEvent
import dev.tuandoan.tasktracker.ui.manager.TaskBulkActionManager
import dev.tuandoan.tasktracker.ui.manager.TaskCrudManager
import dev.tuandoan.tasktracker.ui.state.SelectionState
import dev.tuandoan.tasktracker.ui.state.TaskListState
import dev.tuandoan.tasktracker.ui.state.TaskListStateManager
import dev.tuandoan.tasktracker.ui.state.TaskSelectionStateManager
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter
import dev.tuandoan.tasktracker.utils.TaskDateGrouper
import dev.tuandoan.tasktracker.utils.TaskSection
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
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
 * - Manage selection and bulk operations
 */
@HiltViewModel
class TaskViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val listStateManager: TaskListStateManager,
    private val crudManager: TaskCrudManager,
    private val selectionStateManager: TaskSelectionStateManager,
    private val bulkActionManager: TaskBulkActionManager,
) : ViewModel() {

    // Initialize state from managers
    private val listState: TaskListState = listStateManager.initializeStateFlows(viewModelScope)
    private val selectionState: SelectionState = selectionStateManager.initializeStateFlows(viewModelScope)

    // === Exposed State Flows ===

    // List state
    val allTasks = listState.allTasks
    val visibleTasks = listState.visibleTasks

    // Archived tasks - get from domain layer
    val archivedTasks: StateFlow<List<Task>> = crudManager.getArchivedTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    // Grouped tasks for day-based sections (or priority-based when sorted by priority)
    val groupedVisibleTasks: StateFlow<List<TaskSection>> = combine(
        visibleTasks,
        listState.taskSort,
    ) { tasks, sort ->
        if (sort.key == SortKey.PRIORITY) {
            TaskDateGrouper.groupByPriority(tasks)
        } else {
            TaskDateGrouper.groupTasksByDay(tasks, context)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    val searchQuery = listState.searchQuery
    val filter = listState.filter
    val tagFilter = listState.tagFilter
    val taskSort = listState.taskSort
    val hasActiveSearch = listState.hasActiveSearch
    val hasActiveFilter = listState.hasActiveFilter
    val hasActiveTagFilter = listState.hasActiveTagFilter
    val isLoading = listState.isLoading

    // Available tags computed from all tasks
    val availableTags: StateFlow<List<String>> = allTasks
        .map { tasks ->
            tasks.mapNotNull { it.tag }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    // Form state - moved to dedicated TaskEditorViewModel for screen-based editor

    // Selection state (from manager)
    val selectedIds = selectionState.selectedIds
    val isSelectionMode = selectionState.isSelectionMode
    val selectedCount = selectionState.selectedCount

    // Bulk action state
    val pendingBulkDeleteTasks = bulkActionManager.pendingBulkDeleteTasks
    val pendingBulkArchiveTasks = bulkActionManager.pendingBulkArchiveTasks

    // Combined error state
    val errorMessage = crudManager.initializeErrorState(viewModelScope)

    // === Delete Confirmation State ===

    // StateFlow for pending delete confirmation (single task)
    private val _pendingDeleteTask = MutableStateFlow<Task?>(null)
    val pendingDeleteTask: StateFlow<Task?> = _pendingDeleteTask.asStateFlow()

    // Combined UI events from bulk manager and single operations
    private val _singleTaskUiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = merge(
        bulkActionManager.uiEvent,
        _singleTaskUiEvent.asSharedFlow(),
    ).shareIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 0,
    )

    // === CRUD Operations ===

    fun createTask() {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.createTask(viewModelScope) },
        )
    }

    fun updateTask() {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.updateTask(viewModelScope) },
        )
    }

    fun saveTask() {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.saveTask(viewModelScope) },
        )
    }

    fun deleteTask(task: Task) {
        _pendingDeleteTask.value = task
    }

    fun confirmDeleteTask() {
        val task = _pendingDeleteTask.value ?: return
        _pendingDeleteTask.value = null

        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.deleteTask(task) },
            onSuccess = {
                // Show undo snackbar after successful deletion
                viewModelScope.launch {
                    _singleTaskUiEvent.emit(
                        UiEvent.ShowUndoDelete(
                            tasks = listOf(task),
                            onUndo = { restoreTask(task) },
                        ),
                    )
                }
            },
        )
    }

    fun cancelDeleteTask() {
        _pendingDeleteTask.value = null
    }

    // === Archive Operations ===

    fun archiveTask(task: Task) {
        _pendingDeleteTask.value = task
    }

    fun confirmArchiveTask() {
        val task = _pendingDeleteTask.value ?: return
        _pendingDeleteTask.value = null

        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.archiveTask(task) },
            onSuccess = {
                // Show undo snackbar after successful archive
                viewModelScope.launch {
                    _singleTaskUiEvent.emit(
                        UiEvent.ShowUndoDelete(
                            tasks = listOf(task),
                            onUndo = { unarchiveTask(task) },
                            message = context.getString(R.string.snackbar_task_archived),
                        ),
                    )
                }
            },
        )
    }

    private fun unarchiveTask(task: Task) {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.unarchiveTask(task) },
        )
    }

    // === Archive Management Operations ===

    fun restoreArchivedTask(task: Task) {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.unarchiveTask(task) },
        )
    }

    fun requestPermanentDeleteTask(task: Task) {
        _pendingDeleteTask.value = task
    }

    fun confirmPermanentDeleteTask() {
        val task = _pendingDeleteTask.value ?: return
        _pendingDeleteTask.value = null

        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.hardDeleteTask(task) },
        )
    }

    fun bulkRestoreArchived() = bulkActionManager.bulkRestoreArchived(viewModelScope)

    fun requestBulkPermanentDelete() = bulkActionManager.requestBulkPermanentDelete(archivedTasks.value)

    fun confirmBulkPermanentDelete() = bulkActionManager.confirmBulkPermanentDelete(viewModelScope)

    fun requestBulkArchive() = bulkActionManager.requestBulkArchive(allTasks.value)

    fun confirmBulkArchive() = bulkActionManager.confirmBulkArchive(viewModelScope)

    fun cancelBulkArchive() = bulkActionManager.cancelBulkArchive()

    private fun restoreTask(task: Task) {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.restoreTask(task) },
        )
    }

    fun duplicateTask(taskId: Long) {
        val task = allTasks.value.find { it.id == taskId } ?: return
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.duplicateTask(task) },
            onSuccess = { message ->
                viewModelScope.launch {
                    _singleTaskUiEvent.emit(
                        UiEvent.ShowSnackbar(message = message),
                    )
                }
            },
        )
    }

    fun quickCreateTask(title: String, priority: Priority, dueAt: Long?) {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.quickCreateTask(title, priority.value, dueAt) },
        )
    }

    fun toggleTaskCompletion(task: Task) {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.toggleTaskCompletion(task) },
            onSuccess = { message ->
                viewModelScope.launch {
                    _singleTaskUiEvent.emit(
                        UiEvent.ShowSnackbar(
                            message = message,
                            actionLabel = context.getString(R.string.action_undo),
                            onActionClick = {
                                undoToggleCompletion(task)
                            },
                        ),
                    )
                }
            },
        )
    }

    private fun undoToggleCompletion(task: Task) {
        // After the original toggle, the task's isCompleted has been flipped.
        // To undo, we toggle again with the post-toggle state so it flips back.
        val postToggleTask = task.copy(isCompleted = !task.isCompleted)
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.toggleTaskCompletion(postToggleTask) },
        )
    }

    fun toggleTaskPin(task: Task) {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.toggleTaskPin(task) },
        )
    }

    fun updateTaskPriority(taskId: Long, priority: Int) {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.updateTaskPriority(taskId, priority) },
        )
    }

    fun cyclePriority(taskId: Long) {
        viewModelScope.launch {
            val tasks = allTasks.value
            val task = tasks.find { it.id == taskId } ?: return@launch
            val currentPriority = Priority.fromValue(task.priority)
            val next = when (currentPriority) {
                Priority.LOW -> Priority.MEDIUM
                Priority.MEDIUM -> Priority.HIGH
                Priority.HIGH -> Priority.LOW
            }
            updateTaskPriority(taskId, next.value)
        }
    }

    // === Inline Editing ===

    val inlineEditingTaskId: StateFlow<Long?> = listStateManager.inlineEditingTaskId

    fun startInlineEdit(taskId: Long) = listStateManager.startInlineEdit(taskId)

    fun commitInlineEdit(taskId: Long, newTitle: String) {
        listStateManager.clearInlineEdit()
        if (newTitle.isBlank()) return
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.updateTitle(taskId, newTitle.trim()) },
        )
    }

    fun cancelInlineEdit() = listStateManager.clearInlineEdit()

    // === Selection Management ===

    fun enterSelection(taskId: Long) = selectionStateManager.enterSelection(taskId)

    fun toggleSelection(taskId: Long) = selectionStateManager.toggleSelection(taskId)

    fun clearSelection() = selectionStateManager.clearSelection()

    fun selectAll(currentVisibleTaskIds: List<Long>) = selectionStateManager.selectAll(currentVisibleTaskIds)

    // === Bulk Actions ===

    fun bulkMarkCompleted() = bulkActionManager.bulkMarkCompleted(viewModelScope)

    fun bulkMarkActive() = bulkActionManager.bulkMarkActive(viewModelScope)

    fun requestBulkDelete() = bulkActionManager.requestBulkDelete(allTasks.value)

    fun confirmBulkDelete() = bulkActionManager.confirmBulkDelete(viewModelScope)

    fun cancelBulkDelete() = bulkActionManager.cancelBulkDelete()

    // === Form Management ===
    // Note: Form methods moved to dedicated TaskEditorViewModel for screen-based editor

    // === Search Operations ===

    fun updateSearchQuery(query: String) = listStateManager.updateSearchQuery(query)
    fun clearSearch() = listStateManager.clearSearch()

    // === Filter Operations ===

    fun setFilter(filter: TaskFilter) = listStateManager.setFilter(filter)
    fun setTagFilter(tag: String?) = listStateManager.setTagFilter(tag)
    fun clearTagFilter() = listStateManager.clearTagFilter()

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
