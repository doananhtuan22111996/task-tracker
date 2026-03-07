package dev.tuandoan.tasktracker.domain.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.ITaskManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskCrudUseCase @Inject constructor(
    private val taskManager: ITaskManager,
    @ApplicationContext private val context: Context,
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

        _lastOperationSuccess.value = context.getString(R.string.op_task_created)
        Result.success(Unit)
    } catch (e: Exception) {
        val errorMsg = e.message ?: context.getString(R.string.error_create_task)
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
            _lastOperationSuccess.value = context.getString(R.string.op_task_updated)
            Result.success(Unit)
        } else {
            _errorMessage.value = context.getString(R.string.error_schedule_reminder)
            Result.failure(RuntimeException(context.getString(R.string.error_schedule_reminder)))
        }
    } catch (e: Exception) {
        val errorMsg = e.message ?: context.getString(R.string.error_update_task)
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
        _lastOperationSuccess.value = context.getString(R.string.snackbar_task_deleted)
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: context.getString(R.string.error_delete_task)
        Result.failure(e)
    }

    /**
     * Toggles the completion status of a task
     */
    suspend fun toggleTaskCompletion(task: Task): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.toggleTaskCompletion(task)
        _lastOperationSuccess.value =
            if (!task.isCompleted) {
                context.getString(
                    R.string.op_task_marked_complete,
                )
            } else {
                context.getString(R.string.op_task_marked_incomplete)
            }
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: context.getString(R.string.error_update_task)
        Result.failure(e)
    }

    /**
     * Restores a deleted task
     */
    suspend fun restoreTask(task: Task): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.restoreTask(task)
        _lastOperationSuccess.value = context.getString(R.string.op_task_restored)
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: context.getString(R.string.error_restore_task)
        Result.failure(e)
    }

    /**
     * Duplicates a task, copying title, description, tag, and priority.
     * Resets completion, due date, reminder, pin, and archive state.
     */
    suspend fun duplicateTask(task: Task): Result<Long> = try {
        _isLoading.value = true
        _errorMessage.value = null

        val newTaskId = taskManager.createTask(
            title = task.title,
            description = task.description,
            dueAt = null,
            reminderOffsetMinutes = null,
            tag = task.tag,
        )

        // Set priority if different from default (MEDIUM = 1)
        if (task.priority != 1) {
            taskManager.setPriority(newTaskId, task.priority)
        }

        _lastOperationSuccess.value = context.getString(R.string.snackbar_task_duplicated)
        Result.success(newTaskId)
    } catch (e: Exception) {
        val errorMsg = e.message ?: context.getString(R.string.error_duplicate_task)
        _errorMessage.value = errorMsg
        Result.failure(e)
    } finally {
        _isLoading.value = false
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

        _lastOperationSuccess.value =
            if (completed) {
                context.getString(
                    R.string.snackbar_tasks_marked_completed,
                    taskIds.size,
                )
            } else {
                context.getString(R.string.snackbar_tasks_marked_active, taskIds.size)
            }
        Result.success(Unit)
    } catch (e: Exception) {
        val errorMsg = e.message ?: context.getString(R.string.error_update_tasks)
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
        _lastOperationSuccess.value = context.getString(R.string.snackbar_tasks_deleted, taskIds.size)
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: context.getString(R.string.error_delete_tasks)
        Result.failure(e)
    }

    /**
     * Restore multiple deleted tasks
     */
    suspend fun restoreTasks(tasks: List<Task>): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.restoreTasks(tasks)
        _lastOperationSuccess.value = context.getString(R.string.snackbar_tasks_restored, tasks.size)
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: context.getString(R.string.error_restore_tasks)
        Result.failure(e)
    }

    /**
     * Sets pin status for a task
     */
    suspend fun setPinned(taskId: Long, pinned: Boolean): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.setPinned(taskId, pinned)
        _lastOperationSuccess.value =
            if (pinned) context.getString(R.string.op_task_pinned) else context.getString(R.string.op_task_unpinned)
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: context.getString(R.string.error_pin_task)
        Result.failure(e)
    }

    /**
     * Sets priority for a task
     */
    suspend fun setPriority(taskId: Long, priority: Int): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.setPriority(taskId, priority)
        _lastOperationSuccess.value = context.getString(R.string.op_task_priority_updated)
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: context.getString(R.string.error_update_priority)
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
        _lastOperationSuccess.value = context.getString(R.string.snackbar_task_archived)
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: context.getString(R.string.error_archive_task)
        Result.failure(e)
    }

    /**
     * Unarchives a task (restore from archive)
     */
    suspend fun unarchiveTask(taskId: Long): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.unarchiveTask(taskId)
        _lastOperationSuccess.value = context.getString(R.string.op_task_restored)
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: context.getString(R.string.error_restore_task)
        Result.failure(e)
    }

    /**
     * Bulk archive tasks by IDs
     */
    suspend fun bulkArchiveTasks(taskIds: List<Long>): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.archiveTasks(taskIds)
        _lastOperationSuccess.value = context.getString(R.string.snackbar_tasks_archived, taskIds.size)
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: context.getString(R.string.error_archive_tasks)
        Result.failure(e)
    }

    /**
     * Bulk unarchive tasks by IDs
     */
    suspend fun bulkUnarchiveTasks(taskIds: List<Long>): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.unarchiveTasks(taskIds)
        _lastOperationSuccess.value = context.getString(R.string.snackbar_tasks_restored, taskIds.size)
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: context.getString(R.string.error_restore_tasks)
        Result.failure(e)
    }

    /**
     * Permanently deletes a task (hard delete - archived tasks only)
     */
    suspend fun hardDeleteTask(taskId: Long): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.hardDeleteTask(taskId)
        _lastOperationSuccess.value = context.getString(R.string.snackbar_task_permanently_deleted)
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: context.getString(R.string.error_hard_delete_task)
        Result.failure(e)
    }

    /**
     * Bulk permanently delete tasks by IDs (hard delete - archived tasks only)
     */
    suspend fun bulkHardDeleteTasks(taskIds: List<Long>): Result<Unit> = try {
        _errorMessage.value = null
        taskManager.hardDeleteTasks(taskIds)
        _lastOperationSuccess.value = context.getString(R.string.snackbar_tasks_permanently_deleted, taskIds.size)
        Result.success(Unit)
    } catch (e: Exception) {
        _errorMessage.value = e.message ?: context.getString(R.string.error_hard_delete_tasks)
        Result.failure(e)
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _lastOperationSuccess.value = null
    }
}
