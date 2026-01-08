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
 * Manager for CRUD operations on tasks.
 * Coordinates between form validation and CRUD use case operations.
 * Handles business logic for task creation, updating, deletion, and completion toggling.
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
}

/**
 * Sealed class representing the result of a task operation
 */
sealed class TaskOperationResult {
    data class Success(val message: String) : TaskOperationResult()
    data class ValidationError(val message: String) : TaskOperationResult()
    data class CrudError(val message: String) : TaskOperationResult()
}