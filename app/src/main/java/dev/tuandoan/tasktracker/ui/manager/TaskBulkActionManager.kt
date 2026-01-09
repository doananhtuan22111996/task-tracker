package dev.tuandoan.tasktracker.ui.manager

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.ui.events.UiEvent
import dev.tuandoan.tasktracker.ui.state.SelectionValidationResult
import dev.tuandoan.tasktracker.ui.state.TaskSelectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages bulk operations on tasks with comprehensive error handling and user feedback.
 *
 * ## Overview
 * This manager coordinates all bulk operations in the Task Tracker application,
 * ensuring that operations on multiple tasks are performed safely, efficiently,
 * and with proper user feedback. It acts as a bridge between the selection state
 * and the CRUD operations, adding confirmation workflows and undo functionality.
 *
 * ## Key Features
 * - **Bulk Status Changes**: Mark multiple tasks as completed or active
 * - **Bulk Delete with Confirmation**: Safe deletion with confirmation dialog
 * - **Undo Functionality**: Restore accidentally deleted tasks
 * - **UI Event Management**: Emit events for user notifications (snackbars)
 * - **Comprehensive Validation**: Input validation and error handling
 * - **Performance Optimization**: Limited batch sizes for UI responsiveness
 *
 * ## Architecture Integration
 * ```
 * UI Layer (TaskViewModel)
 *        ↓
 * TaskBulkActionManager ←→ TaskSelectionStateManager
 *        ↓
 * TaskCrudManager
 *        ↓
 * Domain Layer (Use Cases)
 * ```
 *
 * ## Error Handling Strategy
 * - **Validation Errors**: Input validation failures (user-fixable)
 * - **CRUD Errors**: Database or business logic failures
 * - **UI Events**: Automatic user notification via UiEvent system
 * - **Graceful Degradation**: Partial success handling where possible
 *
 * ## Performance Considerations
 * - Maximum bulk operation size: [MAX_BULK_OPERATION_SIZE]
 * - Operations are performed asynchronously with proper coroutine handling
 * - State updates are atomic to prevent UI inconsistencies
 *
 * @since 2.2.0
 * @see TaskSelectionStateManager
 * @see TaskCrudManager
 * @see UiEvent
 */
