package dev.tuandoan.tasktracker.domain.usecase

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.ITaskManager
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskCrudUseCase @Inject constructor(
    private val taskManager: ITaskManager
) {

    // Loading State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error State
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Success State for UI feedback
    private val _lastOperationSuccess = MutableStateFlow<String?>(null)
    val lastOperationSuccess: StateFlow<String?> = _lastOperationSuccess.asStateFlow()

    // All Tasks Data
    fun getAllTasks(): Flow<List<Task>> = taskManager.getAllTasks()

    /**
     * Creates a new task with the provided title and description
     */
    suspend fun createTask(title: String, description: String): Result<Unit> {
        return try {
            _isLoading.value = true
            _errorMessage.value = null

            taskManager.createTask(title = title, description = description)

            _lastOperationSuccess.value = "Task created successfully"
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Failed to create task"
            _errorMessage.value = errorMsg
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Updates an existing task with new title and description
     */
    suspend fun updateTask(taskId: Long, title: String, description: String): Result<Unit> {
        return try {
            _isLoading.value = true
            _errorMessage.value = null

            taskManager.updateTaskContent(
                taskId = taskId,
                title = title,
                description = description
            )

            _lastOperationSuccess.value = "Task updated successfully"
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Failed to update task"
            _errorMessage.value = errorMsg
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Deletes a task
     */
    suspend fun deleteTask(task: Task): Result<Unit> {
        return try {
            _errorMessage.value = null
            taskManager.deleteTask(task)
            _lastOperationSuccess.value = "Task deleted successfully"
            Result.success(Unit)
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Failed to delete task"
            Result.failure(e)
        }
    }

    /**
     * Toggles the completion status of a task
     */
    suspend fun toggleTaskCompletion(task: Task): Result<Unit> {
        return try {
            _errorMessage.value = null
            taskManager.toggleTaskCompletion(task)
            val status = if (!task.isCompleted) "completed" else "marked as active"
            _lastOperationSuccess.value = "Task $status"
            Result.success(Unit)
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Failed to update task"
            Result.failure(e)
        }
    }

    /**
     * Restores a deleted task
     */
    suspend fun restoreTask(task: Task): Result<Unit> {
        return try {
            _errorMessage.value = null
            taskManager.restoreTask(task)
            _lastOperationSuccess.value = "Task restored successfully"
            Result.success(Unit)
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Failed to restore task"
            Result.failure(e)
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _lastOperationSuccess.value = null
    }
}