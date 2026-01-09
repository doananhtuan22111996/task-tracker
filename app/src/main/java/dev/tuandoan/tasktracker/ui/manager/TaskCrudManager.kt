package dev.tuandoan.tasktracker.ui.manager

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.usecase.TaskCrudUseCase
import dev.tuandoan.tasktracker.ui.state.FormValidationResult
import dev.tuandoan.tasktracker.ui.state.TaskFormStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Centralized manager for all CRUD (Create, Read, Update, Delete) operations on tasks.
 *
 * ## Overview
 * This manager serves as the primary coordinator between the UI layer and the domain layer
 * for all task-related data operations. It handles both individual task operations and
 * bulk operations, providing a unified API with consistent error handling and validation.
 *
 * ## Key Responsibilities
 * - **Individual CRUD Operations**: Create, update, delete, and toggle individual tasks
 * - **Bulk Operations**: Efficient batch operations on multiple tasks
 * - **Form Integration**: Coordinates with TaskFormStateManager for validation
 * - **Error Management**: Unified error handling with categorized error types
 * - **State Coordination**: Manages combined error state from multiple sources
 *
 * ## Architecture Position
 * ```
 * UI Layer (ViewModel/Manager)
 *           ↓
 *    TaskCrudManager ←→ TaskFormStateManager
 *           ↓
 *    Domain Layer (Use Cases)
 *           ↓
 *    Data Layer (Repository/DAO)
 * ```
 *
 * ## Error Handling Philosophy
 * This manager implements a three-tier error handling system:
 * - **ValidationError**: Input validation failures (user can fix)
 * - **CrudError**: Business logic or data layer failures
 * - **Success**: Operation completed successfully with message
 *
 * ## Performance Optimizations
 * - Bulk operations use efficient database queries with IN clauses
 * - Input validation prevents unnecessary database calls
 * - Proper transaction management for data consistency
 * - Maximum batch size limits prevent UI blocking
 *
 * ## Thread Safety
 * All methods in this manager are suspending functions designed to be called
 * from coroutine contexts. The manager itself is stateless except for
 * injected dependencies, making it inherently thread-safe.
 *
 * @since 2.0.0 (Enhanced in 2.2.0 with bulk operations)
 * @see TaskOperationResult
 * @see TaskFormStateManager
 * @see TaskCrudUseCase
 */