@Singleton
class TaskBulkActionManager @Inject constructor(
    private val crudManager: TaskCrudManager,
    private val selectionStateManager: TaskSelectionStateManager
) {

    // === Constants ===

    companion object {
        private const val MAX_BULK_OPERATION_SIZE = 500 // Reasonable limit for bulk operations
    }

    // === Bulk Delete Confirmation State ===

    private val _pendingBulkDeleteTasks = MutableStateFlow<List<Task>>(emptyList())
    val pendingBulkDeleteTasks: StateFlow<List<Task>> = _pendingBulkDeleteTasks.asStateFlow()

    // === UI Events ===

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    // === Bulk Operations ===

    /**
     * Marks all currently selected tasks as completed in a bulk operation.
     *
     * This method validates the current selection, performs the bulk completion
     * operation, and provides user feedback through UI events and callbacks.
     * The selection is automatically cleared upon successful completion.
     *
     * @param scope Coroutine scope for asynchronous execution. Should be tied
     *              to the calling component's lifecycle (typically viewModelScope).
     * @param onSuccess Optional callback executed on successful completion.
     *                  Receives a success message as parameter.
     * @param onError Optional callback executed on operation failure.
     *                Receives an error message as parameter.
     *
     * ## Behavior
     * - Validates current selection (must not be empty)
     * - Performs database bulk update operation
     * - Clears selection on success
     * - Emits UI events for user feedback (snackbar)
     * - Handles all error scenarios gracefully
     *
     * ## Error Handling
     * - **No Selection**: Shows "No tasks selected" message
     * - **Validation Errors**: Invalid task IDs or constraint violations
     * - **CRUD Errors**: Database or business logic failures
     *
     * @see bulkMarkActive
     * @see SelectionValidationResult
     */
    fun bulkMarkCompleted(
        scope: CoroutineScope,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        executeBulkOperation(
            scope = scope,
            operation = { taskIds ->
                crudManager.bulkMarkCompleted(taskIds)
            },
            successMessage = { count -> "$count tasks marked as completed" },
            errorMessage = "Failed to mark tasks as completed",
            clearSelectionOnSuccess = true,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    /**
     * Marks selected tasks as active (not completed)
     * @param scope Coroutine scope for execution
     * @param onSuccess Callback executed on successful completion
     * @param onError Callback executed on error
     */
    fun bulkMarkActive(
        scope: CoroutineScope,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        executeBulkOperation(
            scope = scope,
            operation = { taskIds ->
                crudManager.bulkMarkActive(taskIds)
            },
            successMessage = { count -> "$count tasks marked as active" },
            errorMessage = "Failed to mark tasks as active",
            clearSelectionOnSuccess = true,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    /**
     * Initiates bulk delete by showing confirmation dialog
     * @param allTasks List of all tasks to filter selected ones from
     * @throws IllegalArgumentException if input validation fails
     */
    fun requestBulkDelete(allTasks: List<Task>) {
        require(allTasks.isNotEmpty()) { "All tasks list cannot be empty" }

        val validation = selectionStateManager.validateSelection()

        when (validation) {
            is SelectionValidationResult.Empty -> {
                // No selection, nothing to delete
                return
            }
            is SelectionValidationResult.SingleItem -> {
                // Single item selection - find the task
                val task = allTasks.find { it.id == validation.taskId }
                if (task != null) {
                    _pendingBulkDeleteTasks.value = listOf(task)
                } else {
                    throw IllegalStateException(
                        "Selected task with ID ${validation.taskId} not found in task list"
                    )
                }
            }
            is SelectionValidationResult.MultipleItems -> {
                // Multiple items selection
                val selectedTasks = allTasks.filter { it.id in validation.taskIds }

                if (selectedTasks.size != validation.taskIds.size) {
                    val missingIds = validation.taskIds.filter { selectedId ->
                        allTasks.none { it.id == selectedId }
                    }
                    throw IllegalStateException(
                        "Some selected tasks not found in task list. Missing IDs: $missingIds"
                    )
                }

                _pendingBulkDeleteTasks.value = selectedTasks
            }
        }
    }

    /**
     * Confirms bulk delete and performs the operation
     * @param scope Coroutine scope for execution
     * @throws IllegalArgumentException if preconditions are not met
     */
    fun confirmBulkDelete(scope: CoroutineScope) {
        val tasksToDelete = _pendingBulkDeleteTasks.value
        if (tasksToDelete.isEmpty()) {
            throw IllegalStateException("No tasks pending deletion")
        }

        if (tasksToDelete.any { it.id <= 0 }) {
            throw IllegalStateException(
                "Invalid task IDs found: ${tasksToDelete.filter { it.id <= 0 }.map { it.id }}"
            )
        }

        val taskIds = tasksToDelete.map { it.id }
        _pendingBulkDeleteTasks.value = emptyList()

        scope.launch {
            try {
                val result = crudManager.bulkDeleteTasks(taskIds)

                when (result) {
                    is TaskOperationResult.Success -> {
                        // Clear selection and show undo snackbar
                        selectionStateManager.clearSelection()

                        _uiEvent.emit(
                            UiEvent.ShowUndoDelete(
                                tasks = tasksToDelete,
                                onUndo = { restoreTasks(scope, tasksToDelete) }
                            )
                        )
                    }
                    is TaskOperationResult.CrudError -> {
                        _uiEvent.emit(
                            UiEvent.ShowSnackbar(
                                message = result.message
                            )
                        )
                    }
                    is TaskOperationResult.ValidationError -> {
                        _uiEvent.emit(
                            UiEvent.ShowSnackbar(
                                message = result.message
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        message = "Failed to delete tasks: ${e.message}"
                    )
                )
            }
        }
    }

    /**
     * Cancels bulk delete operation
     */
    fun cancelBulkDelete() {
        _pendingBulkDeleteTasks.value = emptyList()
    }

    /**
     * Restores deleted tasks
     * @param scope Coroutine scope for execution
     * @param tasks Tasks to restore
     */
    private fun restoreTasks(scope: CoroutineScope, tasks: List<Task>) {
        scope.launch {
            try {
                val result = crudManager.restoreTasks(tasks)

                when (result) {
                    is TaskOperationResult.Success -> {
                        _uiEvent.emit(
                            UiEvent.ShowSnackbar(
                                message = result.message
                            )
                        )
                    }
                    is TaskOperationResult.CrudError -> {
                        _uiEvent.emit(
                            UiEvent.ShowSnackbar(
                                message = "Failed to restore tasks: ${result.message}"
                            )
                        )
                    }
                    is TaskOperationResult.ValidationError -> {
                        _uiEvent.emit(
                            UiEvent.ShowSnackbar(
                                message = result.message
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        message = "Failed to restore tasks: ${e.message}"
                    )
                )
            }
        }
    }

    // === Helper Methods ===

    /**
     * Generic bulk operation executor with comprehensive error handling
     * @param scope Coroutine scope for execution
     * @param operation The bulk operation to execute
     * @param successMessage Function to generate success message
     * @param errorMessage Base error message for failures
     * @param clearSelectionOnSuccess Whether to clear selection on success
     * @param onSuccess Callback for success
     * @param onError Callback for errors
     */
    private fun executeBulkOperation(
        scope: CoroutineScope,
        operation: suspend (List<Long>) -> TaskOperationResult,
        successMessage: (Int) -> String,
        errorMessage: String,
        clearSelectionOnSuccess: Boolean = true,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!scope.coroutineContext[kotlinx.coroutines.Job]?.isActive!!) {
            onError("Operation cancelled: scope is not active")
            return
        }

        val validation = selectionStateManager.validateSelection()

        val taskIds = when (validation) {
            is SelectionValidationResult.Empty -> {
                onError("No tasks selected for bulk operation")
                return
            }
            is SelectionValidationResult.SingleItem -> {
                require(validation.taskId > 0) { "Invalid task ID: ${validation.taskId}" }
                listOf(validation.taskId)
            }
            is SelectionValidationResult.MultipleItems -> {
                require(validation.taskIds.isNotEmpty()) { "Task IDs list cannot be empty" }
                require(validation.taskIds.all { it > 0 }) {
                    "Invalid task IDs found: ${validation.taskIds.filter { it <= 0 }}"
                }
                validation.taskIds
            }
        }

        if (taskIds.size > MAX_BULK_OPERATION_SIZE) {
            onError("Cannot process more than $MAX_BULK_OPERATION_SIZE tasks at once (selected: ${taskIds.size})")
            return
        }

        scope.launch {
            try {
                val result = operation(taskIds)

                when (result) {
                    is TaskOperationResult.Success -> {
                        if (clearSelectionOnSuccess) {
                            selectionStateManager.clearSelection()
                        }

                        val message = successMessage(taskIds.size)
                        onSuccess(message)

                        _uiEvent.emit(UiEvent.ShowSnackbar(message = message))
                    }
                    is TaskOperationResult.CrudError -> {
                        onError(result.message)
                        _uiEvent.emit(UiEvent.ShowSnackbar(message = result.message))
                    }
                    is TaskOperationResult.ValidationError -> {
                        onError(result.message)
                        _uiEvent.emit(UiEvent.ShowSnackbar(message = result.message))
                    }
                }
            } catch (e: Exception) {
                val message = "$errorMessage: ${e.message}"
                onError(message)
                _uiEvent.emit(UiEvent.ShowSnackbar(message = message))
            }
        }
    }

    /**
     * Gets the current bulk delete confirmation state
     */
    fun hasPendingBulkDelete(): Boolean = _pendingBulkDeleteTasks.value.isNotEmpty()

    /**
     * Gets the count of tasks pending deletion
     */
    fun getPendingDeleteCount(): Int = _pendingBulkDeleteTasks.value.size
}