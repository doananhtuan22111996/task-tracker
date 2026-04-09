package dev.tuandoan.tasktracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.data.preferences.SettingsRepository
import dev.tuandoan.tasktracker.data.preferences.UserPreferences
import dev.tuandoan.tasktracker.domain.ITaskManager
import dev.tuandoan.tasktracker.domain.model.TaskSort
import dev.tuandoan.tasktracker.domain.usecase.StreakUseCase
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
import kotlinx.coroutines.flow.first
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
    private val settingsRepository: SettingsRepository,
    private val taskManager: ITaskManager,
    private val streakUseCase: StreakUseCase,
) : ViewModel() {

    // Initialize state from managers
    private val listState: TaskListState = listStateManager.initializeStateFlows(viewModelScope)
    private val selectionState: SelectionState = selectionStateManager.initializeStateFlows(viewModelScope)

    // Streak map: rootChainId → currentStreak count (for badge display)
    private val _streakMap = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val streakMap: StateFlow<Map<Long, Int>> = _streakMap.asStateFlow()

    init {
        viewModelScope.launch { settingsRepository.ensureFirstLaunchDate() }
        viewModelScope.launch {
            val sort = settingsRepository.getSortPreference()
            listStateManager.initializeSort(sort)
        }
        loadStreakMap()
    }

    private fun loadStreakMap() {
        viewModelScope.launch {
            try {
                _streakMap.value = streakUseCase.getStreakMap()
            } catch (_: Exception) {
                _streakMap.value = emptyMap()
            }
        }
    }

    // === Exposed State Flows ===

    // List state
    val allTasks = listState.allTasks
    val visibleTasks = listState.visibleTasks

    // Archived tasks - get from domain layer
    private val _allArchivedTasks: StateFlow<List<Task>> = crudManager.getArchivedTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    // Archived tag filter
    private val _archivedTagFilter = MutableStateFlow<String?>(null)
    val archivedTagFilter: StateFlow<String?> = _archivedTagFilter.asStateFlow()

    // Archived tasks filtered by tag
    val archivedTasks: StateFlow<List<Task>> = combine(
        _allArchivedTasks,
        _archivedTagFilter,
    ) { tasks, tagFilter ->
        if (tagFilter == null) tasks else tasks.filter { it.tag == tagFilter }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    // Available tags from archived tasks
    val archivedAvailableTags: StateFlow<List<String>> = _allArchivedTasks
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

    // Grouped tasks for day-based sections
    val groupedVisibleTasks: StateFlow<List<TaskSection>> = visibleTasks
        .map { tasks -> TaskDateGrouper.groupTasksByDay(tasks, context) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val searchQuery = listState.searchQuery
    val filter = listState.filter
    val tagFilter = listState.tagFilter
    val currentSort = listState.currentSort
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
                loadStreakMap()
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
                loadStreakMap()
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

    fun toggleTaskCompletion(task: Task) {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.toggleTaskCompletion(task) },
            onSuccess = {
                loadStreakMap()
                if (!task.isCompleted) {
                    viewModelScope.launch { checkAndEmitRatingPrompt() }
                }
            },
        )
    }

    private suspend fun checkAndEmitRatingPrompt() {
        val prefs = settingsRepository.userPreferences.first()
        if (prefs.ratingPromptShown) return
        if (prefs.firstLaunchDate == 0L) return

        val completedCount = taskManager.observeCompletedCount().first()
        if (completedCount < RATING_MIN_COMPLETED_TASKS) return

        val daysSinceFirstLaunch =
            (System.currentTimeMillis() - prefs.firstLaunchDate) / MILLIS_PER_DAY
        if (daysSinceFirstLaunch < RATING_MIN_DAYS_SINCE_LAUNCH) return

        _singleTaskUiEvent.emit(UiEvent.ShowRatingPrompt)
        settingsRepository.setRatingPromptShown(true)
    }

    companion object {
        private const val RATING_MIN_COMPLETED_TASKS = 3
        private const val RATING_MIN_DAYS_SINCE_LAUNCH = 3L
        private const val MILLIS_PER_DAY = 86_400_000L
    }

    fun duplicateTask(task: Task) {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.duplicateTask(task) },
            onSuccess = { message ->
                viewModelScope.launch {
                    _singleTaskUiEvent.emit(UiEvent.ShowSnackbar(message))
                }
            },
        )
    }

    fun skipOccurrence(task: Task) {
        crudManager.executeOperation(
            scope = viewModelScope,
            operation = { crudManager.skipOccurrence(task) },
            onSuccess = { message ->
                loadStreakMap()
                viewModelScope.launch {
                    _singleTaskUiEvent.emit(UiEvent.ShowSnackbar(message))
                }
            },
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

    // === Search Operations ===

    fun updateSearchQuery(query: String) = listStateManager.updateSearchQuery(query)
    fun clearSearch() = listStateManager.clearSearch()

    // === Filter Operations ===

    fun setFilter(filter: TaskFilter) = listStateManager.setFilter(filter)
    fun setTagFilter(tag: String?) = listStateManager.setTagFilter(tag)
    fun clearTagFilter() = listStateManager.clearTagFilter()

    // === Sort Operations ===

    fun updateSort(sort: TaskSort) {
        listStateManager.updateSort(sort)
        viewModelScope.launch { settingsRepository.setSortPreference(sort) }
    }

    // === Archived Tag Filter Operations ===

    fun setArchivedTagFilter(tag: String?) {
        _archivedTagFilter.value = tag
    }

    // === Feature Tips ===

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    fun setTipFabShown() {
        viewModelScope.launch { settingsRepository.setTipShown(SettingsRepository.TipKeys.FAB) }
    }

    fun setTipTagChipsShown() {
        viewModelScope.launch { settingsRepository.setTipShown(SettingsRepository.TipKeys.TAG_CHIPS) }
    }

    // === Error Management ===

    fun clearError() = crudManager.clearAllErrors()

    // === Lifecycle Management ===

    override fun onCleared() {
        super.onCleared()
        // Cleanup handled by managers automatically
    }
}
