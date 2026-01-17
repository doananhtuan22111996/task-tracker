package dev.tuandoan.tasktracker.domain.usecase

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.ITaskManager
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskCrudUseCase @Inject constructor(private val taskManager: ITaskManager) {

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
    suspend fun createTask(title: String, description: String): Result<Unit> =
        createTask(title, description, null, null)

    /**
     * Creates a new task with the provided title, description, due date and reminder
     */
    suspend fun createTask(
        title: String,
        description: String,
        dueAt: Long?,
        reminderOffsetMinutes: Int?,
    ): Result<Unit> = createTask(title, description, dueAt, reminderOffsetMinutes, null)

    /**
     * Creates a new task with the provided title, description, due date, reminder and tag
     */
    suspend fun createTask(
        title: String,
        description: String,
        dueAt: Long?,
        reminderOffsetMinutes: Int?,
        tag: String?,
    ): Result<Unit> = try {
        _isLoading.value = true
        _errorMessage.value = null

        taskManager.createTask(
            title = title,
            description = description,
            dueAt = dueAt,
            reminderOffsetMinutes = reminderOffsetMinutes,
            tag = tag,
        )

        _lastOperationSuccess.value = "Task created successfully"
        Result.success(Unit)
    } catch (e: Exception) {
        val errorMsg = e.message ?: "Failed to create task"
        _errorMessage.value = errorMsg
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }

    /**
     * Updates an existing task with new title and description
     */
    suspend fun updateTask(taskId: Long, title: String, description: String): Result<Unit> =
        updateTask(taskId, title, description, null, null)

    /**
     * Updates an existing task with new title, description, due date and reminder
     */
    suspend fun updateTask(
        taskId: Long,
        title: String,
        description: String,
        dueAt: Long?,
        reminderOffsetMinutes: Int?,
    ): Result<Unit> = updateTask(taskId, title, description, dueAt, reminderOffsetMinutes, null)

    /**
     * Updates an existing task with new title, description, due date, reminder and tag
     */
    suspend fun updateTask(
        taskId: Long,
        title: String,
        description: String,
        dueAt: Long?,
        reminderOffsetMinutes: Int?,
        tag: String?,
    ): Result<Unit> = try {
        _isLoading.value = true
        _errorMessage.value = null

        val success = taskManager.updateTaskContent(
            taskId = taskId,
            title = title,
            description = description,
            dueAt = dueAt,
            reminderOffsetMinutes = reminderOffsetMinutes,
            tag = tag,
        )

        if (success) {
            _lastOperationSuccess.value = "Task updated successfully"
            Result.success(Unit)
        } else {
            _errorMessage.value = "Failed to schedule reminder"
            Result.failure(RuntimeException("Failed to schedule reminder"))
        }
    } catch (e: Exception) {
        val errorMsg = e.message ?: "Failed to update task"
        _errorMessage.value = errorMsg
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }

    /**
     * Deletes a task
     */
    suspend fun deleteTask(task: Task): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.deleteTask(task)
        _lastOperationSuccess.value = "Task deleted successfully"
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: "Failed to delete task"
        Result.failure(e)
    }

    /**
     * Toggles the completion status of a task
     */
    suspend fun toggleTaskCompletion(task: Task): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.toggleTaskCompletion(task)
        val status = if (!task.isCompleted) "completed" else "marked as active"
        _lastOperationSuccess.value = "Task $status"
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: "Failed to update task"
        Result.failure(e)
    }

    /**
     * Restores a deleted task
     */
    suspend fun restoreTask(task: Task): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.restoreTask(task)
        _lastOperationSuccess.value = "Task restored successfully"
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: "Failed to restore task"
        Result.failure(e)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Bulk set completion status for multiple tasks
     */
    suspend fun bulkSetCompleted(taskIds: List<Long>, completed: Boolean): Result<Unit> = try {
        _isLoading.value = true
        _errorMessage.value = null

        taskManager.setCompletedBulk(taskIds, completed)

        val action = if (completed) "completed" else "marked as active"
        _lastOperationSuccess.value = "${taskIds.size} tasks $action"
        Result.success(Unit)
    } catch (e: Exception) {
        val errorMsg = e.message ?: "Failed to update tasks"
        _errorMessage.value = errorMsg
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }

    /**
     * Bulk delete tasks by IDs
     */
    suspend fun bulkDeleteTasks(taskIds: List<Long>): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.deleteTasksByIds(taskIds)
        _lastOperationSuccess.value = "${taskIds.size} tasks deleted successfully"
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: "Failed to delete tasks"
        Result.failure(e)
    }

    /**
     * Restore multiple deleted tasks
     */
    suspend fun restoreTasks(tasks: List<Task>): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.restoreTasks(tasks)
        _lastOperationSuccess.value = "${tasks.size} tasks restored successfully"
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: "Failed to restore tasks"
        Result.failure(e)
    }

    /**
     * Sets pin status for a task
     */
    suspend fun setPinned(taskId: Long, pinned: Boolean): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.setPinned(taskId, pinned)
        val status = if (pinned) "pinned" else "unpinned"
        _lastOperationSuccess.value = "Task $status successfully"
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: "Failed to update pin status"
        Result.failure(e)
    }

    /**
     * Sets priority for a task
     */
    suspend fun setPriority(taskId: Long, priority: Int): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.setPriority(taskId, priority)
        _lastOperationSuccess.value = "Task priority updated successfully"
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: "Failed to update priority"
        Result.failure(e)
    }

    // Archive operations

    /**
     * Gets archived tasks
     */
    fun getArchivedTasks() = taskManager.getArchivedTasks()

    /**
     * Archives a task (soft delete)
     */
    suspend fun archiveTask(taskId: Long): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.archiveTask(taskId)
        _lastOperationSuccess.value = "Task archived successfully"
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: "Failed to archive task"
        Result.failure(e)
    }

    /**
     * Unarchives a task (restore from archive)
     */
    suspend fun unarchiveTask(taskId: Long): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.unarchiveTask(taskId)
        _lastOperationSuccess.value = "Task restored successfully"
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: "Failed to restore task"
        Result.failure(e)
    }

    /**
     * Bulk archive tasks by IDs
     */
    suspend fun bulkArchiveTasks(taskIds: List<Long>): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.archiveTasks(taskIds)
        _lastOperationSuccess.value = "${taskIds.size} tasks archived successfully"
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: "Failed to archive tasks"
        Result.failure(e)
    }

    /**
     * Bulk unarchive tasks by IDs
     */
    suspend fun bulkUnarchiveTasks(taskIds: List<Long>): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.unarchiveTasks(taskIds)
        _lastOperationSuccess.value = "${taskIds.size} tasks restored successfully"
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: "Failed to restore tasks"
        Result.failure(e)
    }

    /**
     * Permanently deletes a task (hard delete - archived tasks only)
     */
    suspend fun hardDeleteTask(taskId: Long): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.hardDeleteTask(taskId)
        _lastOperationSuccess.value = "Task permanently deleted"
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: "Failed to permanently delete task"
        Result.failure(e)
    }

    /**
     * Bulk permanently delete tasks by IDs (hard delete - archived tasks only)
     */
    suspend fun bulkHardDeleteTasks(taskIds: List<Long>): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.hardDeleteTasks(taskIds)
        _lastOperationSuccess.value = "${taskIds.size} tasks permanently deleted"
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: "Failed to permanently delete tasks"
        Result.failure(e)
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _lastOperationSuccess.value = null
    }
}
