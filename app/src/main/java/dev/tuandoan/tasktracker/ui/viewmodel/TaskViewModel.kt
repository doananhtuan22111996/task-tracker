package dev.tuandoan.tasktracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.ITaskManager
import dev.tuandoan.tasktracker.domain.model.CompletedGrouping
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
    private val listStateManager: TaskListStateManager,
    private val crudManager: TaskCrudManager,
    private val selectionStateManager: TaskSelectionStateManager,
    private val bulkActionManager: TaskBulkActionManager,
    private val taskManager: ITaskManager,
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

    // Grouped tasks for day-based sections
    val groupedVisibleTasks: StateFlow<List<TaskSection>> = visibleTasks
        .map { tasks -> TaskDateGrouper.groupTasksByDay(tasks) }
        .stateIn(
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
                            message = "Task archived",
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

    fun toggleTaskCompletion(task: Task) {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.toggleTaskCompletion(task) },
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

    // === Reorder Operations ===

    /**
     * Whether drag-and-drop reorder is currently enabled.
     * Enabled only when sort mode is MANUAL, search is not active, and not in selection mode.
     */
    val isDragEnabled: StateFlow<Boolean> = combine(
        taskSort,
        hasActiveSearch,
        isSelectionMode,
    ) { sort, searching, selecting ->
        sort.key == SortKey.MANUAL && !searching && !selecting
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false,
    )

    /**
     * Persist the reordered task list for a given section.
     * Preserves globally unique sortIndex values by collecting the original
     * sortIndex values from the section's tasks, sorting them, and reassigning
     * in the new order. This prevents index collisions across different sections.
     */
    fun reorderTasks(sectionDateKey: String, reorderedTaskIds: List<Long>) {
        viewModelScope.launch {
            try {
                // Get current tasks to read their existing sortIndex values
                val allCurrentTasks = allTasks.value
                val taskMap = allCurrentTasks.associateBy { it.id }

                // Collect original sortIndex values for tasks in this section, sorted ascending
                val originalSortIndices = reorderedTaskIds
                    .mapNotNull { id -> taskMap[id]?.sortIndex }
                    .sorted()

                // Assign the sorted original indices to the new task order
                val updates = reorderedTaskIds.mapIndexed { index, taskId ->
                    val newSortIndex = if (index < originalSortIndices.size) {
                        originalSortIndices[index]
                    } else {
                        index.toLong()
                    }
                    taskId to newSortIndex
                }

                taskManager.updateSortIndices(updates)
            } catch (e: Exception) {
                _singleTaskUiEvent.emit(
                    UiEvent.ShowSnackbar("Failed to save task order"),
                )
            }
        }
    }

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