class TaskCrudManager @Inject constructor(
    private val crudUseCase: TaskCrudUseCase,
    private val formStateManager: TaskFormStateManager
) {

    /**
     * Initialize error state flow combining CRUD and form errors
     */
    fun initializeErrorState(scope: CoroutineScope): StateFlow<String?> {
        return combine(
            crudUseCase.errorMessage,
            formStateManager.initializeStateFlows(scope).errorMessage
        ) { crudError, formError ->
            crudError ?: formError
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    /**
     * Creates a new task with validation
     */
    suspend fun createTask(scope: CoroutineScope): TaskOperationResult {
        return when (val validation = formStateManager.validateForm()) {
            is FormValidationResult.Error -> {
                formStateManager.setError(validation.message)
                TaskOperationResult.ValidationError(validation.message)
            }
            is FormValidationResult.Success -> {
                val result = crudUseCase.createTask(
                    validation.formData.title,
                    validation.formData.description
                )

                if (result.isSuccess) {
                    formStateManager.hideAddTaskDialog()
                    TaskOperationResult.Success("Task created successfully")
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Failed to create task"
                    TaskOperationResult.CrudError(errorMessage)
                }
            }
        }
    }

    /**
     * Updates an existing task with validation
     */
    suspend fun updateTask(scope: CoroutineScope): TaskOperationResult {
        return when (val validation = formStateManager.validateForm()) {
            is FormValidationResult.Error -> {
                formStateManager.setError(validation.message)
                TaskOperationResult.ValidationError(validation.message)
            }
            is FormValidationResult.Success -> {
                val taskId = validation.formData.selectedTaskId
                if (taskId == null) {
                    val error = "No task selected for update"
                    formStateManager.setError(error)
                    return TaskOperationResult.ValidationError(error)
                }

                val result = crudUseCase.updateTask(
                    taskId, // taskId is smart-cast to non-null Long after the null check
                    validation.formData.title,
                    validation.formData.description
                )

                if (result.isSuccess) {
                    formStateManager.hideAddTaskDialog()
                    TaskOperationResult.Success("Task updated successfully")
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Failed to update task"
                    TaskOperationResult.CrudError(errorMessage)
                }
            }
        }
    }

    /**
     * Saves a task (create or update based on form state)
     */
    suspend fun saveTask(scope: CoroutineScope): TaskOperationResult {
        val formData = formStateManager.getFormData()

        return if (formData.selectedTaskId != null) {
            updateTask(scope)
        } else {
            createTask(scope)
        }
    }

    /**
     * Deletes a task
     */
    suspend fun deleteTask(task: Task): TaskOperationResult {
        val result = crudUseCase.deleteTask(task)

        return if (result.isSuccess) {
            TaskOperationResult.Success("Task deleted successfully")
        } else {
            val errorMessage = result.exceptionOrNull()?.message ?: "Failed to delete task"
            TaskOperationResult.CrudError(errorMessage)
        }
    }

    /**
     * Toggles task completion status
     */
    suspend fun toggleTaskCompletion(task: Task): TaskOperationResult {
        val result = crudUseCase.toggleTaskCompletion(task)

        return if (result.isSuccess) {
            val status = if (task.isCompleted) "incomplete" else "complete"
            TaskOperationResult.Success("Task marked as $status")
        } else {
            val errorMessage = result.exceptionOrNull()?.message ?: "Failed to update task status"
            TaskOperationResult.CrudError(errorMessage)
        }
    }

    /**
     * Restores a deleted task
     */
    suspend fun restoreTask(task: Task): TaskOperationResult {
        val result = crudUseCase.restoreTask(task)

        return if (result.isSuccess) {
            TaskOperationResult.Success("Task restored successfully")
        } else {
            val errorMessage = result.exceptionOrNull()?.message ?: "Failed to restore task"
            TaskOperationResult.CrudError(errorMessage)
        }
    }

    /**
     * Bulk mark tasks as completed
     * @param taskIds List of task IDs to mark as completed
     * @throws IllegalArgumentException if input validation fails
     */
    suspend fun bulkMarkCompleted(taskIds: List<Long>): TaskOperationResult {
        return try {
            validateBulkOperationInput(taskIds, "mark as completed")
            crudUseCase.bulkSetCompleted(taskIds, true)
            TaskOperationResult.Success("${taskIds.size} tasks marked as completed")
        } catch (e: IllegalArgumentException) {
            TaskOperationResult.ValidationError(e.message ?: "Invalid input for bulk completion")
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Failed to mark tasks as completed"
            TaskOperationResult.CrudError(errorMessage)
        }
    }

    /**
     * Bulk mark tasks as active (not completed)
     * @param taskIds List of task IDs to mark as active
     * @throws IllegalArgumentException if input validation fails
     */
    suspend fun bulkMarkActive(taskIds: List<Long>): TaskOperationResult {
        return try {
            validateBulkOperationInput(taskIds, "mark as active")
            crudUseCase.bulkSetCompleted(taskIds, false)
            TaskOperationResult.Success("${taskIds.size} tasks marked as active")
        } catch (e: IllegalArgumentException) {
            TaskOperationResult.ValidationError(e.message ?: "Invalid input for bulk activation")
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Failed to mark tasks as active"
            TaskOperationResult.CrudError(errorMessage)
        }
    }

    /**
     * Bulk delete tasks by IDs
     * @param taskIds List of task IDs to delete
     * @throws IllegalArgumentException if input validation fails
     */
    suspend fun bulkDeleteTasks(taskIds: List<Long>): TaskOperationResult {
        return try {
            validateBulkOperationInput(taskIds, "delete")
            crudUseCase.bulkDeleteTasks(taskIds)
            TaskOperationResult.Success("${taskIds.size} tasks deleted successfully")
        } catch (e: IllegalArgumentException) {
            TaskOperationResult.ValidationError(e.message ?: "Invalid input for bulk deletion")
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Failed to delete tasks"
            TaskOperationResult.CrudError(errorMessage)
        }
    }

    /**
     * Restore multiple deleted tasks
     * @param tasks List of tasks to restore
     * @throws IllegalArgumentException if input validation fails
     */
    suspend fun restoreTasks(tasks: List<Task>): TaskOperationResult {
        return try {
            validateTaskListInput(tasks, "restore")
            crudUseCase.restoreTasks(tasks)
            TaskOperationResult.Success("${tasks.size} tasks restored successfully")
        } catch (e: IllegalArgumentException) {
            TaskOperationResult.ValidationError(e.message ?: "Invalid input for task restoration")
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Failed to restore tasks"
            TaskOperationResult.CrudError(errorMessage)
        }
    }

    /**
     * Clears all errors from both CRUD and form state
     */
    fun clearAllErrors() {
        crudUseCase.clearError()
        formStateManager.clearError()
    }

    /**
     * Executes a CRUD operation in the provided scope with error handling
     */
    fun executeOperation(
        scope: CoroutineScope,
        operation: suspend () -> TaskOperationResult,
        onSuccess: ((String) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        scope.launch {
            when (val result = operation()) {
                is TaskOperationResult.Success -> onSuccess?.invoke(result.message)
                is TaskOperationResult.ValidationError -> onError?.invoke(result.message)
                is TaskOperationResult.CrudError -> onError?.invoke(result.message)
            }
        }
    }

    // === Validation Helper Methods ===

    /**
     * Validates input for bulk operations on task IDs
     * @param taskIds List of task IDs to validate
     * @param operationName Name of the operation for error messages
     * @throws IllegalArgumentException if validation fails
     */
    private fun validateBulkOperationInput(taskIds: List<Long>, operationName: String) {
        require(taskIds.isNotEmpty()) { "Cannot $operationName: task IDs list is empty" }
        require(taskIds.size <= MAX_BULK_OPERATION_SIZE) {
            "Cannot $operationName more than $MAX_BULK_OPERATION_SIZE tasks at once (requested: ${taskIds.size})"
        }
        require(taskIds.all { it > 0 }) {
            "Cannot $operationName: invalid task IDs found: ${taskIds.filter { it <= 0 }}"
        }
        require(taskIds.distinct().size == taskIds.size) {
            "Cannot $operationName: duplicate task IDs found"
        }
    }

    /**
     * Validates input for operations on task objects
     * @param tasks List of tasks to validate
     * @param operationName Name of the operation for error messages
     * @throws IllegalArgumentException if validation fails
     */
    private fun validateTaskListInput(tasks: List<Task>, operationName: String) {
        require(tasks.isNotEmpty()) { "Cannot $operationName: tasks list is empty" }
        require(tasks.size <= MAX_BULK_OPERATION_SIZE) {
            "Cannot $operationName more than $MAX_BULK_OPERATION_SIZE tasks at once (requested: ${tasks.size})"
        }
        require(tasks.all { it.id > 0 }) {
            "Cannot $operationName: invalid task IDs found: ${tasks.filter { it.id <= 0 }.map { it.id }}"
        }
        require(tasks.map { it.id }.distinct().size == tasks.size) {
            "Cannot $operationName: duplicate tasks found"
        }
    }

    companion object {
        private const val MAX_BULK_OPERATION_SIZE = 500 // Consistent with TaskBulkActionManager
    }
}

/**
 * Sealed class representing the result of a task operation with comprehensive error categorization.
 *
 * This sealed class provides a type-safe way to handle different outcomes of task operations,
 * enabling proper error handling and user feedback throughout the application.
 *
 * ## Usage Pattern
 * ```kotlin
 * when (val result = taskCrudManager.createTask(scope)) {
 *     is TaskOperationResult.Success -> {
 *         // Show success message to user
 *         showSnackbar(result.message)
 *     }
 *     is TaskOperationResult.ValidationError -> {
 *         // Handle user input errors (fixable)
 *         showFieldError(result.message)
 *     }
 *     is TaskOperationResult.CrudError -> {
 *         // Handle system/database errors
 *         showErrorDialog(result.message)
 *     }
 * }
 * ```
 *
 * ## Error Categories
 * The three result types represent different categories of outcomes, each requiring
 * different handling strategies in the UI layer.
 *
 * @since 2.0.0
 */
sealed class TaskOperationResult {
    /**
     * Represents a successful operation with a user-friendly message.
     *
     * @param message A descriptive success message suitable for display to users,
     *                typically shown in a snackbar or success notification.
     */
    data class Success(val message: String) : TaskOperationResult()

    /**
     * Represents a validation error that can typically be fixed by user action.
     *
     * These errors occur due to invalid input data, form validation failures,
     * or business rule violations that the user can correct.
     *
     * @param message A user-friendly error message explaining what needs to be fixed.
     */
    data class ValidationError(val message: String) : TaskOperationResult()

    /**
     * Represents a CRUD or system-level error that is not directly user-fixable.
     *
     * These errors typically occur due to database issues, network problems,
     * or other system-level failures that require different handling than
     * validation errors.
     *
     * @param message A descriptive error message suitable for user display,
     *                though may require technical intervention to resolve.
     */
    data class CrudError(val message: String) : TaskOperationResult()
}